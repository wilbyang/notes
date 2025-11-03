# DuckDB Problem Solving 指南：把“数据近身计算”变成工程利器

> 目标：不仅仅是“在 Python 里跑个 SQL”，而是以 DuckDB 的嵌入式、向量化、零服务依赖特性，解决那些常规数据平台高成本、低迭代速度的真实痛点。让你在遇到数据密集型问题时，第一反应不再只是“建表 + ETL + 等集群”，而是思考：我能否将算子直接搬到本地内存里，让结果在交互循环中被瞬间验证？

## 0. 思维框架：为什么选择 DuckDB 下潜？
触发点特征：
1. 小中规模数据（GB ~ 十数 GB），但需要频繁迭代分析。  
2. 数据分布在本地文件（Parquet/CSV/JSON）或对象存储映射。  
3. 想用 SQL + 向量化引擎避免写低效循环。  
4. 不希望部署服务（无守护进程、免 DevOps 成本）。  
5. 需要跨语言（Python/Java/CLI）统一访问语义。  

DuckDB 提供：嵌入式、列存、向量化执行 + ANSI SQL + 零网络 + 强 Parquet 生态适配。

> 心智模型：它是“数据算子加速器”，把一次分析从“写脚本 → 跑慢 → 重构”变为“加载 → SQL 组合 → 秒级反馈 → 快速假设验证”。

## 1. 真实场景与策略

### 场景 A：产品分析迭代极慢（需要快速验证行为假设）
痛点：在传统数仓里跑一个改动要排队；本地 CSV 数据又处理慢。
策略：将原始事件日志 Parquet 直接通过 DuckDB 外部表读取，使用窗口函数 + 复杂聚合在本地秒级完成；用 notebook 做交互式迭代。
要点：`SELECT user_id, COUNT(*) FILTER(WHERE action='click') / NULLIF(COUNT(*) FILTER(WHERE action='view'),0) AS click_view_ratio FROM events GROUP BY user_id;`
成功指标：假设验证时间从小时 → 分钟。

### 场景 B：训练数据抽样与特征生成耦合严重
痛点：Python pandas 特征工程代码冗长且慢；难以复现同一抽样逻辑。
策略：把特征工程表达为 SQL：使用 CTE 分层、窗口函数产出统计特征、`UNNEST` 处理数组；DuckDB 向量化降低 Python 解释器开销。
要点：
```sql
WITH base AS (
	SELECT id, ts, label, json_extract_scalar(meta,'$.country') AS country
	FROM parquet_scan('data/train/*.parquet')
), stats AS (
	SELECT country, COUNT(*) AS cnt, AVG(label) AS avg_label FROM base GROUP BY country
)
SELECT b.*, s.avg_label FROM base b LEFT JOIN stats s USING(country);
```
成功指标：特征构建耗时降低 3~10x；SQL 可版本化与审计。

### 场景 C：ETL 前置“质量闸门”缺失（脏数据流入下游）
痛点：数据错误只能在下游模型/报表发现，修复链路长。
策略：DuckDB 作为本地质量预检：集中执行规则（空值率、分布偏差、主键唯一性）并输出 JSON 报告，失败则阻断提交。
要点：
```sql
SELECT 'null_rate' AS rule, COUNT(*) FILTER(WHERE col IS NULL)*1.0/COUNT(*) AS value FROM data;
SELECT col, COUNT(*) AS dup_cnt FROM data GROUP BY col HAVING COUNT(*)>1; -- 唯一性
```
成功指标：质量缺陷检测前移；下游回滚减少。

### 场景 D：跨格式联邦查询（多源拼接分析）
痛点：不同格式/路径数据先要导入统一仓库才能 JOIN，耗时。
策略：DuckDB 直接扫描多个 Parquet/CSV/JSON 文件或 HTTP S3 映射，利用虚拟表函数（如 `read_parquet`, `read_csv_auto`）做即时联邦 JOIN。
要点：`SELECT * FROM read_parquet('s3://bucket/a.parquet') a JOIN read_csv_auto('local/b.csv') b ON a.id=b.id;`
成功指标：开发准备时间从天 → 小时；零重复落地。

### 场景 E：复杂报表生成流水线为单线程瓶颈
痛点：Python 里多层聚合 + 分组 + 透视慢。
策略：用 DuckDB 的 `PIVOT` 与窗口函数一次完成；减少中间 DataFrame 物化。
要点：`SELECT * FROM (SELECT region, product, SUM(amount) AS amt FROM sales GROUP BY 1,2) PIVOT (SUM(amt) FOR product IN ('A','B','C'));`
成功指标：报表生成时间降低 ≥50%。

### 场景 F：日志小型 OLAP（无需引入 ClickHouse/Druid）
痛点：需要临时多维切片分析，小型团队不想维护集群。
策略：将 Parquet 日志和维度表都放置在对象存储 / NAS；DuckDB 嵌入式实例 + 缓存热数据 + 利用 `STATISTICS` 分区裁剪。
要点：`SELECT date, action, COUNT(*) FROM logs WHERE date BETWEEN '2025-10-01' AND '2025-10-31' GROUP BY 1,2;`
成功指标：查询延迟（千万行内）可控制在秒级。

### 场景 G：模型离线验证（批量指标计算）
痛点：写 Python 循环计算 precision/recall/F1 的批量版本缓慢且代码重复。
策略：将预测与真实标签表 JOIN 后使用聚合计算所有指标；避免行级循环。
要点：
```sql
WITH eval AS (
 SELECT truth, pred, COUNT(*) AS cnt
 FROM predictions JOIN labels USING(id)
 GROUP BY 1,2
), m AS (
 SELECT SUM(CASE WHEN truth=1 AND pred=1 THEN cnt ELSE 0 END) AS TP,
				SUM(CASE WHEN truth=0 AND pred=1 THEN cnt ELSE 0 END) AS FP,
				SUM(CASE WHEN truth=1 AND pred=0 THEN cnt ELSE 0 END) AS FN
 FROM eval
)
SELECT TP*1.0/(TP+FP) AS precision,
			 TP*1.0/(TP+FN) AS recall,
			 2*TP*1.0/(2*TP+FP+FN) AS f1
FROM m;
```
成功指标：指标计算统一、性能较循环提升数量级。

### 场景 H：本地数据湖 Schema 演进试验场
痛点：直接在生产 Hive/Glue 上试 Schema 代价高。
策略：用 DuckDB 读取多版本 Parquet，比较列存在性与类型并用 SQL 自动生成迁移 diff 报告。
要点：
```sql
SELECT column_name, logical_type, physical_type
FROM parquet_metadata('data/v2/*.parquet') EXCEPT
SELECT column_name, logical_type, physical_type
FROM parquet_metadata('data/v1/*.parquet');
```
成功指标：Schema 升级评估时间显著缩短。

### 场景 I：数据科学交互循环加速（Prompt Engineering/特征假设）
痛点：LLM prompt/特征试验需要快速查看统计分布/相关性。
策略：通过 DuckDB 快速计算频次、分位、相关系数；无需把数据完整载入 pandas。
要点：`SELECT corr(a,b) FROM read_parquet('features.parquet');` / `SELECT quantile(value,0.9) FROM t;`
成功指标：实验迭代速度提升；减少内存峰值。

### 场景 J：轻量数据服务原型（原型阶段不建后端）
痛点：验证内部分析接口需求时不想搭建复杂服务。
策略：嵌入式 DuckDB + 小型 HTTP 层（Flask/FastAPI），请求内执行 SQL 并流式返回；后续再替换为正式数据平台。
要点：预编译语句 + 简单缓存；限制查询黑名单防止资源滥用。
成功指标：原型上线 < 1 天。功能验证后再抬升架构。

## 2. 操作模式抽象
| 模式 | 目标 | 关键动作 | 技术点 | 风险控制 |
|------|------|----------|--------|----------|
| Ad-hoc Analysis | 快速洞察 | 即时 SQL/窗口函数 | 临时会话 | 资源边界/内存监测 |
| Local ETL Gate | 质量前置 | 规则聚合输出报告 | CTE + 聚合 | 超时回退 |
| Feature Synthesis | 特征生成 | 窗口/数组/JSON 提取 | 向量化执行 | 类型校验 |
| Federated Read | 多源拼接 | 外部函数扫描 JOIN | `read_*` 函数 | I/O 并行/重试 |
| Metric Batch | 指标合成 | 聚合统计 | 单语法块 | 溢出检查 |
| Schema Diff | 演进评估 | 元数据比较 | `parquet_metadata` | 版本隔离 |
| Lightweight Service | 原型接口 | 嵌入+HTTP | 预编译/缓存 | 注入防护 |
| DSL Acceleration | 运算优化 | SQL 内联 UDF | 注册函数 | 正确性测试 |

## 3. 微策略库
1. CTE 分层构造：将复杂逻辑按语义拆分，保留可读性与可调试性。  
2. 延迟物化：能用单 SQL 完成的不要拆 pandas 中间表。  
3. 统计哨兵：对关键指标建立 SQL 视图，作为质量闸门。  
4. 元数据先行：在加载数据前先跑 `parquet_metadata` 看列类型与行数。  
5. 索引式思维：虽然 DuckDB 没有传统二级索引，但可通过列裁剪 + 谓词下推 + 分区文件筛选模拟性能。  
6. 精准抽样：`TABLESAMPLE SYSTEM (10)` 或自定义 `WHERE hash(id)%10=0` 保证复现。  
7. 函数注册：将常用业务变换实现为 Python UDF 或 Extension，减少重复 SQL 片段。  
8. 批指标合成：把多个指标放同一个 CTE，减少多次扫描。  
9. 外部资源控制：通过 `PRAGMA threads=N` 限制资源占用。  
10. 结果快照：重要分析结果 `COPY (SELECT ...) TO 'snapshot.parquet'` 版本化追踪。  

## 4. 工具与扩展生态
| 能力 | 说明 | 场景 |
|------|------|------|
| Parquet 原生 | 无需导入即可查询 | 冷数据快速洞察 |
| Arrow 集成 | 与内存列式互转 | 数据科学流水线 |
| Python UDF | 轻量嵌入自定义逻辑 | 特征工程个性化 |
| Extension | C/C++ 扩展算子 | 高性能特殊函数 |
| HTTPFS/S3 | 远程对象存储读取 | 联邦查询 |
| Catalog & View | 逻辑命名与复用 | 质量闸门/共享逻辑 |

## 5. 示例：质量闸门脚本（Python + DuckDB）
```python
import duckdb, json, sys

con = duckdb.connect()
path = sys.argv[1]

metrics = {}
metrics['row_count'] = con.execute(f"SELECT COUNT(*) FROM read_parquet('{path}')").fetchone()[0]
metrics['null_rate_colA'] = con.execute(f"SELECT COUNT(*) FILTER(WHERE colA IS NULL)*1.0/COUNT(*) FROM read_parquet('{path}')").fetchone()[0]
dup = con.execute(f"SELECT colA, COUNT(*) c FROM read_parquet('{path}') GROUP BY 1 HAVING COUNT(*)>1 LIMIT 10").fetchdf()
metrics['dup_sample'] = dup.to_dict(orient='records')

print(json.dumps(metrics, ensure_ascii=False, indent=2))
```
要点：
* 单连接多查询避免重复 IO。
* 使用聚合一次得出指标；只在重复值出现时输出样例。

## 6. 示例：自定义 UDF 加速特征表达
```python
import duckdb
con = duckdb.connect()

def ratio(a: int, b: int) -> float:
		return a / b if b else 0.0

con.create_function('ratio', ratio)
res = con.execute("SELECT ratio(clicks, views) FROM read_parquet('events.parquet')").fetchdf()
```
要点：将业务常用分母保护逻辑集中；SQL 更简洁。

## 7. 性能与价值度量
| 维度 | 指标 | 采集 | 目标 |
|------|------|------|------|
| 分析迭代速度 | 假设 → 结果时间 | 手动/脚本计时 | 分钟级 |
| 查询延迟 | 行数 vs 耗时 | 自动基准 | 秒级（千万行）|
| 资源占用 | 峰值内存 | `system_monitor` | 可控于机器物理内存内 |
| 重复代码减少 | SQL 片段复用率 | 版本 diff | 明显下降 |
| 质量缺陷前置率 | 缺陷被前置发现占比 | 报告统计 | >70% |

## 8. 风险与防御
| 风险 | 表现 | 防御 |
|------|------|------|
| 内存溢出 | 一次性查询读取巨量文件 | 分批扫描/限制线程 |
| 类型不一致 | 不同文件列类型漂移 | 元数据对比预检 |
| UDF 性能劣化 | Python 函数行级调用过度 | 改写为纯 SQL 或 C 扩展 |
| 误用为长任务平台 | 单机长跑阻塞其他开发 | 约定最大查询时长 |
| 安全风险 | 原型 API 未限制 SQL | 白名单/只读视图 |
| 数据重复快照膨胀 | 大量临时 Parquet | 生命周期管理/清理策略 |

## 9. 创造力视角（5 个跃迁思考）
1. 算子内联视角：将复杂 Python for 循环抽象为一个窗口 + 聚合组合，消除解释器开销。  
2. 数据邻近视角：算子靠近数据文件本身（Parquet scan → 直接聚合），避免多层拉取。  
3. Schema 反射视角：先读元数据再设计 SQL，而不是 trial & error。  
4. 分段迭代视角：用 CTE 拆出可验证的子逻辑，逐层加复杂度。  
5. 价值压缩视角：在单查询中合成多指标，减少多轮往返与等待。  

## 10. 决策树（简版）
```
问题类型 ─► 数据量是否远超内存? ─► 是 → 选择分布式或分批方案
						│                        └► 否 → DuckDB 嵌入式
						└► 需要频繁模式迭代? ─► 是 → 优先 SQL + CTE
						│                        └► 否 → 直接批处理脚本
						└► 多源文件格式? ─► 是 → read_* 联邦查询
																	 └► 否 → 单源优化（列裁剪）
						└► 需前置质量闸门? ─► 是 → 质量规则脚本
																	 └► 否 → 快速分析路径
```

## 11. 心智宣言
DuckDB 把“数据处理”这一传统需要重平台支撑的行为，重新压缩为“本地一次函数调用 + 瞬时算子组合”。它让个人开发者具备“小型数据平台”的瞬时能力：读取、联邦、聚合、验证、原型服务—all in embedded。真正的跃迁不是学会几个函数，而是重塑决策顺序：先想“本地向量化是否足够”，再考虑是否需要集群。让复杂的数据工作流有一条轻盈的、低成本的备用路径。

> 下次再遇到“要不要建个临时 Hive 表”的犹豫，不妨试试：用 DuckDB 两行 SQL 先让结果落地，再决定是否需要重型化。你的数据迭代速度，将因此不同。


## 12. 最小 C++ Extension 示例：自定义聚合 `positive_sum`
目标：实现一个只对正数求和的聚合函数，展示 DuckDB 扩展编写最小骨架与工程要点。该案例可扩展为更复杂统计（如 Winsorized Mean、条件权重和）。

### 12.1 聚合构成要素
DuckDB 自定义聚合需要：
1. 状态结构（State）
2. 初始化函数 (initialize)
3. 更新函数 (update)
4. 合并函数 (combine) – 用于并行部分结果汇总
5. 终结函数 (finalize)
6. 注册逻辑（放入 Extension 初始化）

### 12.2 目录结构（最小）
```
duckdb-positive-sum/
  CMakeLists.txt
  positive_sum.cpp
```

### 12.3 `positive_sum.cpp` 示例代码
```cpp
#include "duckdb.hpp"
#include "duckdb/function/aggregate_function.hpp"
#include "duckdb/main/extension.hpp"

using namespace duckdb;

struct PositiveSumState { double value; }; // 简单状态

static void PositiveSumInit(DataChunk &args, AggregateInputData &aggr_input, Vector &state_vector, idx_t count) {
	auto states = FlatVector::GetData<PositiveSumState>(state_vector);
	for (idx_t i = 0; i < count; i++) {
		states[i].value = 0.0;
	}
}

static void PositiveSumUpdate(Vector inputs[], AggregateInputData &aggr_input, idx_t input_count,
							  Vector &state_vector, idx_t count) {
	auto &input = inputs[0];
	auto states = FlatVector::GetData<PositiveSumState>(state_vector);
	UnifiedVectorFormat vdata;
	input.ToUnifiedFormat(count, vdata);
	auto data = (double*)vdata.data;
	auto sel = vdata.sel;
	for (idx_t i = 0; i < count; i++) {
		auto idx = sel.get_index(i);
		if (!vdata.validity.RowIsValid(idx)) continue; // 跳过 NULL
		double v = data[idx];
		if (v > 0) states[i].value += v; // 仅累加正数
	}
}

static void PositiveSumCombine(Vector &source, Vector &target, AggregateInputData &aggr_input, idx_t count) {
	auto src = FlatVector::GetData<PositiveSumState>(source);
	auto dst = FlatVector::GetData<PositiveSumState>(target);
	for (idx_t i = 0; i < count; i++) {
		dst[i].value += src[i].value;
	}
}

static void PositiveSumFinalize(Vector &state_vector, AggregateInputData &aggr_input, Vector &result, idx_t count) {
	auto states = FlatVector::GetData<PositiveSumState>(state_vector);
	auto out = FlatVector::GetData<double>(result);
	for (idx_t i = 0; i < count; i++) {
		out[i] = states[i].value;
	}
}

static unique_ptr<FunctionData> PositiveSumBind(ClientContext &context, AggregateFunction &function,
												vector<unique_ptr<Expression>> &arguments) {
	// 可做类型/参数检查；此处简化。
	return nullptr;
}

extern "C" DUCKDB_EXTENSION_API void duckdb_extension_init(duckdb::DatabaseInstance &db) {
	Connection conn(db);
	// 定义聚合函数：输入 double，输出 double，状态为 PositiveSumState
	auto fun = AggregateFunction("positive_sum",
		{LogicalType::DOUBLE}, LogicalType::DOUBLE,
		PositiveSumInit, PositiveSumUpdate, PositiveSumCombine, PositiveSumFinalize,
		nullptr, // simple state destructor
		PositiveSumBind,
		sizeof(PositiveSumState));
	conn.CreateAggregateFunction(fun);
}

extern "C" DUCKDB_EXTENSION_API const char *duckdb_extension_version() {
	return DuckDB::LibraryVersion();
}
```

### 12.4 `CMakeLists.txt`（简化示例）
```cmake
cmake_minimum_required(VERSION 3.16)
project(positive_sum_ext CXX)
find_package(DuckDB REQUIRED) # 假设已安装或使用源码子目录
add_library(positive_sum_ext SHARED positive_sum.cpp)
target_link_libraries(positive_sum_ext DuckDB::DuckDB)
set_target_properties(positive_sum_ext PROPERTIES
	CXX_STANDARD 17
	PREFIX ""
	OUTPUT_NAME "positive_sum"
)
```

### 12.5 构建与加载（示意）
构建：
```bash
mkdir build && cd build
cmake .. -DDuckDB_ROOT=/path/to/duckdb
make -j
```
加载与使用：
```sql
LOAD './positive_sum';
SELECT positive_sum(x) FROM read_parquet('data.parquet');
```

### 12.6 校验与测试
1. 正数与负数混合：`VALUES (1.0),(-2.0),(3.5),(NULL)` → 期望 4.5
2. 全负数：返回 0
3. 大量 NULL：确保忽略
4. 并行：构造 > 并行阈值行数，验证 combine 行为正确（结果与单线程相同）。

### 12.7 性能与扩展思考
* 利用 UnifiedVectorFormat 减少多类型判断。  
* 若需要支持多类型（INT、DECIMAL），可注册多个重载或在 update 中做类型模板化。  
* 可以进一步实现可配置阈值：`positive_sum(x, min_value)`，通过 bind 提取第二参数放入函数数据。  
* 复杂聚合（如分位数、草图结构）可将状态扩展为结构体 + 自定义内存管理。  

### 12.8 失败与回滚策略
* 扩展加载失败：提供降级路径（使用内置 `SUM` 作为替代）。  
* 聚合错误（类型不匹配）：在 bind 阶段提前抛出友好错误。  
* 性能不达标：基准对比内置 `SUM`，若差距 >20% 则暂缓上线。  

---
该最小扩展示例说明：通过少量 C++ 代码即可把“业务特化逻辑”搬进 DuckDB 执行内核，形成真正的“数据近身算子”。与前文场景结合，你可以快速迭代：先用 SQL + Python UDF 验证 → 性能/调用频次升高 → 升级为 C++ Extension → 继续向更复杂统计演进。

## 13. Parquet 分区规划与裁剪策略
目标：在 DuckDB 直接扫描文件体系结构（无集中元数据服务）的前提下，通过良好分区 + 列/文件裁剪设计，最小化 IO，保证交互式查询稳定在秒级。该节给出：分区列选择原则、文件粒度建议、典型场景矩阵、裁剪层次、反模式、决策表。

### 13.1 分区列选择 5 原则
1. 高选择性但查询常用：过滤后显著减少数据（如 `date`, `country`），且几乎每个查询都带条件。
2. 基数适中：避免过高（>10^5 目录爆炸）或过低（单目录巨文件无法裁剪）。理想基数 10^2 ~ 10^3。
3. 低演变频率：不频繁回填或修改，保证分区稳定性。
4. 排列顺序按“过滤稳定性”递减：最前层为几乎所有查询都会限定的维度。
5. 避免把高基数随机 ID 放入目录；改用列内统计裁剪。

### 13.2 文件大小建议
| 规模 | 单文件目标大小 | 原因 |
|------|----------------|------|
| KB-级太小 | 避免 | 元数据与打开成本过高 |
| 1~8 MB | 可接受（极轻量场景） | 本地交互快速预览 |
| 32~256 MB | 推荐区间 | 顺序读效率 + DuckDB 向量批处理友好 |
| >512 MB | 谨慎 | 内存峰值与裁剪粒度变粗 |

策略：若落盘初始为大量 <1MB 文件，先用 DuckDB 自身合并：`COPY (SELECT * FROM read_parquet('raw/*.parquet')) TO 'merged/...' (FORMAT PARQUET, COMPRESSION ZSTD);`

### 13.3 典型场景分区矩阵
| 场景 | 推荐分区列 | 说明 | 反例 |
|------|------------|------|------|
| 行为日志 | dt/hour/app | dt 几乎所有查询使用；hour 提高尖峰过滤；app 防止热点集中 | 按 user_id 分区（高基数） |
| 电商订单 | dt/status | status 维度低基数有过滤价值 | city 若基数过大且不常过滤 |
| 监控指标 | dt/metric_name | metric_name 中等基数，便于单指标查询 | host （高基数）放前导致目录膨胀 |
| 机器学习特征快照 | dt/version | version 用于回溯；dt 历史对比 | user_segment 若动态变化频繁 |
| 异常检测结果 | dt/severity | severity 低基数；用于告警视图 | trace_id 分区导致碎片 |

### 13.4 裁剪 4 层次
1. 目录裁剪（分区裁剪）：路径 pattern / 过滤解析 → 直接减少文件集合。
2. 文件级统计裁剪：Parquet footer 中 `min/max` / null_count → 排除不可能命中文件。
3. 列裁剪：只读取查询涉及列，DuckDB 自动推导列集合。
4. 向量批内谓词裁剪：执行阶段过滤数据批减少后续算子数据量。

> 设计目标：尽量让“目录裁剪”与“文件统计裁剪”拦截 ≥80% 的无关数据，降低后端 CPU 解压负荷。

### 13.5 Parquet 元数据辅助策略
利用 `parquet_metadata()`：
```sql
-- 查看某字段在不同文件的 min/max 分布
SELECT file_name, row_group_id, column_name, statistics_min, statistics_max
FROM parquet_metadata('logs/dt=2025-10-*/hour=*/*.parquet')
WHERE column_name='latency_ms';
```
用例：分析发现某些小时段 latency 全在低值区间，则高阈值过滤可排除文件。

### 13.6 冷热分层与读取策略
* 热数据（近 3~7 天）保持较细 hour 分区；冷数据合并为日级或周级大文件。
* 查询 routing：若时间范围跨越冷热边界，先分别聚合再 UNION，减少对冷热大文件混合扫描。

### 13.7 反模式清单
| 反模式 | 后果 | 替代 |
|--------|------|------|
| 过度嵌套分区（dt/hour/minute）| 目录爆炸 + 小文件 | 保留至 hour，分钟过滤下推在列统计实现 |
| 用高基数 ID 分区 | 文件过度碎片 | 保留为列 + 后期索引/统计裁剪 |
| 无压缩（UNCOMPRESSED） | IO 膨胀 | ZSTD / SNAPPY |
| 小文件雪花化 | 打开/元数据成本高 | 定期合并 batch compact |
| 单列宽表无列裁剪意识 | 读取多余列 | SELECT 只取必要列，或垂直拆分 |
| 盲目统一大文件 >1GB | 失去并行 & 粗粒度裁剪 | 控制在推荐区间 |

### 13.8 决策表（简化）
```
输入: 预计行数N, 典型查询过滤列集合F, 列基数字典B
1. 对 F 中每列评估: (过滤必然性 * 选择性收益) 排序 → 取前2~3列作为分区层级
2. 计算单日数据大小S → 估计单分区数据量 S / (分区组合数)
3. 若 单分区数据量 < 8MB → 合并/减少分区层级
4. 若 单分区数据量 > 512MB → 考虑增加一层（但评估基数）或拆分写入批
5. 试运行: 基于样本生成布局 → 使用 parquet_metadata 做命中率模拟
6. 命中率模拟：给定典型查询集合，计算理论需扫描文件占总文件占比 ≤ 20% 则接受
```

### 13.9 实操小脚本（命中率估算）
思路：收集典型查询的时间范围 + 维度过滤，统计匹配路径 pattern 的文件数 / 总文件数。
伪 Python：
```python
import glob, json

all_files = glob.glob('logs/dt=*/hour=*/*.parquet')
def match(q):  # q={'dt':['2025-10-01','2025-10-02'], 'hour':["10","11"], 'app':['web']}
	sel = []
	for f in all_files:
		ok = True
		for k, v in q.items():
			# 简单解析 k=val from path
			if not any(f"{k}={val}" in f for val in v):
				ok = False; break
		if ok: sel.append(f)
	return len(sel)

queries = [ {'dt':['2025-10-01'], 'hour':['10','11']}, {'dt':['2025-10-02'], 'app':['web']} ]
stats = []
for q in queries:
	hit = match(q)
	stats.append({'q':q,'hit':hit,'ratio':hit/len(all_files)})
print(json.dumps(stats, indent=2))
```

### 13.10 度量与迭代
| 指标 | 目标 |
|------|------|
| 平均查询扫描文件占比 | < 20% |
| 典型查询冷启动延迟 | 秒级（<3s）|
| 小文件比例 (<8MB) | < 5% |
| 大文件比例 (>512MB) | < 10% |
| 分区重建频率 | ≤ Schema 大改时 |

---
通过上述规划与策略，你可在无需重型元数据服务的前提下，将“目录布局”本身变成廉价且高效的裁剪索引。重点不在追求完美层级，而在基于查询反馈的迭代：采集命中率 → 调整分区列 → 定期合并小文件 → 复测。

## 14. DuckDB 在 GIS / LBS 场景的应用
DuckDB 通过 `spatial` 扩展（以及社区对 GeoParquet、WKT/WKB 的支持）可在本地嵌入式环境下完成“准专业”地理分析：附近搜索、区域聚合、轨迹切片、栅格化热力、地理分桶（geohash/h3/quadbin）等，不必依赖 PostGIS 集群或大数据 Geo 引擎。适用于“中等数据量 + 快速交互验证 + 模型特征生成”场景。

### 14.1 基础安装与启用
```sql
INSTALL spatial; -- 只需一次
LOAD spatial;    -- 会话内加载
```
校验：`SELECT ST_Version();`

### 14.2 数据格式与读取
1. GeoParquet：含几何列（通常命名 `geometry`）；DuckDB 可直接读取：`SELECT ST_Area(geometry) FROM read_parquet('regions/*.parquet');`
2. WKT/WKB：使用 `ST_GeomFromText(wkt)` / `ST_GeomFromWKB(blob)` 转换。
3. 分桶坐标（geohash/h3）：存为字符串/整数列，配合 UDF 映射成点或多边形中心。

### 14.3 典型 LBS / GIS Problem Solving 场景

#### 场景 A：附近 POI 搜索（半径过滤）
数据：用户位置点表 `user_locs(lat, lon)`；POI 表 `pois(id, category, lat, lon)`。
策略：先粗过滤（经纬度 bounding box）→ 精确球面距离。
```sql
WITH params AS (SELECT 31.2304 AS qlat, 121.4737 AS qlon, 1000 AS radius_m),
rect AS (
	SELECT qlat, qlon, radius_m,
				 qlat + (radius_m/111320.0) AS max_lat,
				 qlat - (radius_m/111320.0) AS min_lat,
				 qlon + (radius_m/(111320.0*cos(radians(qlat)))) AS max_lon,
				 qlon - (radius_m/(111320.0*cos(radians(qlat)))) AS min_lon
	FROM params
)
SELECT p.id, p.category,
			 ST_DistanceSphere(ST_Point(p.lon, p.lat), ST_Point(r.qlon, r.qlat)) AS dist
FROM pois p, rect r
WHERE p.lat BETWEEN r.min_lat AND r.max_lat
	AND p.lon BETWEEN r.min_lon AND r.max_lon
	AND ST_DistanceSphere(ST_Point(p.lon,p.lat), ST_Point(r.qlon,r.qlat)) <= r.radius_m
ORDER BY dist LIMIT 50;
```
成功指标：半径 1km 查询在百万 POI 下仍保持亚秒级（依赖列裁剪 + 边界初筛）。

#### 场景 B：地理网格热力 (Hex / H3 模拟)
策略：若未使用专门 H3 库，可用近似 geohash 或自定义“经纬度栅格”聚合。
```sql
-- 简易 0.01° 网格聚合
SELECT floor(lat*100)/100 AS lat_bin,
			 floor(lon*100)/100 AS lon_bin,
			 COUNT(*) AS cnt
FROM user_locs
GROUP BY 1,2
HAVING cnt > 10
ORDER BY cnt DESC
LIMIT 100;
```
进一步：可把 `(lat_bin, lon_bin)` 组合为字符串 key 供前端渲染热力图。

#### 场景 C：行政区域落点统计（点 ↦ 多边形）
数据：区域 GeoParquet `regions(id, name, geometry)`；事件点 `events(lat, lon)`。
策略：先构造点 geometry，再用空间包含；对多 polygon 可预先 simplify 减少复杂度。
```sql
WITH e AS (
	SELECT *, ST_Point(lon, lat) AS geom FROM events
), r AS (
	SELECT id, name, geometry FROM read_parquet('regions/*.parquet')
)
SELECT r.name, COUNT(*) AS events_cnt
FROM r JOIN e ON ST_Contains(r.geometry, e.geom)
GROUP BY r.name
ORDER BY events_cnt DESC;
```
成功指标：区域映射正确率 100%；简化后执行时间下降明显（可用 `ST_Simplify`）。

#### 场景 D：轨迹切片与停留点识别
数据：`tracks(user_id, ts, lat, lon)` 有序。
策略：借助窗口函数 + 距离与时间阈值识别“停留段”。
```sql
WITH ordered AS (
	SELECT *,
		LAG(lat) OVER(PARTITION BY user_id ORDER BY ts) AS prev_lat,
		LAG(lon) OVER(PARTITION BY user_id ORDER BY ts) AS prev_lon,
		LAG(ts)  OVER(PARTITION BY user_id ORDER BY ts) AS prev_ts
	FROM tracks
), metrics AS (
	SELECT *,
		ST_DistanceSphere(ST_Point(lon,lat), ST_Point(prev_lon,prev_lat)) AS step_m,
		(ts - prev_ts) AS dt_s
	FROM ordered
), flag AS (
	SELECT *,
		CASE WHEN step_m < 30 AND dt_s > 120 THEN 1 ELSE 0 END AS stay_marker
	FROM metrics
), groups AS (
	SELECT *, SUM(CASE WHEN stay_marker=0 THEN 1 ELSE 0 END)
				 OVER (PARTITION BY user_id ORDER BY ts ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS seg_id
	FROM flag
)
SELECT user_id, seg_id,
			 MIN(ts) AS seg_start,
			 MAX(ts) AS seg_end,
			 AVG(lat) AS lat_center,
			 AVG(lon) AS lon_center
FROM groups
WHERE stay_marker=1
GROUP BY user_id, seg_id
HAVING (seg_end - seg_start) > 300; -- 停留>5分钟
```
成功指标：识别出的停留点复核准确率 >90%；运行时间可在百万轨迹点内维持秒级。

#### 场景 E：多区域 AB 区域指标对比
目标：比较两个城区在新功能上线前后的活跃度变化。
策略：时间窗口 + 区域聚合 + Diff 计算在一条 SQL 内完成，避免多阶段中间表。
```sql
WITH region_events AS (
	SELECT r.name AS region, e.ts, e.user_id
	FROM read_parquet('regions/*.parquet') r
	JOIN (SELECT *, ST_Point(lon,lat) AS geom FROM events) e
		ON ST_Contains(r.geometry, e.geom)
	WHERE e.ts BETWEEN '2025-10-01' AND '2025-10-31'
), base AS (
	SELECT region,
				 DATE_TRUNC('day', ts) AS d,
				 COUNT(DISTINCT user_id) AS dau
	FROM region_events
	GROUP BY 1,2
), pivot AS (
	SELECT region,
				 AVG(CASE WHEN d < '2025-10-15' THEN dau END) AS dau_before,
				 AVG(CASE WHEN d >= '2025-10-15' THEN dau END) AS dau_after
	FROM base GROUP BY region
)
SELECT region, dau_before, dau_after,
			 (dau_after - dau_before)*1.0/NULLIF(dau_before,0) AS lift
FROM pivot ORDER BY lift DESC;
```

### 14.4 空间索引与近似策略
DuckDB 当前未内置重型 R-Tree 索引持久化机制（spatial 扩展已在演进），小型/中等数据量下可用以下组合：
1. 预过滤 bounding box：使用经纬度范围减少候选集合。
2. Geohash/H3/Quadbin 近似：
	 * 写入时生成列 `geohash_6` / `h3_7`；查询时先做相等/IN 过滤，再用精确 `ST_Distance` 验证。
3. 预聚合：为热查询维度（如小时+区域格）生成每日 summary 表，减少点级扫描。
4. 分区：按日期 + 粗粒度地理格（比如 geohash 前缀 4）组织目录 → 目录裁剪 + 列过滤联合使用。

### 14.5 Geo 特征工程（ML 场景）
常见特征：
* 用户栅格停留时间占比：`stay_time_in_grid / total_active_time`。
* 轨迹平均转向角：窗口计算相邻向量夹角，再聚合。
* 区域多样性指数：一天内访问不同 geohash 数量 / 总打点数。
* 距离连续增长异常：检测潜在速度欺诈（位置跳跃）。

示例（转向角片段）：
```sql
WITH o AS (
	SELECT user_id, ts, lat, lon,
		LAG(lat) OVER(PARTITION BY user_id ORDER BY ts) AS lat1,
		LAG(lon) OVER(PARTITION BY user_id ORDER BY ts) AS lon1,
		LAG(lat,2) OVER(PARTITION BY user_id ORDER BY ts) AS lat0,
		LAG(lon,2) OVER(PARTITION BY user_id ORDER BY ts) AS lon0
	FROM tracks
), v AS (
	SELECT *,
		ST_DistanceSphere(ST_Point(lon1,lat1), ST_Point(lon0,lat0)) AS a,
		ST_DistanceSphere(ST_Point(lon,lat), ST_Point(lon1,lat1)) AS b,
		ST_DistanceSphere(ST_Point(lon,lat), ST_Point(lon0,lat0)) AS c
	FROM o WHERE lat0 IS NOT NULL
), angle AS (
	SELECT user_id, ts,
		ACOS( (a*a + b*b - c*c) / NULLIF(2*a*b,0) ) AS turn_angle
	FROM v WHERE a>0 AND b>0
)
SELECT user_id, AVG(turn_angle) AS avg_turn_angle FROM angle GROUP BY user_id;
```

### 14.6 性能调优准则
| 问题 | 现象 | 策略 |
|------|------|------|
| 邻近搜索慢 | 反复全表扫描 | 先 geohash 前缀过滤 + 距离精算 |
| 区域包含耗时 | 多边形过度精细 | ST_Simplify 预处理；缓存 bounding box |
| 轨迹计算内存高 | 过多窗口字段 | 只保留必要列；分区按用户/日期处理 |
| 聚合热点 | 时间范围跨度大 | 先分段聚合再 UNION ALL 汇总 |
| 重复构造点 | 多次 ST_Point | 预生成 geom 列（视图或持久列） |

### 14.7 风险与防御
| 风险 | 表现 | 防御 |
|------|------|------|
| 投影/坐标混用 | 距离偏差 | 统一使用 WGS84；如需平面距离，转换到合适投影 |
| 精度损失 | 过度 simplify | 控制简化阈值，抽样验证面积/周长误差 |
| 误用 geohash 粒度 | 漏检/过宽候选 | 选择适中长度；多级 fallback |
| 轨迹排序缺失 | 窗口运算错误 | 保证 `ORDER BY ts`；缺失补充排序键 |
| 内存放大 | 读取大型区域集合 | 仅加载需要字段；分批区域 subset |

### 14.8 度量指标
| 维度 | 指标 | 目标 |
|------|------|------|
| 附近搜索延迟 | P95(ms) | < 300ms (百万 POI 内) |
| 区域包含吞吐 | 点/秒 | > 1e5 (本地) |
| 轨迹特征提取 | 用户/秒 | > 1e3 |
| 网格聚合覆盖率 | 有效格子占比 | 与期望热点分布匹配 |
| 误差控制 | 距离/面积偏差 | < 2% (简化后) |

### 14.9 决策速览（GIS 路径）
```
需求类型 → 纯点附近? → bbox + sphere_distance
				→ 区域统计? → ST_Contains / Simplify + 预聚合
				→ 轨迹特征? → 窗口 + 距离/角度推导
				→ 热力格? → 栅格/Geohash 聚合 + 阈值过滤
				→ 多尺度查询? → 预生成多粒度分桶 (hash_5, hash_6)
```

---
通过空间扩展，DuckDB 成为“单机可迭代 GIS 分析实验室”：用极低的进入门槛完成早期验证、特征提取与策略评估，然后再决定是否迁移到 PostGIS / 分布式 Geo 引擎。关键心法：先用近似（分桶/bbox）快速收缩候选，再用精确几何函数验证与度量。


