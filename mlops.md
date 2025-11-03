# 基于案例实践 MLOps

> 命题：在真实业务迭代与模型生命周期治理的场景下，如何用 MLOps 思维与工具链实现“可复现 + 可回溯 + 可扩展 + 可监测”的闭环，而不是堆砌零散组件。本文循序渐进通过案例阶梯展现心智模型跃迁：从一次性训练脚本 → 标准化实验 → 数据与特征管线 → 模型注册与部署策略 → 评估与漂移监控 → 主动再训练策略 → 组织与合规治理。

---
## 0. 学习路径总览（阶段与心智转变）
阶段 | 关注焦点 | 心智转变 | 典型失败模式
---|---|---|---
1. 脚本阶段 | 单次训练成功 | “能跑就行” → 不可复现 | 结果无法重现、参数遗失
2. 实验管理 | 版本与指标跟踪 | 将实验视为对象 | excel 手工记参数
3. 数据/特征管线 | 数据契约与血缘 | 从“拿到就用”→“可验证/可演化” | 上游变更导致隐性退化
4. 模型注册与发布 | 版本与环境一致性 | 从“复制文件”→“规范产物制品” | 线上使用陈旧权重
5. 评估与线上监控 | 真实流量统计与比较 | 静态精度 → 多维行为健康 | 模型静默劣化
6. 漂移与警戒 | 数据/概念/性能漂移检测 | 被动应对 → 主动预测 | 延迟发现影响业务
7. 主动再训练 | 触发策略与资源调度 | Cron 式 → 事件驱动+策略选型 | 过度/不足再训练
8. 组织治理 | 角色/流程/合规 | 单人英雄 → 协同规则化 | 安全/审计缺失

认知比喻：MLOps 就是“模型生命周期的操作系统”，你在给模型的演化装上中枢神经与感知系统。

---
## 1. 案例一：一次性脚本到实验对象化
### 场景
数据科学家在本地跑 `train.py`，调几次学习率，取最好结果发给后端并手工记下参数。两周后需重训，一切忘光。

### 痛点
- 参数、数据版本、随机种子、指标无系统存档。
- 结果不可复现，难以做因果归因。

### 目标
引入一个最小实验记录器（文件/轻量 DB），自动捕捉：配置、代码版本（Git commit）、数据快照标识、指标。

### 最小实现骨架
```python
import json, time, hashlib, subprocess, os

def capture_git_commit():
    try:
        return subprocess.check_output(['git', 'rev-parse', 'HEAD']).decode().strip()
    except Exception:
        return 'no-git'

def experiment_id(cfg):
    raw = json.dumps(cfg, sort_keys=True).encode()
    return hashlib.md5(raw).hexdigest()[:8]

class Experiment:
    def __init__(self, cfg):
        self.cfg = cfg
        self.commit = capture_git_commit()
        self.id = experiment_id(cfg)
        self.start = time.time()
        self.metrics = {}
    def log_metric(self, name, value):
        self.metrics[name] = value
    def complete(self):
        rec = {
            'id': self.id,
            'cfg': self.cfg,
            'commit': self.commit,
            'duration': time.time() - self.start,
            'metrics': self.metrics
        }
        os.makedirs('runs', exist_ok=True)
        with open(f'runs/{self.id}.json', 'w') as f: json.dump(rec, f, ensure_ascii=False, indent=2)

cfg = {'lr':1e-3,'batch':64,'seed':42,'model':'mlp32'}
exp = Experiment(cfg)
# ... 训练过程
exp.log_metric('val_auc', 0.812)
exp.log_metric('train_loss_final', 0.21)
exp.complete()
```
### 心智升维点
- 实验是“实体”，不是散落的运行结果。
- 任何后续分析与对比建立在可复现记录之上。

### 延伸挑战
- 加入依赖包版本快照 (`pip freeze`).
- 加入自动复制 `train.py` 内容到归档，防止代码漂移。

---
## 2. 案例二：数据集版本化与数据契约
### 场景
训练集由多个来源合并（日志、配置表、补充标签），上游某字段类型变更导致模型精度下降无人察觉。

### 痛点
- 无法追溯数据列变化。
- 特征统计不被记录，导致漂移无基线。

### 目标
建立数据版本元信息：哈希 + 模式(schema) + 基本统计（均值/标准差/缺失率）持久化；对比新旧差异。

### 最小实现骨架
```python
import pandas as pd, json, hashlib, os

def dataset_fingerprint(df: pd.DataFrame):
    schema = {c:str(df[c].dtype) for c in df.columns}
    stats = {c:{'mean':float(df[c].mean()) if pd.api.types.is_numeric_dtype(df[c]) else None,
               'missing':float(df[c].isna().mean())} for c in df.columns}
    raw = json.dumps({'schema':schema,'stats':stats}, sort_keys=True).encode()
    h = hashlib.sha1(raw).hexdigest()[:10]
    return h, schema, stats

os.makedirs('data_meta', exist_ok=True)

# 加载数据
train = pd.read_parquet('data/train.parquet')
fp, schema, stats = dataset_fingerprint(train)
with open(f'data_meta/{fp}.json','w') as f:
    json.dump({'fingerprint':fp,'schema':schema,'stats':stats}, f, indent=2)
```
### 心智升维点
- “数据契约” = 对下游的稳定承诺：字段含义+类型+质量范围。
- 漂移检测前提是有历史基线。

### 延伸挑战
- 增加：分类特征取值集合与 Top-N 频次。
- 构建一个对比函数：新数据集 vs 旧数据集 → 输出差异摘要。

---
## 3. 案例三：特征管线与可复用算子库
### 场景
多个团队成员各自写特征脚本；重复逻辑多，修 bug 需要多处修改。

### 痛点
- 特征工程不可组合：难以重排顺序/插拔。
- 重复实现增加认知与维护成本。

### 目标
建立特征算子库：纯函数 + 输入输出契约 + 组合流水线；记录每个特征的血缘（来源字段与依赖）。

### 最小实现骨架
```python
from typing import Dict, Any

# 约定：所有特征函数接受 dict 输入，返回 dict 输出（新增特征）

def feat_ratio(d: Dict[str, Any]):
    return {'feat_ratio': d['x'] / (d['y'] + 1)}

def feat_log_amount(d):
    import numpy as np
    return {'feat_log_amount': np.log1p(d['amount'])}

FEATURE_FUNCS = [feat_ratio, feat_log_amount]

def apply_features(row):
    out = {}
    for f in FEATURE_FUNCS:
        out.update(f(row))
    return out
```
### 心智升维点
- 特征是“可组合算子”而非脚本段落；利于 A/B 与增量扩展。
- 血缘记录使得出错时快速定位源头字段。

### 延伸挑战
- 添加血缘自动记录：函数加装饰器标记使用字段集合。
- 构造 DAG：节点=特征，边=依赖字段或其他特征。

---
## 4. 案例四：模型注册与制品管理
### 场景
部署时直接复制某目录下的 `model.pkl` 到服务器；不清楚其训练数据、参数、框架版本。

### 痛点
- 模型不透明：无法判断是否应升级或回滚。
- 缺乏一致性：线上环境与训练环境差异导致推理异常。

### 目标
引入“模型制品”概念：包含权重 + 元数据（训练配置、数据指纹、代码 commit）。注册到一个目录或服务。

### 最小实现骨架
```python
import os, json, shutil

def register_model(model_path, meta):
    os.makedirs('registry', exist_ok=True)
    mid = meta['id']
    target = f'registry/{mid}'
    os.makedirs(target, exist_ok=True)
    shutil.copy(model_path, f'{target}/model.pkl')
    with open(f'{target}/meta.json','w') as f:
        json.dump(meta, f, indent=2)
    return mid

meta = {
  'id':'mdl_20231101_a1',
  'data_fp':'abc123ef',
  'commit':'b7c9d1',
  'train_cfg':{'lr':1e-3,'batch':64},
  'metrics':{'val_auc':0.81}
}
register_model('outputs/model.pkl', meta)
```
### 心智升维点
- 模型不只是文件，是“带上下文的版本”。
- 回滚 = 选择旧版本目录，而不是找随机文件。

### 延伸挑战
- 增加签名：哈希校验 model.pkl 完整性。
- 构建一个 `list_models()` 按指标排序检索。

---
## 5. 案例五：灰度发布与在线对比评估
### 场景
新模型直接全量替换，发现召回下降却无法快速切换回旧版本或做并行对比。

### 痛点
- 缺乏多版本在线并行（shadow / A/B）。
- 无统一线上指标采集管道。

### 目标
实现最小灰度：同时加载两个模型，按百分比路由请求；记录两侧实时指标（延迟、预测差异、业务转化）。

### 最小实现骨架（伪）
```python
from random import random

MODEL_A = load_model('registry/mdl_old/model.pkl')
MODEL_B = load_model('registry/mdl_new/model.pkl')
TRAFFIC_SPLIT = 0.2  # 20% 给新模型

def route_predict(x):
    if random() < TRAFFIC_SPLIT:
        pred = MODEL_B.predict(x)
        log_online('B', pred)
        return pred
    else:
        pred = MODEL_A.predict(x)
        log_online('A', pred)
        return pred
```
### 心智升维点
- 发布是“受控实验”，而非一次性替换。
- 在线指标让“好不好”从主观转为数据驱动。

### 延伸挑战
- 加入一致性检查：两模型预测差异超过阈值报警。
- 引入动态流量调度：根据实时表现逐步提升新模型份额。

---
## 6. 案例六：数据 & 概念漂移检测
### 场景
模型精度逐月下降；发现用户行为模式变了，新特征分布偏移。

### 痛点
- 漂移检测滞后（仅靠离线再训练结果）。
- 无指标细分：数据分布、标签分布、预测置信度未被区分观察。

### 目标
实现最小漂移监测：对输入特征的分布（均值、KS 检验）、标签延迟对齐后精度、预测置信度熵建立时间序列。

### 最小实现骨架（概念）
```python
from scipy.stats import ks_2samp
import numpy as np

# 历史基线分布（离线计算）
baseline = {'feat_ratio_mean':0.12, 'feat_ratio_std':0.03, 'feat_ratio_samples': np.array([...])}

# 实时批次数据
batch_values = np.array([...])
ks_p = ks_2samp(baseline['feat_ratio_samples'], batch_values).pvalue
mean_shift = abs(batch_values.mean() - baseline['feat_ratio_mean'])

alert = (ks_p < 0.01) or (mean_shift > 3*baseline['feat_ratio_std'])
```
### 心智升维点
- 漂移指标是“模型感知层”，否则你只在结果劣化后被动反应。
- 区分：数据漂移（X 分布） vs 概念漂移（P(Y|X) 变化）。

### 延伸挑战
- 建立多特征协方差变化监测。
- AI 辅助：对漂移模式分类（季节性 / 结构突变 / 渐进漂移）。

---
## 7. 案例七：主动再训练与触发策略
### 场景
当前再训练用固定 cron：每周一 2:00；有时无收益，有时已经晚了。

### 痛点
- 浪费资源：无漂移仍再训练。
- 滞后：漂移早发生但等到计划窗口。

### 目标
制定触发条件：当漂移指标+在线业务指标组合满足条件（如：KS p < 0.01 AND 业务转化下降 > 2%）则增量再训练；否则延后。

### 最小实现骨架
```python
DRIFT_THRESHOLD = 0.01
BIZ_DROP_THRESHOLD = 0.02

def should_retrain(ks_p, biz_delta):
    return (ks_p < DRIFT_THRESHOLD) and (biz_delta < -BIZ_DROP_THRESHOLD)

if should_retrain(latest_ks_p, latest_conversion_delta):
    trigger_pipeline('retrain_job_v2')
```
### 心智升维点
- 再训练是“策略响应”，不是机械时间表。
- 引入成本意识：再训练开销 vs 性能收益。

### 延伸挑战
- 加入冷却时间窗口，避免频繁触发。
- 对比“按需触发”与“固定周期”资源消耗差异。

---
## 8. 案例八：特征与模型的血缘与审计
### 场景
审计要求：某次线上决策的输入特征与模型版本需可追溯，且可证明未使用禁止字段。

### 痛点
- 临时脚本无法提供决策溯源。
- 合规检查只能人工抽样。

### 目标
在推理阶段记录：请求 ID、模型版本 ID、特征字典哈希、时间戳；定期抽样对照特征白名单。

### 最小实现骨架
```python
import hashlib, json, time

ALLOWED_FEATURES = {'feat_ratio','feat_log_amount','feat_age'}

def hash_features(feats):
    raw = json.dumps(feats, sort_keys=True).encode()
    return hashlib.sha1(raw).hexdigest()


def audit_predict(model, feats, request_id):
    # 合规校验
    if not set(feats.keys()).issubset(ALLOWED_FEATURES):
        raise ValueError('Forbidden feature used')
    fid = hash_features(feats)
    record = {'req_id':request_id,'model':model.version,'fhash':fid,'ts':time.time()}
    append_log(record)
    return model.predict(feats)
```
### 心智升维点
- 审计是“第一类功能”而非附加；设计上即需支持。
- 血缘使“为什么这次预测这样”可以被解释。

### 延伸挑战
- 建立特征变化差异的审计对比工具（最近 1 小时 vs 历史平均）。
- 增加加密签名，提高不可抵赖性。

---
## 9. 案例九：跨环境一致性（Dev / Staging / Prod）
### 场景
本地通过，部署后报错：依赖版本不同；或本地预处理与线上服务不一致导致特征错位。

### 痛点
- 环境漂移：包版本、系统差异。
- 预处理重复实现（训练与推理两套）。

### 目标
通过：容器化 + 单一特征库 + 环境锁文件（requirements.txt / conda env）保证一致性；预处理封装为可复用模块。

### 最小实现骨架（概念）
```
# Dockerfile 伪示例
FROM python:3.10-slim
COPY requirements.txt ./
RUN pip install -r requirements.txt
COPY feature_lib.py model.pkl service.py ./
CMD ["python", "service.py"]
```
### 心智升维点
- 一致性构建 = 降低“线上只发生在那台机器”神秘事件。
- 训练 → 产物 → 部署是一条流水线，不是人工拼接。

### 延伸挑战
- 集成 CI：提交即自动构建镜像 + 单元测试预处理一致性。
- 运行时验证：推理服务启动自测一个虚拟样本与训练时预期对比。

---
## 10. 案例十：组织流程与角色协作
### 场景
数据科学、工程、运维、安全各自为政，模型生命周期交接混乱。

### 痛点
- 模型上线周期长，沟通成本高。
- 问题定位责任不清。

### 目标
定义最小角色模型：
- DS：负责实验与特征定义 → 产出模型制品与元数据。
- MLE：负责打包、性能与部署策略。
- SRE：负责监控与报警阈值调优。
- Compliance：负责审计与特征白名单维护。

### 心智升维点
- MLOps 是跨角色协作协议，减少重复沟通与信息缺失。
- 按功能域分层（实验/管线/发布/监控/治理）。

### 延伸挑战
- 绘制 RACI 表（Responsible, Accountable, Consulted, Informed）。
- 定义“从实验到上线”标准工单模板。

---
## 11. 综合项目：端到端闭环原型
场景：构建一个电商推荐模型的 MLOps 闭环：数据快照 → 特征算子库 → 实验记录 → 模型注册 → 灰度上线 → 漂移监控 → 主动再训练 → 审计日志 → 回滚策略。

路线概述：
1. 数据摄取 + 指纹记录。
2. 特征流水线构造 + 算子血缘标记。
3. 实验记录器 + 指标可视化（tensorboard / wandb 等）。
4. 训练完成 → 注册模型制品。
5. 部署阶段：灰度路由服务启动。
6. 在线监控：数据分布、预测置信度、业务 KPI。
7. 漂移触发 → 增量再训练 → 新版本注册 → 扩大流量。
8. 审计与合规：全量请求特征哈希 + 版本留档。
9. 回滚：选择上一版本制品目录 + 重载服务。

关键心智：模型不再是孤立“权重文件”，而是被嵌入到一条可观测、可控制、可回滚的生命周期流水线之中。

---
## 12. 学习闭环与迁移
迁移到更成熟平台（Kubeflow / MLflow / Vertex AI）时携带的心智：
- 一切实体化：实验、数据集、特征、模型、监控指标都应有唯一 ID。
- 改动 = 新版本（不可变原则），可回溯历史。
- 自动化 = 将频繁/易错环节抽象成管线组件，减少粘贴脚本。
- 治理 = 从 Day 1 考虑审计与合规，而不是事后补。

自检问题：
1. 什么信息构成一个“合格的模型制品元数据”？
2. 漂移检测与再训练触发关系怎样设计才避免过拟合？
3. 如何统一训练与推理时的特征处理？
4. 在审计场景下，怎样证明某次预测未使用禁用特征？
5. 灰度发布失败时快速回滚机制的关键要素是什么？

> MLOps 的价值不在炫技堆工具，而在用结构化心智让模型迭代“可控且可证”。当你的流程能被追踪、验证与快速调整，模型才能安全、持续为业务创造复利。
