# 基于案例学 Dask

> 命题：如何通过一组真实、递进的场景，建立从“单机脚本思维”到“分布式图调度心智”的跃迁，掌握 Dask 在数据处理 / 特征工程 / 机器学习流水线加速中的核心用法与架构视角，而不是停留在零散 API 记忆。

---
## 0. 学习路径总览：从列表操作到任务图认知
阶段 (Phase) 与心智转变：
1. 切片式扩展：把原本 pandas / numpy 操作直接替换成 Dask DataFrame / Array，理解延迟计算 (lazy) 与 `compute()`。
2. 图可视化：意识到你的操作形成一张 DAG（Directed Acyclic Graph），调度策略决定性能与内存占用峰值。
3. 分块与分区 (Chunk/Partition) 设计：决定计算与数据搬运成本。
4. Shuffle 与 Skew 管理：从“顺利计算”转变到“代价感知”调优。
5. 特征工程流水线：多步加工串联 → 多分支共享中间结果。
6. 分布式集群与调度器：本地线程池 → 分布式 scheduler → 动态扩容。
7. 与 ML 生态对接：Dask-ML / XGBoost dask / 外部库并行批训练。
8. 监控与诊断：Dashboard 指标 → 任务热点 → 内存压力 → 反压 (backpressure)。
9. 系统化治理：可复现 pipeline + 参数化分块策略 + 自动剖析。

认知比喻：Dask 是“你代码里隐形的任务图编排器”，学习过程就是让这张图逐渐可见、可控、可优化。

---
## 1. 案例一：从 pandas 到 Dask DataFrame（延迟与并行的第一步）
### 1.1 场景
现有一个处理数千万行 CSV 的 pandas 脚本：读入 → 过滤 → 分组聚合 → 导出。执行极慢且内存溢出。

### 1.2 痛点
- pandas 单机内存限制，读入即爆。
- 脚本线性执行，无法利用多核。
- 改批处理很麻烦。

### 1.3 目标
使用 Dask DataFrame 替换：分块读取、延迟执行、仅在最终需要结果时触发计算。

### 1.4 最小实现骨架
```python
import dask.dataframe as dd
# 假设有多个分片文件 data_*.csv
df = dd.read_csv('data_*.csv')
filtered = df[df['amount'] > 100]
result = filtered.groupby('user_id')['amount'].sum()
# 仍未计算，构建了任务图
final = result.compute()  # 在此触发并行计算
```
### 1.5 心智升维点
- `compute()` 是一个“承诺兑现”动作：之前构建的是图，不是立即结果。
- 把“内存限制”对话转化为“分区大小”与“任务并行度”调节。

### 1.6 延伸挑战
- 尝试 `persist()` 部分中间结果，观察 Dashboard 中内存占用与任务数量变化。
- 修改 `read_csv(..., blocksize=...)` 对比任务切分颗粒度效果。

---
## 2. 案例二：DAG 可视化与性能洞察
### 2.1 场景
前一案例执行缓慢，但不清楚慢在哪里：是 I/O，还是聚合阶段？需要洞察。

### 2.2 目标
使用 `visualize()` 生成任务图，理解依赖结构；使用 Dashboard 查看 worker 负载与瓶颈。

### 2.3 最小实现骨架
```python
# 继续前例 result 对象
result.visualize(filename='groupby_graph.svg')
# 在运行环境启动 scheduler 后访问: http://localhost:8787/status
```
### 2.4 心智升维点
- 图不是装饰：它揭示重算风险（有无公共子表达式）。
- 关注“宽 vs 深”：宽代表并行潜力，深代表串行瓶颈。

### 2.5 延伸挑战
- 添加一个重复使用的中间计算，看图中是否出现共享节点。
- 对比“立即 compute” vs “persist 再 compute”执行差异。

---
## 3. 案例三：分区策略与数据倾斜处理
### 3.1 场景
用户聚合发现某些分区处理时间远超其他；导致整体拖慢（straggler）。

### 3.2 痛点
- key 分布极度不均匀（长尾用户）。
- shuffle 代价大，内存瞬时飙升。

### 3.3 目标
通过重新分区、抽样分析 key 分布、选择合理分块大小降低倾斜影响。

### 3.4 骨架
```python
# 查看分区数与示例大小
len(df.divisions), df.npartitions

# 采样 key 分布
sample = df['user_id'].sample(frac=0.001).compute()
# 分析频次，决定是否做自定义分桶

# 重新分区（按行数或哈希）
df2 = df.repartition(npartitions=200)

# 或者先 map_partitions 局部聚合再做全局 reduce，降低数据移动
partial = df.map_partitions(lambda pdf: pdf.groupby('user_id')['amount'].sum())
final = partial.groupby('user_id').sum().compute()
```
### 3.5 心智升维点
- 倾斜处理策略：局部聚合 → 全局归并；减少跨分区 shuffle。
- 分区是“并行单元”，其大小与倾斜决定调度效率。

### 3.6 延伸挑战
- 尝试预估“最重分区执行时间”与总时间关系，形成简单预测模型。
- 编写一个函数：自动检测分区执行时间差值超过阈值时触发警告。

---
## 4. 案例四：多阶段特征工程流水线复用
### 4.1 场景
你有一个复杂特征工程：原始表 → 清洗 → 派生列 → 多种聚合 → 合并多路结果；脚本重复执行多次不同参数版本，极其耗时。

### 4.2 痛点
- 重复计算中间层，浪费资源。
- 脚本耦合，难以插拔新特征。

### 4.3 目标
构建一个“管线图”并对关键中间结果 `persist()`，下游共享；让变更只在最小子图重新计算。

### 4.4 骨架
```python
raw = dd.read_parquet('raw/*.parquet')
clean = raw[raw['valid'] == 1]
feat_a = clean.assign(ratio = clean['x'] / (clean['y'] + 1))
feat_b = clean.groupby('user_id')['z'].mean()
feat_c = clean.groupby('user_id')['w'].std()

# 持久化共享节点
cached = feat_a.persist()
# 下游再组合
merged = dd.merge(cached, feat_b.compute(), left_on='user_id', right_index=True)
```
### 4.5 心智升维点
- `persist` 让你把“逻辑图”的部分切换为“物化数据”，减少重算。
- 你在管理一张“演化中的 DAG”，不是一次性脚本。

### 4.6 延伸挑战
- 度量：对比完全重算 vs 部分持久化的总执行时间与内存高峰。
- 实现一个装饰器：标记可缓存步骤并自动选择是否持久化。

---
## 5. 案例五：与 Scikit-Learn / XGBoost 的分布式训练
### 5.1 场景
需要训练一个梯度提升模型（XGBoost）在千万行特征数据上；单机内存不足且训练时间极长。

### 5.2 目标
利用 Dask 与 XGBoost 原生集成：分布式切分数据 → 并行训练 → 聚合模型。

### 5.3 骨架
```python
from dask.distributed import Client
import dask.dataframe as dd
import xgboost as xgb

client = Client()  # 本地或远程集群

df = dd.read_parquet('features/*.parquet')
X = df.drop('label', axis=1)
y = df['label']

# 转换为 Dask DMatrix
dtrain = xgb.dask.DaskDMatrix(client, X, y)
params = {"objective": "binary:logistic", "tree_method": "hist"}
output = xgb.dask.train(client, params, dtrain, num_boost_round=200)
booster = output['booster']
```
### 5.4 心智升维点
- 算法被“数据分区驱动”：每个 worker 局部统计 → 全局合并。
- Dask 负责数据与任务调度，XGBoost 专注算法逻辑。

### 5.5 延伸挑战
- 调整分区大小与 `num_boost_round` 观察扩展性拐点。
- 记录每轮训练的 worker 利用率，分析是否存在 IO 绑定。

---
## 6. 案例六：自定义计算图与 delayed 构造
### 6.1 场景
有部分计算不是 DataFrame 操作：例如多文件 JSON 合并 + 外部统计程序调用；希望统一进 Dask 图管理。

### 6.2 目标
用 `dask.delayed` 包裹任意 Python 函数，形成可调度节点；与 DataFrame 结果融合。

### 6.3 骨架
```python
from dask import delayed
import json, glob

@delayed
def load_json(path):
    with open(path) as f: return json.load(f)

files = glob.glob('stats/*.json')
objs = [load_json(p) for p in files]
merged = delayed(lambda lst: sum(o['value'] for o in lst))(objs)
final_value = merged.compute()
```
### 6.4 心智升维点
- 任何可序列化的纯函数都可进入任务图。
- 统一：DataFrame / Array / delayed 之间可互相组合与依赖。

### 6.5 延伸挑战
- 将外部 shell 调用包装为 delayed，监控其耗时分布。
- 构造一个混合图：特征工程 + 模型训练 + 外部指标写回。

---
## 7. 案例七：Dashboard 诊断与瓶颈分析
### 7.1 场景
Pipeline 运行不稳定，偶尔卡住；需要快速定位：是内存溢出、GC 抖动还是网络传输。

### 7.2 目标
使用 Dashboard 面板：Tasks, Workers, Graph, System 观察热点；搭建“诊断 checklist”。

### 7.3 核心观察项
- 任务积压：某类任务耗时显著高于平均。
- 内存水位：worker 是否频繁 spill 到磁盘。
- 网络流量：shuffle 阶段是否出现突增。
- CPU 利用：是否出现 core 利用不均衡（倾斜）。

### 7.4 心智升维点
- 诊断是“观察任务图的动态投影”，建立对阶段模式的辨识：读取 → 转换 → 聚合 → shuffle → 输出。
- 建立经验：看到某种图拓扑 + 指标模式 → 快速联想到对应调优策略（例如：重分区、局部预聚合、增大 blocksize）。

### 7.5 延伸挑战
- 写一个自动脚本：周期性抓取 Dashboard JSON API 指标，统计任务耗时分位并触发报警。
- 制作内部“拓扑模式 → 调优动作”手册。

---
## 8. 案例八：Backpressure 与资源治理
### 8.1 场景
异步提交大量新任务（生产流量批入），导致系统崩溃或延迟暴涨。

### 8.2 目标
实施简单 backpressure：限制同时在飞任务数量，监控队列长度，根据压力动态调整提交速率。

### 8.3 骨架概念（伪代码）
```python
from dask.distributed import Client
client = Client()

MAX_INFLIGHT = 1000
futures = []
for batch in incoming_batches():
    if len(futures) - sum(f.done() for f in futures) > MAX_INFLIGHT:
        wait_some(futures)  # 自定义等待策略
    fut = client.submit(process_batch, batch)
    futures.append(fut)
```
### 8.4 心智升维点
- 调度器不是无限弹性：过载导致调度开销 > 计算收益。
- 治理：使用队列长度 / pending 任务数作为反馈信号。

### 8.5 延伸挑战
- 将“提交速率”与历史平均完成速率做闭环控制（简单 PID）。
- 建立任务分类优先级：重要任务抢占调度。

---
## 9. 案例九：系统化 Pipeline 封装与配置化
### 9.1 场景
团队多人协作：各自脚本风格不同，难以统一可维护性与复现。

### 9.2 目标
- 抽象 Pipeline：节点=步骤函数，边=依赖。
- 配置文件决定启用哪些特征/分区策略/持久化策略。

### 9.3 概念骨架
```python
class Step:
    def __init__(self, name, func, deps=None, persist=False):
        self.name = name; self.func = func; self.deps = deps or []; self.persist = persist
    def run(self, context):
        inputs = [context[d] for d in self.deps]
        out = self.func(*inputs)
        if self.persist: out = out.persist()
        context[self.name] = out

steps = [
    Step('raw', lambda: dd.read_parquet('raw/*.parquet')),
    Step('clean', lambda r: r[r.valid==1], deps=['raw'], persist=True),
    Step('agg', lambda c: c.groupby('user_id').x.mean(), deps=['clean']),
]
context = {}
for s in steps: s.run(context)
final = context['agg'].compute()
```
### 9.4 心智升维点
- 任务图显式化：不再是隐式链式调用，而是“可编排对象”。
- 为后续自动插入监控/缓存/重试提供钩子。

### 9.5 延伸挑战
- 添加重试逻辑：失败步骤重新提交。
- 加入图可视化输出：根据 steps 自动绘制依赖 SVG。

---
## 10. 综合项目：从原始日志到特征矩阵再到模型训练
场景：海量行为日志（多文件、分区） → 清洗去噪 → 构造多组统计特征 → 训练分布式 XGBoost → 输出预测。

路线：
1. `read_parquet` 分区读取原始日志。
2. 清洗（过滤异常用户 / 缺失值填充）。
3. 多路聚合（行为频率、时间间隔统计、滑动窗口平均）。
4. 中间特征表持久化（减少重算）。
5. 合并形成训练矩阵；分区检查 & 倾斜治理。
6. Dask-ML 或 XGBoost 分布式训练；监控 Worker 利用与内存。
7. 预测输出写入（可能用 `map_partitions` 进行后处理）。
8. Dashboard 指标归档 + 执行拓扑图存档。

关键心智：你在操控一张不断物化与简化的任务图；所有性能调优归结为“图形状 + 分区策略 + 资源治理”三件事。

---
## 11. 迁移与心智固化
迁移到 Spark / Ray / 自研调度系统时带走的认知：
- 延迟计算与显式触发分离（先构图，再执行）。
- 分区是并行的最小单元；其大小与倾斜决定性能。
- Shuffle / 广播是高成本操作，需在图层面规避或缩减。
- 任务图可视化与指标联动是调优起点，而不是“最后救火”。

自检问题：
1. 何时使用 `persist()` 而不是直接 `compute()`？列出两个判据。
2. 如何识别与缓解分区倾斜？
3. 怎样在图中寻找可缓存的“重用热点”？
4. 外部 I/O 密集任务进入 Dask 图需要注意什么？
5. Backpressure 策略的两个核心监控指标是什么？

---
## 12. 延伸路线图
- 自定义调度插件：收集任务统计，做策略性重排。
- Adaptive scaling：根据负载动态扩容/缩容 Workers。
- 与 Arrow / Parquet 深度结合：零拷贝加速。
- 混合模式：Dask + PyTorch 数据加载（分布式预处理）。
- 成本模型：预估任务图执行时间与内存峰值（启发式或历史回放）。

> 学 Dask 的本质是“让你看见并掌控代码背后的计算图”，当心智从“脚本串联”迁移到“图治理”后，性能、可维护性与扩展性三者协同提升。
