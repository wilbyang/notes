# 基于案例学 JAX

> 命题：如何在真实工程与科研的连续场景中，循序渐进掌握 JAX 的核心心智与技巧，而不是碎片化 API 记忆。本文通过一条“能力阶梯”式案例链路：从向量化 → 流式计算 → 自动微分 → 高阶微分 → 可组合变换栈 → 模型化与结构搜索 → 分布式与批量实验治理。每一节给出：动机痛点、最小可行实现、思维升维点、延伸挑战。

---
## 0. 学习路径总览（从算子到系统）
阶段与目标：
1. 函数式基底：写纯函数 + 理解 PRNGKey。
2. 向量化与形状掌控：`vmap` 心智建立（把“循环”升格为“批量维”）。
3. 流式与状态递推：`lax.scan` 处理长序列与递推结构。
4. 自动微分：`grad` / `value_and_grad` 验证梯度与数值稳定。
5. 高阶微分：`jacfwd` / `jacrev` / `hessian` 在研究型 loss 中的应用。
6. 变换组合：`jit` + `vmap` + `grad` 的语义保持；理解编译边界。
7. 模型构造：Flax / Equinox 的不同范式与参数树。
8. 结构与超参并行：把“搜索”视为可向量化的维度。
9. 分布式/多设备：`pmap` 入门与随机性治理。
10. 实验治理与复现：Key 拆分策略、变换栈记录、失效模式探测。

认知比喻：JAX 是“算子变换显微镜”+“实验编排加速器”。学习顺序应让心智结构稳定，再扩张技术面。

---
## 1. 案例一：从 Python for 到 `vmap`（向量化心智）
### 1.1 场景
你在做用户行为基础统计：对每个用户的点击序列计算归一化权重和，再进一步得到加权点击率。传统写法是双层 for：外层用户，内层事件。

痛点：
- Python 循环慢，无法自然上 GPU。
- 形状处理反复出错（batch 与 time 搞混）。

### 1.2 目标
将“对单个用户序列的统计”提升为批量维度的纯函数映射，消除显式 Python 循环。

### 1.3 最小实现
```python
import jax, jax.numpy as jnp

# 单用户：给定事件权重 w 与点击标记 c，计算加权点击率
# 纯函数，无副作用

def weighted_ctr_single(w, c):
    w = w / (jnp.sum(w) + 1e-8)
    return jnp.sum(w * c)

# 批量：B 个用户，每个长度 T
B, T = 4, 10
key = jax.random.PRNGKey(0)
w_batch = jax.random.uniform(key, (B, T))
c_batch = jax.random.bernoulli(key, p=0.3, shape=(B, T)).astype(jnp.float32)

# vmap 映射：把第 0 维视为“批量维”
weighted_ctr_batch = jax.vmap(weighted_ctr_single)(w_batch, c_batch)
print(weighted_ctr_batch)
```
### 1.4 思维升维点
- 你不在“循环”，你在“声明一个对批量维的映射”；`vmap` 保持数学语义不变。
- 纯函数是前提：如果你写入全局变量，`vmap` 会复制副作用→不稳定。

### 1.5 延伸挑战
- 将多个统计（CTR、平均停留时长、点击熵）封装为单用户多指标函数，`vmap` 后返回结构化 pytree。
- 重写你项目里仍在用 for 的三个地方，审视是否适合 `vmap`。

---
## 2. 案例二：长序列与 `scan`（流式递推编排）
### 2.1 场景
需要对每个用户的超长事件序列（例如 100k 长度）做指数衰减累积评分；直接构造整张 T x 特征矩阵展开内存爆炸。

### 2.2 目标
用 `lax.scan` 实现 O(T) 内存 streaming 递推，避免显式全展开。

### 2.3 最小实现
```python
import jax, jax.numpy as jnp

def decay_accumulate(events, r):
    # events: (T,)
    def step(carry, x):
        new = r * carry + x
        return new, new
    last, all_vals = jax.lax.scan(step, 0.0, events)
    return all_vals  # (T,)

T = 1000
key = jax.random.PRNGKey(1)
events = jax.random.uniform(key, (T,))
score_seq = decay_accumulate(events, 0.9)
```
### 2.4 思维升维点
- `scan` 把“循环体”变成一个可组合的函数对象，JAX 能分析它的状态流。
- `scan` 可以嵌套：外层 `vmap`（用户），内层 `scan`（时间）。形成二维结构组合。

### 2.5 延伸挑战
- 将两个不同衰减率同时计算（双状态），返回一个二元组序列。
- 比较 `scan` 与手写 for 的性能与内存（加 `block_until_ready`）。

---
## 3. 案例三：自动微分与数值稳定性（`grad` 心智）
### 3.1 场景
你设计一个自定义损失：`loss = mean((pred - y)^2) + alpha * sum(|pred|^3)`；担心三次项导致梯度爆炸。

### 3.2 目标
用 `value_and_grad` 获取损失与梯度，验证梯度规模，建立“梯度体检”工具。

### 3.3 最小实现
```python
import jax, jax.numpy as jnp

alpha = 0.01

def loss_fn(params, x, y):
    w, b = params
    pred = x @ w + b
    mse = jnp.mean((pred - y)**2)
    reg = alpha * jnp.sum(jnp.abs(pred)**3)
    return mse + reg

value_and_grad = jax.value_and_grad(loss_fn)
key = jax.random.PRNGKey(2)
X = jax.random.normal(key, (128, 20))
true_w = jax.random.normal(key, (20, 1))
Y = X @ true_w + 0.1
params = (jax.random.normal(key, (20, 1)), jnp.array([0.0]))
loss, grads = value_and_grad(params, X, Y)

# 梯度体检
flat_grads, _ = jax.tree_util.tree_flatten(grads)
grad_norm = sum([jnp.linalg.norm(g) for g in flat_grads])
print(loss, grad_norm)
```
### 3.4 思维升维点
- 自动微分让你可以“先写数学，再验证梯度规模”而不是先怕风险。
- 自定义正则的试验成本大幅下降。

### 3.5 延伸挑战
- 引入梯度裁剪函数，测试不同裁剪阈值对收敛的影响。
- 对比 `finite differences` 的近似梯度验证正确性。

---
## 4. 案例四：高阶微分（灵敏度分析与研究型 Loss）
### 4.1 场景
你在研究：参数扰动对模型输出的二阶影响（例如稳健性分析）。希望得到 Hessian 的对角近似。

### 4.2 目标
用 `jax.hessian` 或组合 `jacrev(jacfwd)` 计算二阶信息，提取对角元素作为敏感度指标。

### 4.3 最小实现
```python
import jax, jax.numpy as jnp

def simple_model(w, x):
    return jnp.tanh(x @ w)

def scalar_objective(w, x):
    preds = simple_model(w, x)
    return jnp.mean(preds**2)

key = jax.random.PRNGKey(3)
x = jax.random.normal(key, (64, 16))
w = jax.random.normal(key, (16,))

hess = jax.hessian(lambda w: scalar_objective(w, x))(w)  # (16,16)
# 对角作为敏感度近似
sensitivity = jnp.diag(hess)
```
### 4.4 思维升维点
- 高阶微分在 JAX 中是“继续可组合的变换”，不是新的范式。
- 用于：不确定性估计、鲁棒性评估、自动调正则强度（自适应权重）。

### 4.5 延伸挑战
- 只对部分参数分组求 Hessian，对比不同组差异。
- 设计一个基于敏感度自适应的正则项重新加入训练循环。

---
## 5. 案例五：变换组合栈（`jit` + `vmap` + `grad`）
### 5.1 场景
训练 step 内部需要：前向预测 → 损失 → 梯度 → 参数更新；你希望它高效且语义稳定。

### 5.2 目标
构建一个组合栈：内核纯函数；外层按层次增加变换；测试编译与执行差异。

### 5.3 最小实现
```python
import optax

def predict(params, x):
    w, b = params
    return x @ w + b

def loss_fn(params, x, y):
    pred = predict(params, x)
    return jnp.mean((pred - y)**2)

loss_and_grad = jax.value_and_grad(loss_fn)
optimizer = optax.adam(1e-2)
opt_state = optimizer.init(params)

@jax.jit
def train_step(params, opt_state, batch):
    x, y = batch
    loss, grads = loss_and_grad(params, x, y)
    updates, opt_state = optimizer.update(grads, opt_state)
    params = optax.apply_updates(params, updates)
    return params, opt_state, loss
```
### 5.4 思维升维点
- 变换层次：语义不变，只是执行形式改变。
- 若 `jit` 后结果不同，首先怀疑：随机种子 / 纯度被破坏。

### 5.5 延伸挑战
- 增加 `vmap` 在超参组合维度：一次性训练多个学习率。
- 加入 `pmap` 在多设备维度（伪分布式）。

---
## 6. 案例六：模型范式对比（Flax vs Equinox）
### 6.1 场景
需要快速试验两个 MLP 结构，并后续可能加 BN/Dropout；同时希望对结构进行批量评估。

### 6.2 目标
理解 Flax 的 `init/apply` 与 Equinox 的 dataclass 风格对搜索与实验速度的影响。

### 6.3 Flax 最小示例
（参见前文，不赘述；关注参数树与 `apply` 纯度。）

### 6.4 Equinox 最小示例
```python
import equinox as eqx

class MLP(eqx.Module):
    layers: list
    def __init__(self, in_size, hidden, out_size, key):
        k1, k2 = jax.random.split(key)
        self.layers = [
            eqx.nn.Linear(in_size, hidden, key=k1),
            eqx.nn.Linear(hidden, out_size, key=k2)
        ]
    def __call__(self, x):
        return self.layers[1](jax.nn.relu(self.layers[0](x)))
```
### 6.5 思维升维点
- Flax：工程规范，对状态有原生支持。
- Equinox：结构自由度高，原型演化快。

### 6.6 延伸挑战
- 批量构造多个 Equinox 模型并用 `vmap` 前向评估。
- 用参数分组对不同层加不同正则。

---
## 7. 案例七：超参数并行探索（将“调参”变成张量操作）
### 7.1 场景
你有 12 组 (lr, weight_decay) 组合需要快速筛选；传统做法循环 12 次训练若干 step。

### 7.2 目标
将超参组合视为一个批量维度，用 `vmap` 并行计算若干步后的 validation metric。

### 7.3 最小实现骨架
```python
hps = jnp.array([
    [1e-3, 1e-4],
    [1e-3, 1e-5],
    [5e-4, 1e-4],
    ...
])  # shape (H,2)

def single_hp_step(params, hp, batch):
    lr, wd = hp
    loss, grads = loss_and_grad(params, batch[0], batch[1])
    params = tuple([
        p - lr * g - wd * p for p, g in zip(params, grads)
    ])
    return params, loss

v_step = jax.vmap(single_hp_step, in_axes=(None, 0, None))
new_params, losses = v_step(params, hps, batch)
```
### 7.4 思维升维点
- “搜索空间 = 批量维”心智降低脚本复杂度。
- 可与 `jit` 结合形成高速筛选器。

### 7.5 延伸挑战
- 与结构搜索组合：外层结构、内层超参双层 `vmap`。
- 记录每次筛选的 metric 演化形成 Pareto 前沿可视化。

---
## 8. 案例八：分布式与多设备（`pmap` 入门）
### 8.1 场景
你有 2~8 张 GPU，需要加速大 batch 训练。希望最少代码改动实现数据并行。

### 8.2 目标
用 `pmap` 包裹训练 step，对输入 batch 分片；管理随机种子拆分。

### 8.3 最小骨架
```python
# 假设 devices = jax.local_device_count()
key = jax.random.PRNGKey(42)
keys = jax.random.split(key, devices)

@jax.pmap
def train_step_pmap(params, x, y, key):
    loss, grads = loss_and_grad(params, x, y)
    # 聚合梯度（pmap 会自动通过跨设备通信）
    updates, _ = optimizer.update(grads, opt_state)  # 需在外管理 opt_state, 简化演示
    params = optax.apply_updates(params, updates)
    return params, loss
```
### 8.4 思维升维点
- 多设备扩展是“包裹式”的，不改变你的数学语义。
- seed 拆分必须显式，否则设备间随机行为不一致。

### 8.5 延伸挑战
- 对比单设备与多设备吞吐；绘制 step 时间趋势。
- 引入梯度同步延迟模拟（研究通信瓶颈）。

---
## 9. 案例九：实验治理与复现（把“实验”对象化）
### 9.1 场景
多次调参后无法复现之前的好结果；不同脚本种子散落，难以对比。

### 9.2 目标
集中化：配置对象 + master key + 变换栈日志 + 度量体检。

### 9.3 最小治理结构概念
```python
class Experiment:
    def __init__(self, cfg, master_key):
        self.cfg = cfg
        self.master_key = master_key
        self.step_keys = jax.random.split(master_key, cfg['num_steps'])
        self.history = []
    def log(self, step, metrics):
        self.history.append((step, metrics))

cfg = {'lr':1e-3, 'num_steps':100, 'model':'mlp32'}
exp = Experiment(cfg, jax.random.PRNGKey(0))
```
### 9.4 思维升维点
- 实验本身是一个“可追踪对象”，随机性与配置集中管理 → 可复现性提升。
- 失效模式（梯度爆炸/NaN）可以被标准化检测函数自动化扫描。

### 9.5 延伸挑战
- 给 history 加入哈希与输出摘要，形成检索索引。
- 基于多个实验的汇总统计自动推荐下一个超参区间。

---
## 10. 综合项目：将上述案例串联为“一日速成原型”
场景：构建一个多任务（行为预测 + 文本关联）轻量模型并做初步超参筛选。

总路线：
1. 数据读取 → 编码纯函数。
2. 行为序列用 `scan` 得到嵌入；文本用简单 pooling。
3. 拼接后进入 MLP（Equinox）。
4. 损失：主任务 + 相关性正则（二阶可选）。
5. 用 `vmap` 并行 6 组超参快速前 20 step 筛选。
6. 选 Top-2 超参进入长训练（jit 编译）。
7. 记录实验对象，输出性能曲线与梯度体检报告。

关键心智：所有扩展（多任务、多正则、超参搜索）都回到“纯函数 + 可组合变换”的骨干，而不是脚本复杂化。

---
## 11. 学习闭环与迁移
迁移到其他框架（PyTorch）时带走的心智：
- 先确定纯函数边界，再谈加速与并行。
- 把搜索与试验空间向量化，而非陷入嵌套循环。
- 数值稳定与梯度检查常规化，而非事后排错。

闭环自检问题：
1. 何时选 `scan` 而非 `vmap`？列出三个判据。
2. 怎样设计一个可同时评估 10 种结构与 8 组超参的批量范式？
3. 如果 `jit` 后结果与未编译不同，你的排查顺序是什么？
4. 如何将 Hessian 对角用于自适应正则？
5. 实验治理对象最少应记录哪些字段？

---
## 12. 后续延伸路线图
- 自定义梯度：`custom_vjp` 修补数值不稳定算子。
- SDE / Diffusion 采样：`scan` + 噪声调度复合管线。
- 分区并行：`pjit` 与参数切片（更大规模）。
- 结构自动生成：基于配置 DSL → 模型构造 → 向量化评估。

> 学会 JAX，不只是“能跑代码”，而是获得一套更贴近数学对象与实验演化的抽象工具。保持纯度，掌控形状，拥抱变换——其余皆可迭代。
