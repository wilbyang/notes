# ETL-dbt 从入门到精通

## 引言：为什么需要 dbt？

当数据工程师第一次接触 dbt 时，常见的反应是："这不就是把 SQL 脚本换个地方写吗？"但随着项目深入，会经历三个认知阶段：

1. **抗拒期**："我的 SQL 脚本用得好好的，为什么要学新工具？"
2. **理解期**："原来数据转换也可以有软件工程的最佳实践..."
3. **顿悟期**："这不是工具，是思维方式的革新"

dbt 的价值不仅是提供了一个工具，更重要的是它引导我们完成**从 ETL 到 ELT、从脚本到工程化、从孤岛到协作**的思维转变。

---

## 核心思维转变

### 传统 ETL 的困境

```
数据仓库现状（传统方式）：

1. scripts/
   ├── extract_orders.py      # 谁写的？什么时候更新的？
   ├── transform_v2_final.sql # v1 在哪？为什么有 v2？
   ├── load_customers.sh      # 依赖什么？
   └── backup_old/            # 一堆历史遗留...

2. 运行方式：
   - crontab 定时任务（改一次要 SSH 到服务器）
   - 手动执行顺序（记在某个人的笔记本里）
   - 依赖关系不清晰（改一个表，影响哪些下游？不知道）

3. 数据质量：
   - 没有测试（上线后才发现数据错了）
   - 没有文档（只有原作者知道这个字段什么意思）
   - 没有血缘（数据从哪来到哪去？问遍团队）
```

**心智模型**：数据转换是"一次性脚本"的堆砌。

### dbt 的新范式

```
dbt 项目结构：

models/
├── staging/           # 第一层：标准化原始数据
│   ├── _stg_sources.yml
│   ├── stg_orders.sql
│   └── stg_customers.sql
├── intermediate/      # 第二层：业务逻辑
│   ├── int_customer_orders.sql
│   └── int_order_items.sql
└── marts/            # 第三层：面向分析
    ├── finance/
    │   └── fct_revenue.sql
    └── marketing/
        └── dim_customer_segments.sql

tests/
├── generic/          # 通用测试
└── singular/         # 自定义测试

心智模型转变：
✅ SQL 即代码（版本控制、Code Review）
✅ 声明式依赖（自动推导执行顺序）
✅ 测试驱动（数据质量内置）
✅ 自动文档（血缘图自动生成）
```

---

## 案例一：从脚本到模型 - 基础思维转变

### 业务场景

你是一家电商公司的数据分析师，每天需要生成"每日订单汇总报表"。

### 传统脚本方式

```sql
-- daily_report.sql（在某个文件夹里，手动执行）

-- 步骤1：清理临时表
DROP TABLE IF EXISTS tmp_orders_today;

-- 步骤2：提取今日订单
CREATE TABLE tmp_orders_today AS
SELECT
    order_id,
    customer_id,
    order_date,
    total_amount,
    status
FROM raw_orders
WHERE DATE(order_date) = CURRENT_DATE;

-- 步骤3：关联客户信息
DROP TABLE IF EXISTS tmp_customer_orders;

CREATE TABLE tmp_customer_orders AS
SELECT
    o.order_id,
    o.order_date,
    o.total_amount,
    c.customer_name,
    c.customer_segment
FROM tmp_orders_today o
LEFT JOIN raw_customers c ON o.customer_id = c.id;

-- 步骤4：生成报表
DROP TABLE IF EXISTS daily_revenue_report;

CREATE TABLE daily_revenue_report AS
SELECT
    customer_segment,
    COUNT(*) as order_count,
    SUM(total_amount) as revenue,
    AVG(total_amount) as avg_order_value
FROM tmp_customer_orders
GROUP BY customer_segment;

-- 步骤5：清理临时表
DROP TABLE tmp_orders_today;
DROP TABLE tmp_customer_orders;
```

**问题清单**：
- ❌ 必须手动执行（或写复杂的调度脚本）
- ❌ 临时表管理混乱（tmp_ 前缀到处都是）
- ❌ 没有依赖管理（如果 raw_customers 表结构变了？）
- ❌ 没有版本控制（上周的逻辑是什么？不知道）
- ❌ 难以测试（如何验证数据正确性？）
- ❌ 无法复用（类似逻辑要重写）

### 第一次思维转变：模型化思维

**dbt 方案**：

```yaml
# models/staging/sources.yml
version: 2

sources:
  - name: raw
    description: "原始数据源"
    tables:
      - name: orders
        description: "订单表"
        columns:
          - name: order_id
            description: "订单唯一标识"
            tests:
              - unique
              - not_null
      - name: customers
        description: "客户表"
```

```sql
-- models/staging/stg_orders.sql
-- 模型1：标准化订单数据

WITH source AS (
    SELECT * FROM {{ source('raw', 'orders') }}
),

renamed AS (
    SELECT
        order_id,
        customer_id,
        order_date,
        total_amount,
        status,
        -- 数据清洗
        CASE
            WHEN status IN ('paid', 'shipped', 'delivered') THEN status
            ELSE 'other'
        END AS cleaned_status
    FROM source
    WHERE order_date >= '2024-01-01'  -- 只处理有效数据
)

SELECT * FROM renamed
```

```sql
-- models/staging/stg_customers.sql
-- 模型2：标准化客户数据

SELECT
    id AS customer_id,
    name AS customer_name,
    COALESCE(segment, 'unknown') AS customer_segment,
    created_at AS customer_created_at
FROM {{ source('raw', 'customers') }}
```

```sql
-- models/marts/finance/fct_daily_revenue.sql
-- 模型3：每日收入事实表

WITH orders AS (
    SELECT * FROM {{ ref('stg_orders') }}
),

customers AS (
    SELECT * FROM {{ ref('stg_customers') }}
),

joined AS (
    SELECT
        o.order_id,
        o.order_date,
        o.total_amount,
        c.customer_name,
        c.customer_segment
    FROM orders o
    LEFT JOIN customers c ON o.customer_id = c.customer_id
    WHERE DATE(o.order_date) = CURRENT_DATE
),

aggregated AS (
    SELECT
        customer_segment,
        COUNT(*) AS order_count,
        SUM(total_amount) AS revenue,
        AVG(total_amount) AS avg_order_value,
        CURRENT_DATE AS report_date
    FROM joined
    GROUP BY customer_segment
)

SELECT * FROM aggregated
```

**运行方式**：

```bash
# 开发环境：运行单个模型
dbt run --select stg_orders

# 生产环境：运行所有模型
dbt run

# dbt 自动：
# 1. 解析依赖关系（stg_orders → fct_daily_revenue）
# 2. 按顺序执行
# 3. 创建/更新表或视图
# 4. 记录运行日志
```

**关键洞察**：

| 维度 | 传统脚本思维 | dbt 模型思维 |
|-----|------------|------------|
| **代码组织** | 过程式（做什么） | 声明式（是什么） |
| **执行方式** | 手动/Cron | 自动依赖解析 |
| **复用性** | 复制粘贴 | ref() 引用 |
| **测试** | 手动检查 | 内置测试框架 |
| **文档** | Word/Wiki | 代码即文档 |
| **版本控制** | 文件命名 v1/v2 | Git |

### 深入理解：ref() 函数的魔法

```sql
-- 传统方式：硬编码表名
SELECT * FROM tmp_customers  -- 如果表名改了？全局搜索替换

-- dbt 方式：引用模型
SELECT * FROM {{ ref('stg_customers') }}

-- dbt 做了什么：
-- 1. 在开发环境，生成：dev_schema.stg_customers
-- 2. 在生产环境，生成：prod_schema.stg_customers
-- 3. 自动追踪依赖：stg_customers 必须先于当前模型运行
-- 4. 生成血缘图：可视化数据流向
```

**心智模型突破**：

```
传统思维：SQL 脚本 = 一次性执行的指令
         (改一次，到处找依赖)

dbt 思维：SQL 模型 = 可复用的数据转换单元
         (改一次，依赖自动更新)
```

---

## 案例二：分层建模 - 架构思维

### 业务场景

电商公司发展壮大，数据需求爆发：
- 财务部门要收入报表
- 营销部门要客户分群
- 运营部门要库存预警
- 产品部门要用户行为分析

### 传统方式的混乱

```
所有需求都直接从原始表查询：

SELECT ... FROM raw_orders ...  -- 财务的查询
SELECT ... FROM raw_orders ...  -- 营销的查询
SELECT ... FROM raw_orders ...  -- 运营的查询

问题：
1. 重复逻辑（每个部门都写一遍订单清洗逻辑）
2. 不一致（财务和营销对"有效订单"的定义不同）
3. 性能差（每次都扫描原始表）
4. 难维护（原始表加字段，所有查询都要改）
```

### 第二次思维转变：分层架构

**dbt 分层哲学**：

```
数据流向（从左到右）：

原始数据源              staging 层              intermediate 层        marts 层
───────────            ──────────            ──────────────        ──────────
raw_orders      →      stg_orders      →     int_customer_orders  → fct_revenue (财务)
raw_customers   →      stg_customers   →                          → dim_customers (营销)
raw_products    →      stg_products    →     int_product_metrics  → fct_inventory (运营)
raw_events      →      stg_events      →     int_user_sessions    → fct_user_behavior (产品)

每一层的职责：
┌─────────────────────────────────────────────────────────────────┐
│ staging: 1对1映射原始表，只做最基础的清洗（重命名、类型转换）     │
│ intermediate: 业务逻辑层，复杂的JOIN、计算、窗口函数           │
│ marts: 面向业务的最终表，直接支撑报表和分析                    │
└─────────────────────────────────────────────────────────────────┘
```

**实战案例**：

```sql
-- models/staging/stg_orders.sql
-- 职责：标准化字段、基础过滤

{{ config(materialized='view') }}  -- 轻量级，用视图

SELECT
    order_id,
    customer_id,
    product_id,
    order_date,
    -- 标准化金额字段（统一货币单位）
    total_amount_cents / 100.0 AS total_amount,
    -- 标准化状态（统一枚举值）
    LOWER(TRIM(status)) AS status,
    created_at,
    updated_at
FROM {{ source('raw', 'orders') }}
WHERE
    order_date >= '2024-01-01'  -- 只保留有效期数据
    AND is_deleted = FALSE      -- 排除软删除
```

```sql
-- models/intermediate/int_order_daily_stats.sql
-- 职责：复杂业务逻辑、聚合计算

{{ config(materialized='table') }}  -- 中间结果，用表

WITH orders AS (
    SELECT * FROM {{ ref('stg_orders') }}
),

customers AS (
    SELECT * FROM {{ ref('stg_customers') }}
),

-- 业务逻辑1：计算订单状态
order_status AS (
    SELECT
        order_id,
        customer_id,
        order_date,
        total_amount,
        CASE
            WHEN status IN ('paid', 'shipped') THEN 'active'
            WHEN status = 'delivered' THEN 'completed'
            WHEN status = 'cancelled' THEN 'cancelled'
            ELSE 'other'
        END AS order_status_group
    FROM orders
),

-- 业务逻辑2：关联客户维度
enriched AS (
    SELECT
        o.*,
        c.customer_segment,
        c.customer_tier,
        -- 计算客户生命周期天数
        DATEDIFF('day', c.first_order_date, o.order_date) AS days_since_first_order
    FROM order_status o
    LEFT JOIN customers c ON o.customer_id = c.customer_id
),

-- 业务逻辑3：按日聚合
daily_aggregated AS (
    SELECT
        DATE(order_date) AS order_date,
        customer_segment,
        order_status_group,
        COUNT(DISTINCT order_id) AS order_count,
        COUNT(DISTINCT customer_id) AS unique_customers,
        SUM(total_amount) AS total_revenue,
        AVG(total_amount) AS avg_order_value,
        -- 高级指标
        PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY total_amount) AS median_order_value
    FROM enriched
    GROUP BY 1, 2, 3
)

SELECT * FROM daily_aggregated
```

```sql
-- models/marts/finance/fct_revenue.sql
-- 职责：面向财务部门的收入事实表

{{ config(
    materialized='incremental',
    unique_key='report_date'
) }}

SELECT
    order_date AS report_date,
    customer_segment,
    SUM(total_revenue) AS daily_revenue,
    SUM(order_count) AS daily_orders,
    daily_revenue / NULLIF(daily_orders, 0) AS avg_order_value,
    -- 财务特有指标
    SUM(CASE WHEN order_status_group = 'completed' THEN total_revenue ELSE 0 END) AS confirmed_revenue
FROM {{ ref('int_order_daily_stats') }}
GROUP BY 1, 2

{% if is_incremental() %}
    -- 增量更新：只处理新数据
    WHERE order_date > (SELECT MAX(report_date) FROM {{ this }})
{% endif %}
```

```sql
-- models/marts/marketing/dim_customer_segments.sql
-- 职责：面向营销部门的客户维度表

SELECT
    customer_id,
    customer_segment,
    -- 营销特有指标
    SUM(order_count) AS lifetime_orders,
    SUM(total_revenue) AS lifetime_value,
    MAX(order_date) AS last_order_date,
    MIN(order_date) AS first_order_date,
    DATEDIFF('day', first_order_date, last_order_date) AS customer_age_days,
    -- 分群逻辑
    CASE
        WHEN lifetime_value > 10000 THEN 'VIP'
        WHEN lifetime_value > 5000 THEN 'High Value'
        WHEN lifetime_value > 1000 THEN 'Medium Value'
        ELSE 'Low Value'
    END AS value_tier
FROM {{ ref('int_order_daily_stats') }}
GROUP BY customer_id, customer_segment
```

**项目结构**：

```
models/
├── staging/
│   ├── _sources.yml              # 定义数据源
│   ├── stg_orders.sql            # 1:1 映射
│   ├── stg_customers.sql
│   └── stg_products.sql
│
├── intermediate/
│   ├── _int_models.yml           # 中间层文档
│   ├── int_order_daily_stats.sql # 复杂逻辑
│   ├── int_customer_metrics.sql
│   └── int_product_inventory.sql
│
└── marts/
    ├── finance/
    │   ├── _finance_models.yml
    │   ├── fct_revenue.sql       # 财务事实表
    │   └── dim_accounting.sql
    ├── marketing/
    │   ├── _marketing_models.yml
    │   ├── dim_customer_segments.sql  # 营销维度表
    │   └── fct_campaigns.sql
    └── core/
        ├── dim_date.sql          # 共享维度表
        └── dim_product.sql
```

**心智模型升级**：

```
Level 0 (传统脚本):
  每个报表 = 独立脚本
  问题：重复、不一致、难维护

Level 1 (dbt 基础):
  每个报表 = dbt 模型
  改进：版本控制、依赖管理

Level 2 (dbt 分层):
  数据流 = staging → intermediate → marts
  升华：复用、一致、分工明确

Level 3 (dbt 架构):
  数据仓库 = 模块化的数据产品
  境界：每一层都有清晰的接口契约
```

### 实践原则

**staging 层原则**：
1. ✅ 1:1 映射源表（一个源表对应一个 staging 模型）
2. ✅ 只做最基础的清洗（重命名、类型转换、简单过滤）
3. ✅ 使用 `view` 物化（节省存储）
4. ❌ 不做 JOIN（保持单一数据源）
5. ❌ 不做复杂计算（留给 intermediate）

**intermediate 层原则**：
1. ✅ 复杂业务逻辑（JOIN、窗口函数、计算字段）
2. ✅ 可复用的中间结果（被多个 marts 引用）
3. ✅ 使用 `table` 物化（提升性能）
4. ✅ 以 `int_` 开头命名
5. ❌ 不对外暴露（仅内部使用）

**marts 层原则**：
1. ✅ 面向业务部门（财务、营销、运营）
2. ✅ 直接支撑报表和分析
3. ✅ 考虑增量更新（性能优化）
4. ✅ 完善的文档和测试
5. ✅ 遵循维度建模（事实表 fct_、维度表 dim_）

---

## 案例三：增量更新 - 性能优化思维

### 业务场景

订单表每天新增 100 万行，历史数据已有 10 亿行。
如果每次都全量重建表，需要 4 小时，无法满足业务需求（每天早上 8 点要看到昨天的数据）。

### 传统全量更新的瓶颈

```sql
-- 每次都处理全部 10 亿行
CREATE OR REPLACE TABLE daily_revenue AS
SELECT
    DATE(order_date) AS report_date,
    SUM(total_amount) AS revenue
FROM raw_orders  -- 扫描全表，4 小时
GROUP BY 1;
```

**问题**：
- 资源浪费（99.9% 的数据没变，但每次都重算）
- 时间长（随着数据增长，越来越慢）
- 成本高（云数仓按计算量收费）

### 第三次思维转变：增量思维

**dbt 增量策略**：

```sql
-- models/marts/finance/fct_daily_revenue.sql

{{ config(
    materialized='incremental',
    unique_key='report_date',
    incremental_strategy='merge'
) }}

WITH orders AS (
    SELECT * FROM {{ ref('stg_orders') }}

    {% if is_incremental() %}
        -- 增量模式：只处理新数据
        WHERE order_date > (SELECT MAX(report_date) FROM {{ this }})
    {% endif %}
),

aggregated AS (
    SELECT
        DATE(order_date) AS report_date,
        customer_segment,
        COUNT(*) AS order_count,
        SUM(total_amount) AS revenue
    FROM orders
    GROUP BY 1, 2
)

SELECT * FROM aggregated
```

**执行逻辑**：

```
首次运行（全量）：
┌─────────────────────────────────────┐
│ 1. is_incremental() = False         │
│ 2. 处理所有历史数据                  │
│ 3. 创建表 fct_daily_revenue         │
│ 4. 耗时：4 小时                      │
└─────────────────────────────────────┘

第二次运行（增量）：
┌─────────────────────────────────────┐
│ 1. is_incremental() = True          │
│ 2. 只处理昨天的新数据                │
│ 3. MERGE 到现有表                    │
│ 4. 耗时：30 秒                       │
└─────────────────────────────────────┘

性能对比：4小时 → 30秒（480倍提速）
```

### 增量策略选择

**策略 1：append（追加）**

```sql
{{ config(
    materialized='incremental',
    incremental_strategy='append'
) }}

-- 适用场景：只插入，不更新
-- 例如：日志表、事件表

SELECT
    event_id,
    user_id,
    event_type,
    event_time
FROM {{ ref('stg_events') }}

{% if is_incremental() %}
    WHERE event_time > (SELECT MAX(event_time) FROM {{ this }})
{% endif %}
```

**执行逻辑**：
```sql
-- append 策略生成的 SQL（Snowflake 示例）
INSERT INTO prod.fct_events
SELECT * FROM new_data;  -- 只插入，不检查重复
```

**优点**：最快（无需检查重复）
**缺点**：无法处理更新（如订单状态变更）

---

**策略 2：merge（合并）**

```sql
{{ config(
    materialized='incremental',
    unique_key='order_id',
    incremental_strategy='merge'
) }}

-- 适用场景：有更新需求
-- 例如：订单表（状态会变化）

SELECT
    order_id,
    customer_id,
    status,  -- 会更新：pending → paid → shipped
    total_amount,
    updated_at
FROM {{ ref('stg_orders') }}

{% if is_incremental() %}
    WHERE updated_at > (SELECT MAX(updated_at) FROM {{ this }})
{% endif %}
```

**执行逻辑**：
```sql
-- merge 策略生成的 SQL
MERGE INTO prod.fct_orders AS target
USING new_data AS source
ON target.order_id = source.order_id
WHEN MATCHED THEN
    UPDATE SET
        status = source.status,
        updated_at = source.updated_at
WHEN NOT MATCHED THEN
    INSERT (order_id, customer_id, status, ...)
    VALUES (source.order_id, source.customer_id, source.status, ...);
```

**优点**：支持更新和插入（upsert）
**缺点**：比 append 慢（需要匹配 unique_key）

---

**策略 3：delete+insert（删除再插入）**

```sql
{{ config(
    materialized='incremental',
    unique_key='report_date',
    incremental_strategy='delete+insert'
) }}

-- 适用场景：按分区更新
-- 例如：每日报表（整天重算）

SELECT
    DATE(order_date) AS report_date,
    SUM(total_amount) AS revenue
FROM {{ ref('stg_orders') }}

{% if is_incremental() %}
    WHERE order_date >= CURRENT_DATE - INTERVAL '7 days'  -- 重算最近7天
{% endif %}

GROUP BY 1
```

**执行逻辑**：
```sql
-- delete+insert 策略
DELETE FROM prod.fct_daily_revenue
WHERE report_date IN (SELECT DISTINCT report_date FROM new_data);

INSERT INTO prod.fct_daily_revenue
SELECT * FROM new_data;
```

**优点**：简单可靠（先删后插，保证一致性）
**缺点**：对分区表友好，非分区表性能较差

---

### 高级技巧：时间窗口策略

```sql
-- 场景：订单可能延迟到达，需要回溯 3 天

{{ config(
    materialized='incremental',
    unique_key='order_id',
    incremental_strategy='merge'
) }}

SELECT
    order_id,
    order_date,
    total_amount,
    status
FROM {{ ref('stg_orders') }}

{% if is_incremental() %}
    -- 不只是处理今天，而是回溯 3 天
    WHERE order_date >= (
        SELECT DATEADD('day', -3, MAX(order_date))
        FROM {{ this }}
    )
{% endif %}
```

**时间窗口选择指南**：

| 回溯窗口 | 适用场景 | 数据量增加 | 可靠性 |
|---------|---------|-----------|-------|
| 0 天 | 实时数据（无延迟） | 最小 | 低 |
| 1-3 天 | 常规业务数据 | 小 | 中 |
| 7-30 天 | 有上游数据修正 | 中 | 高 |
| 全量 | 数据质量不确定 | 最大 | 最高 |

---

### 常见陷阱与解决方案

**陷阱 1：忘记处理重复数据**

```sql
-- ❌ 错误：append 策略 + 有重复数据
{{ config(incremental_strategy='append') }}

SELECT * FROM source
WHERE date = CURRENT_DATE  -- 如果任务重跑，会插入重复数据
```

```sql
-- ✅ 正确：使用 merge 策略
{{ config(
    incremental_strategy='merge',
    unique_key='event_id'  -- 指定唯一键
) }}
```

---

**陷阱 2：unique_key 选择错误**

```sql
-- ❌ 错误：复合键没有正确设置
{{ config(unique_key='order_id') }}

SELECT
    order_id,
    product_id,  -- 一个订单有多个商品
    quantity
FROM order_items
-- 问题：order_id 不唯一，导致数据丢失
```

```sql
-- ✅ 正确：使用复合键
{{ config(unique_key=['order_id', 'product_id']) }}

-- 或者生成代理键
SELECT
    {{ dbt_utils.generate_surrogate_key(['order_id', 'product_id']) }} AS item_key,
    order_id,
    product_id,
    quantity
FROM order_items
```

---

**陷阱 3：增量条件写错**

```sql
-- ❌ 错误：使用创建时间判断
{% if is_incremental() %}
    WHERE created_at > (SELECT MAX(created_at) FROM {{ this }})
{% endif %}

-- 问题：如果有订单状态更新，更新时间 > 创建时间，会漏掉
```

```sql
-- ✅ 正确：使用更新时间
{% if is_incremental() %}
    WHERE updated_at > (SELECT MAX(updated_at) FROM {{ this }})
{% endif %}
```

---

### 性能对比实测

**测试场景**：1 亿行订单表，每天新增 100 万行

| 策略 | 首次运行 | 增量运行 | 处理数据量 | 存储成本 |
|-----|---------|---------|-----------|---------|
| 全量刷新 | 4 小时 | 4 小时 | 1 亿行 | 正常 |
| append | 4 小时 | 20 秒 | 100 万行 | 正常 |
| merge | 4 小时 | 45 秒 | 100 万行 | 正常 |
| delete+insert | 4 小时 | 35 秒 | 100 万行（按分区） | 稍高 |

**心智模型**：

```
传统思维：每次运行 = 从头开始
         (全量思维)

dbt 增量：每次运行 = 只处理变化
         (增量思维)

高级增量：每次运行 = 处理变化 + 修正窗口
         (时间窗口思维)
```

---

## 案例四：测试驱动 - 数据质量保证

### 业务场景

凌晨 3 点，CEO 给你打电话："为什么今天的收入报表显示负数？"
你查了一个小时，发现是上游 ETL 脚本有 bug，导入了脏数据。
**问题**：为什么不能在数据进入之前就拦截？

### 传统方式：事后补救

```
问题发现链路：
1. 脏数据进入数据仓库（凌晨 1 点）
2. 报表生成（凌晨 2 点）
3. CEO 看到异常（早上 7 点）
4. 打电话质问（早上 7:05）
5. 排查问题（早上 7:30）
6. 修复数据（早上 9:00）
7. 重新生成报表（早上 9:30）

时间损失：8.5 小时
信任损失：无法估量
```

### 第四次思维转变：测试驱动数据开发

**dbt 测试框架**：

```yaml
# models/staging/stg_orders.yml
version: 2

models:
  - name: stg_orders
    description: "标准化订单数据"
    columns:
      - name: order_id
        description: "订单唯一标识"
        tests:
          - unique              # 测试1：唯一性
          - not_null            # 测试2：非空

      - name: total_amount
        description: "订单总金额"
        tests:
          - not_null
          - dbt_utils.expression_is_true:  # 测试3：自定义断言
              expression: ">= 0"
              config:
                severity: error  # 失败则中断

      - name: status
        description: "订单状态"
        tests:
          - accepted_values:    # 测试4：枚举值
              values: ['pending', 'paid', 'shipped', 'delivered', 'cancelled']

      - name: order_date
        tests:
          - dbt_utils.expression_is_true:
              expression: ">= '2024-01-01'"  # 不应有未来日期
          - dbt_utils.expression_is_true:
              expression: "<= CURRENT_DATE"
```

**运行测试**：

```bash
# 开发环境：运行测试
dbt test

# 输出示例
Running with dbt=1.5.0
Found 3 models, 8 tests, 0 snapshots

1 of 8 START test unique_stg_orders_order_id ................. [RUN]
1 of 8 PASS unique_stg_orders_order_id ........................ [PASS in 0.53s]

2 of 8 START test not_null_stg_orders_total_amount ........... [RUN]
2 of 8 FAIL 2 test not_null_stg_orders_total_amount .......... [FAIL 2 in 0.41s]

3 of 8 START test accepted_values_stg_orders_status .......... [RUN]
3 of 8 FAIL 5 test accepted_values_stg_orders_status ......... [FAIL 5 in 0.38s]

Completed with 2 errors and 0 warnings:

Failure in test not_null_stg_orders_total_amount
  Got 2 results, configured to fail if != 0

  compiled SQL at target/compiled/my_project/models/staging/stg_orders.yml
```

**失败时的 SQL**：

```sql
-- dbt 自动生成的测试 SQL
SELECT *
FROM prod.stg_orders
WHERE total_amount IS NULL

-- 返回结果：
-- order_id | total_amount | status
-- ---------|--------------|--------
-- 123456   | NULL         | paid     ← 发现问题！
-- 789012   | NULL         | shipped  ← 发现问题！
```

### 测试类型全景

**1. 通用测试（Generic Tests）**

```yaml
# 内置的 4 种测试
tests:
  - unique                    # 唯一性
  - not_null                  # 非空
  - accepted_values:          # 枚举值
      values: ['A', 'B']
  - relationships:            # 外键约束
      to: ref('other_table')
      field: id
```

**2. 单一测试（Singular Tests）**

```sql
-- tests/assert_revenue_positive.sql
-- 测试：收入不应该是负数

SELECT
    report_date,
    SUM(revenue) AS total_revenue
FROM {{ ref('fct_daily_revenue') }}
GROUP BY report_date
HAVING SUM(revenue) < 0
```

如果查询返回任何行 = 测试失败

**3. dbt_utils 扩展测试**

```yaml
# 需要先安装：dbt deps
tests:
  - dbt_utils.recency:
      datepart: day
      field: updated_at
      interval: 1  # 数据应该在 1 天内更新

  - dbt_utils.at_least_one:
      # 至少有一行数据

  - dbt_utils.cardinality_equality:
      # 两个表行数应该相等
      to: ref('source_table')

  - dbt_utils.unique_combination_of_columns:
      # 复合唯一键
      combination_of_columns:
        - order_id
        - product_id
```

### 实战：建立数据质量门禁

**场景**：防止脏数据流入下游

```yaml
# models/staging/stg_orders.yml
version: 2

models:
  - name: stg_orders
    tests:
      # 表级测试：行数合理性
      - dbt_utils.expression_is_true:
          expression: "COUNT(*) BETWEEN 100000 AND 2000000"
          config:
            severity: warn  # 警告但不中断

    columns:
      - name: order_id
        tests:
          - unique:
              config:
                severity: error     # 失败则中断
                error_if: ">100"    # 超过 100 条重复就报错
                warn_if: ">0"       # 有重复就警告

      - name: total_amount
        tests:
          - not_null:
              where: "status != 'cancelled'"  # 条件测试

          - dbt_utils.expression_is_true:
              expression: "BETWEEN 0 AND 1000000"
              config:
                severity: error

      - name: customer_id
        tests:
          - relationships:
              to: ref('stg_customers')
              field: customer_id
              config:
                severity: warn  # 孤儿订单警告但不中断
```

**CI/CD 集成**：

```yaml
# .github/workflows/dbt_test.yml
name: dbt Test

on: [pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: Install dbt
        run: pip install dbt-snowflake

      - name: Run dbt tests
        run: |
          dbt deps
          dbt run --select state:modified+  # 只运行修改的模型
          dbt test --select state:modified+

      - name: Fail if tests failed
        if: failure()
        run: exit 1
```

**效果**：

```
提交 PR → 自动运行测试 → 发现问题 → 阻止合并

问题发现时间：提交代码后 3 分钟
vs
传统方式：数据进入生产后 8 小时
```

### 测试策略矩阵

| 层级 | 测试重点 | 测试类型 | 严重性 |
|-----|---------|---------|-------|
| staging | 数据完整性 | unique, not_null, accepted_values | error |
| intermediate | 业务逻辑正确性 | 自定义测试（singular） | error |
| marts | 数据新鲜度、一致性 | recency, relationships | warn |

### 心智模型转变

```
传统思维：数据问题 = 事后排查
         "先上线，出问题再说"

测试思维：数据质量 = 代码质量
         "测试不通过 = 不能上线"

TDD 思维：先写测试，再写模型
         "定义预期 → 实现逻辑 → 验证结果"
```

---

## 案例五：文档和血缘 - 可维护性

### 业务场景

新来的数据分析师问你："fct_revenue 表里的 `adjusted_revenue` 字段是什么意思？"
你翻了半天代码，问了三个人，才搞清楚：这是扣除退款后的收入。

**问题**：为什么不能有自解释的文档？

### 传统方式：口口相传

```
文档形式：
- 📄 Word 文档（2 年前的，过期了）
- 💬 邮件线程（谁还记得在哪？）
- 🧠 老员工的大脑（离职了）
- 🔍 代码注释（不一定写）

问题：
- 文档和代码分离（代码改了，文档没改）
- 难以查找（在哪个 Word 文件里？）
- 缺乏上下文（这个字段从哪来？）
```

### 第五次思维转变：代码即文档

**dbt 文档系统**：

```yaml
# models/marts/finance/fct_revenue.yml
version: 2

models:
  - name: fct_revenue
    description: |
      # 财务收入事实表

      ## 业务定义
      记录每日按客户细分的收入数据，用于财务报表和分析。

      ## 更新频率
      每日凌晨 2:00 更新（增量）

      ## 数据源
      - stg_orders: 订单明细
      - stg_customers: 客户主数据

      ## 负责人
      - 开发：data-team@company.com
      - 业务：finance-team@company.com

      ## 变更历史
      - 2024-01-15: 新增 adjusted_revenue 字段
      - 2024-02-01: 修改客户分群逻辑

    columns:
      - name: report_date
        description: "报表日期（订单日期）"
        tests:
          - not_null
          - unique

      - name: customer_segment
        description: |
          客户细分维度：
          - VIP: 年消费 > 10 万
          - Premium: 年消费 > 5 万
          - Standard: 年消费 > 1 万
          - Basic: 其他

      - name: revenue
        description: "原始收入（订单金额总和）"
        tests:
          - not_null
          - dbt_utils.expression_is_true:
              expression: ">= 0"

      - name: adjusted_revenue
        description: |
          **调整后收入**

          计算公式：
          ```
          revenue - refunds + adjustments
          ```

          说明：
          - 扣除了退款金额
          - 加上了手动调整（如折扣）
          - 用于财务对账

          注意事项：
          - 可能为负数（退款 > 原订单）
          - 与财务系统口径一致
        meta:
          metrics:
            -财务报表主指标
          sla:
            - 每日 8:00 前更新
```

**生成文档网站**：

```bash
# 生成文档
dbt docs generate

# 启动文档服务器
dbt docs serve

# 浏览器访问 http://localhost:8080
```

**文档网站功能**：

```
功能清单：
┌─────────────────────────────────────────┐
│ 1. 模型列表                              │
│    - 按文件夹分组                        │
│    - 搜索功能                            │
│    - 标签筛选                            │
│                                         │
│ 2. 血缘图（Lineage Graph）               │
│    - 可视化数据流向                      │
│    - 点击节点查看详情                    │
│    - 支持放大/缩小/搜索                  │
│                                         │
│ 3. 模型详情页                            │
│    - 模型描述（Markdown 渲染）           │
│    - 字段列表和说明                      │
│    - 编译后的 SQL                        │
│    - 依赖关系                            │
│    - 测试结果                            │
│                                         │
│ 4. 源表文档                              │
│    - 数据源描述                          │
│    - 数据新鲜度                          │
│    - 表结构                              │
└─────────────────────────────────────────┘
```

### 血缘图的威力

**场景**：上游表 `raw_orders` 要加一个字段 `discount_amount`

**传统方式**：
```
问题：这个改动会影响哪些下游？
答案：不知道，只能：
1. 全局搜索表名（可能遗漏）
2. 问遍团队（浪费时间）
3. 上线后出问题（生产故障）
```

**dbt 方式**：
```
1. 打开文档网站
2. 搜索 "raw_orders"
3. 点击血缘图
4. 一目了然：

raw_orders
    ↓
stg_orders
    ↓
int_order_daily_stats
    ↓            ↓
fct_revenue  dim_customer_segments
    ↓            ↓
财务报表      营销仪表板

影响范围：5 个模型，2 个报表
预计改动时间：30 分钟
```

### 高级文档技巧

**1. 使用宏生成动态文档**

```sql
-- macros/generate_column_docs.sql
{% macro generate_column_docs(column_list) %}
  {% for col in column_list %}
  - name: {{ col.name }}
    description: {{ col.description }}
    {% if col.tests %}
    tests:
      {% for test in col.tests %}
      - {{ test }}
      {% endfor %}
    {% endif %}
  {% endfor %}
{% endmacro %}
```

**2. 链接外部资源**

```yaml
models:
  - name: fct_revenue
    description: |
      财务收入事实表

      相关文档：
      - [业务需求文档](https://wiki.company.com/revenue-requirements)
      - [字段映射表](https://docs.google.com/spreadsheets/d/xxx)
      - [Looker 仪表板](https://looker.company.com/revenue-dashboard)
```

**3. 添加元数据**

```yaml
models:
  - name: fct_revenue
    meta:
      owner: "data-team"
      sla: "daily by 8am"
      tier: "critical"  # 关键表
      contains_pii: false
    config:
      tags: ["finance", "daily", "incremental"]
```

**4. 自动化文档部署**

```yaml
# .github/workflows/deploy_docs.yml
name: Deploy dbt Docs

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: Generate docs
        run: |
          dbt deps
          dbt docs generate

      - name: Deploy to S3
        run: |
          aws s3 sync target/ s3://company-dbt-docs/

      - name: Notify team
        run: |
          curl -X POST $SLACK_WEBHOOK \
            -d '{"text":"📚 dbt 文档已更新！查看：https://docs.company.com"}'
```

### 文档即知识管理

**心智模型转变**：

```
Level 0 (无文档):
  知识 = 口口相传
  问题：人员流动 → 知识丢失

Level 1 (Word 文档):
  知识 = 文档
  问题：文档与代码分离 → 过期

Level 2 (dbt 文档):
  知识 = 代码 + 描述
  优势：文档和代码同步更新

Level 3 (知识图谱):
  知识 = 文档 + 血缘 + 测试 + 运行历史
  境界：自解释的数据资产
```

---

## 案例六：宏和包 - 代码复用

### 业务场景

你发现团队里每个人都在写类似的代码：
- 计算两个日期之间的工作日
- 生成代理键（surrogate key）
- 脱敏 PII 数据
- 货币转换

每个人的实现都略有不同，导致结果不一致。

### 传统方式：复制粘贴

```sql
-- analyst_A 的实现
SELECT MD5(CONCAT(order_id, product_id)) AS key ...

-- analyst_B 的实现
SELECT SHA256(order_id || '-' || product_id) AS key ...

-- analyst_C 的实现
SELECT CONCAT(order_id, '_', product_id) AS key ...

问题：
- 不一致（3 种不同算法）
- 重复代码（到处复制粘贴）
- 难维护（要改所有地方）
```

### 第六次思维转变：宏和包

**dbt 宏（Macros）**：

```sql
-- macros/generate_surrogate_key.sql
{% macro generate_surrogate_key(columns) %}
    MD5(CONCAT(
        {% for col in columns %}
            COALESCE(CAST({{ col }} AS STRING), '')
            {% if not loop.last %} , '|' , {% endif %}
        {% for %}
    ))
{% endmacro %}
```

**使用宏**：

```sql
-- models/marts/fct_order_items.sql
SELECT
    {{ generate_surrogate_key(['order_id', 'product_id']) }} AS item_key,
    order_id,
    product_id,
    quantity
FROM {{ ref('stg_order_items') }}
```

**编译后的 SQL**：

```sql
SELECT
    MD5(CONCAT(
        COALESCE(CAST(order_id AS STRING), '') , '|' ,
        COALESCE(CAST(product_id AS STRING), '')
    )) AS item_key,
    order_id,
    product_id,
    quantity
FROM prod.stg_order_items
```

### 常用宏模式

**1. 动态 SQL 生成**

```sql
-- macros/pivot_columns.sql
{% macro pivot_columns(column, values) %}
    {% for value in values %}
        SUM(CASE WHEN {{ column }} = '{{ value }}' THEN 1 ELSE 0 END) AS {{ value }}
        {% if not loop.last %},{% endif %}
    {% endfor %}
{% endmacro %}
```

```sql
-- 使用
SELECT
    product_category,
    {{ pivot_columns('status', ['pending', 'paid', 'shipped']) }}
FROM orders
GROUP BY product_category

-- 编译为：
SELECT
    product_category,
    SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) AS pending,
    SUM(CASE WHEN status = 'paid' THEN 1 ELSE 0 END) AS paid,
    SUM(CASE WHEN status = 'shipped' THEN 1 ELSE 0 END) AS shipped
FROM orders
GROUP BY product_category
```

**2. 数据脱敏**

```sql
-- macros/mask_pii.sql
{% macro mask_pii(column, mask_type='email') %}
    {% if mask_type == 'email' %}
        CONCAT(
            LEFT({{ column }}, 3),
            '***@',
            SPLIT_PART({{ column }}, '@', 2)
        )
    {% elif mask_type == 'phone' %}
        CONCAT('***-****-', RIGHT({{ column }}, 4))
    {% elif mask_type == 'card' %}
        CONCAT('****-****-****-', RIGHT({{ column }}, 4))
    {% endif %}
{% endmacro %}
```

```sql
-- 使用
SELECT
    customer_id,
    {{ mask_pii('email', 'email') }} AS email,
    {{ mask_pii('phone', 'phone') }} AS phone
FROM {{ ref('stg_customers') }}
```

### dbt 包（Packages）

**安装社区包**：

```yaml
# packages.yml
packages:
  - package: dbt-labs/dbt_utils
    version: 1.1.1

  - package: calogica/dbt_expectations
    version: 0.9.0

  - package: dbt-labs/codegen
    version: 0.11.0
```

```bash
# 安装包
dbt deps
```

**常用包功能**：

**1. dbt_utils**

```sql
-- 生成代理键
{{ dbt_utils.generate_surrogate_key(['order_id', 'product_id']) }}

-- 透视
{{ dbt_utils.pivot(column='status', values=dbt_utils.get_column_values(...)) }}

-- 日期序列
{{ dbt_utils.date_spine(start_date='2024-01-01', end_date='2024-12-31') }}

-- 获取表的所有列
{{ dbt_utils.get_column_values(ref('my_table'), 'status') }}

-- Union 多个表
{{ dbt_utils.union_relations(relations=[ref('table1'), ref('table2')]) }}
```

**2. dbt_expectations（数据质量测试）**

```yaml
# models/schema.yml
tests:
  - dbt_expectations.expect_column_values_to_be_between:
      min_value: 0
      max_value: 100

  - dbt_expectations.expect_column_values_to_match_regex:
      regex: "^[A-Z]{3}$"  # 3个大写字母

  - dbt_expectations.expect_table_row_count_to_be_between:
      min_value: 1000
      max_value: 1000000
```

**3. codegen（代码生成）**

```sql
-- 自动生成 staging 模型
{{ codegen.generate_base_model(
    source_name='raw',
    table_name='orders'
) }}

-- 自动生成 schema.yml
{{ codegen.generate_model_yaml(
    model_names=['stg_orders', 'stg_customers']
) }}
```

### 构建内部包

**场景**：公司有多个 dbt 项目，共享通用逻辑

**项目结构**：

```
company-dbt-utils/  (内部包)
├── macros/
│   ├── business_metrics.sql
│   ├── data_masking.sql
│   └── currency_conversion.sql
├── models/
│   └── utils/
│       └── dim_date.sql  # 共享日期维度表
└── dbt_project.yml

project-A/  (业务项目)
├── packages.yml  # 引用内部包
├── models/
└── dbt_project.yml
```

**内部包（company-dbt-utils）**：

```sql
-- macros/business_metrics.sql
{% macro calculate_ltv(revenue_column, customer_id_column, months=12) %}
    SUM({{ revenue_column }}) OVER (
        PARTITION BY {{ customer_id_column }}
        ORDER BY order_date
        ROWS BETWEEN {{ months }} PRECEDING AND CURRENT ROW
    )
{% endmacro %}
```

**业务项目使用**：

```yaml
# project-A/packages.yml
packages:
  - git: "https://github.com/company/company-dbt-utils.git"
    revision: v1.2.0
```

```sql
-- project-A/models/fct_customer_ltv.sql
SELECT
    customer_id,
    {{ calculate_ltv('revenue', 'customer_id', months=12) }} AS ltv_12m
FROM {{ ref('fct_orders') }}
```

### 心智模型总结

```
传统思维：代码 = 一次性脚本
         复用 = 复制粘贴

宏思维：代码 = 可参数化的模板
       复用 = 函数调用

包思维：代码 = 可分发的库
       复用 = 依赖管理

企业思维：代码 = 数据产品
         复用 = 平台能力
```

---

## 综合案例：构建现代数据栈

### 业务背景

一家 SaaS 公司，数据来源：
- PostgreSQL（业务数据库）
- Stripe（支付数据）
- Google Analytics（网站数据）
- Salesforce（CRM 数据）

目标：构建统一数据仓库，支撑各部门分析需求。

### 完整项目结构

```
dbt_project/
├── dbt_project.yml
├── packages.yml
├── profiles.yml
│
├── models/
│   ├── staging/
│   │   ├── postgres/
│   │   │   ├── _postgres_sources.yml
│   │   │   ├── stg_postgres_users.sql
│   │   │   ├── stg_postgres_subscriptions.sql
│   │   │   └── stg_postgres_events.sql
│   │   ├── stripe/
│   │   │   ├── _stripe_sources.yml
│   │   │   ├── stg_stripe_charges.sql
│   │   │   └── stg_stripe_customers.sql
│   │   └── ga/
│   │       └── stg_ga_sessions.sql
│   │
│   ├── intermediate/
│   │   ├── _int_models.yml
│   │   ├── int_user_subscription_history.sql
│   │   ├── int_monthly_recurring_revenue.sql
│   │   └── int_user_engagement.sql
│   │
│   └── marts/
│       ├── finance/
│       │   ├── _finance_models.yml
│       │   ├── fct_mrr.sql
│       │   ├── fct_arr.sql
│       │   └── dim_customers.sql
│       ├── product/
│       │   ├── fct_user_engagement.sql
│       │   └── fct_feature_adoption.sql
│       └── core/
│           └── dim_date.sql
│
├── tests/
│   └── assert_mrr_is_positive.sql
│
├── macros/
│   ├── calculate_mrr.sql
│   └── generate_schema_name.sql
│
└── snapshots/
    └── subscription_snapshot.sql
```

### 核心模型示例

```sql
-- models/intermediate/int_monthly_recurring_revenue.sql
{{ config(
    materialized='incremental',
    unique_key='date_month',
    cluster_by=['date_month']
) }}

WITH subscriptions AS (
    SELECT * FROM {{ ref('stg_postgres_subscriptions') }}
    {% if is_incremental() %}
    WHERE updated_at >= (SELECT MAX(date_month) FROM {{ this }})
    {% endif %}
),

charges AS (
    SELECT * FROM {{ ref('stg_stripe_charges') }}
    {% if is_incremental() %}
    WHERE charge_date >= (SELECT MAX(date_month) FROM {{ this }})
    {% endif %}
),

date_spine AS (
    {{ dbt_utils.date_spine(
        datepart="month",
        start_date="cast('2024-01-01' as date)",
        end_date="cast(current_date as date)"
    ) }}
),

mrr_per_month AS (
    SELECT
        DATE_TRUNC('month', s.created_at) AS date_month,
        s.customer_id,
        s.plan_id,
        s.mrr_amount,
        s.status,
        c.total_charges
    FROM subscriptions s
    LEFT JOIN charges c
        ON s.customer_id = c.customer_id
        AND DATE_TRUNC('month', c.charge_date) = DATE_TRUNC('month', s.created_at)
    WHERE s.status IN ('active', 'trialing')
),

aggregated AS (
    SELECT
        date_month,
        COUNT(DISTINCT customer_id) AS active_customers,
        SUM(mrr_amount) AS total_mrr,
        SUM(CASE WHEN date_month = DATE_TRUNC('month', created_at) THEN mrr_amount ELSE 0 END) AS new_mrr,
        SUM(total_charges) AS total_charges
    FROM mrr_per_month
    GROUP BY date_month
)

SELECT * FROM aggregated
```

### 运维最佳实践

**1. 环境管理**

```yaml
# profiles.yml
my_project:
  target: dev
  outputs:
    dev:
      type: snowflake
      account: dev-account
      database: DEV_DB
      schema: dbt_{{ env_var('DBT_USER') }}  # 每个开发者独立 schema

    prod:
      type: snowflake
      account: prod-account
      database: PROD_DB
      schema: ANALYTICS
```

**2. CI/CD 流程**

```yaml
# .github/workflows/dbt_ci.yml
name: dbt CI

on:
  pull_request:
    paths:
      - 'models/**'
      - 'tests/**'

jobs:
  dbt-ci:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v2

      - name: Setup Python
        uses: actions/setup-python@v2

      - name: Install dbt
        run: pip install dbt-snowflake

      - name: Run dbt build on modified models
        run: |
          dbt deps
          dbt build --select state:modified+ --defer --state ./target

      - name: Comment PR with results
        uses: actions/github-script@v6
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: '✅ dbt tests passed!'
            })
```

**3. 监控和告警**

```sql
-- macros/alert_on_test_failure.sql
{% macro alert_on_test_failure() %}
    {% if execute %}
        {% if results %}
            {% for result in results %}
                {% if result.status == 'fail' %}
                    {{ log("❌ Test failed: " ~ result.node.name, info=True) }}
                    -- 发送 Slack 通知
                    {{ send_slack_alert(result.node.name) }}
                {% endif %}
            {% endfor %}
        {% endif %}
    {% endif %}
{% endmacro %}
```

---

## 心智模型总结：完整转变

### 从脚本到工程

```
阶段 0：手工 SQL 脚本
─────────────────────
- 文件夹里的 .sql 文件
- 手动执行或 crontab
- 没有版本控制
- 没有测试
- 没有文档

↓

阶段 1：dbt 基础
─────────────────
- Git 版本控制
- ref() 引用模型
- 自动依赖管理
- 基础测试

↓

阶段 2：分层架构
─────────────────
- staging/intermediate/marts
- 职责清晰
- 代码复用
- 性能优化（增量）

↓

阶段 3：数据工程化
──────────────────
- 测试驱动开发
- 自动化文档
- CI/CD 集成
- 监控告警

↓

阶段 4：数据产品化
──────────────────
- 宏和包复用
- 内部平台
- SLA 保证
- 数据治理
```

### 核心思维转变

| 维度 | 传统 ETL | dbt ELT |
|-----|---------|---------|
| **代码组织** | 脚本堆砌 | 模块化模型 |
| **执行方式** | 过程式 | 声明式 |
| **依赖管理** | 手动编排 | 自动推导 |
| **测试** | 事后发现 | 内置测试 |
| **文档** | 外部维护 | 代码即文档 |
| **复用** | 复制粘贴 | 宏和包 |
| **版本控制** | 文件命名 | Git + PR |
| **协作** | 单人维护 | 团队协作 |
| **性能** | 全量刷新 | 增量更新 |
| **质量** | 人工检查 | 自动化测试 |

---

## 实践建议

### 新项目如何开始

**第 1 周：基础设置**
1. 安装 dbt：`pip install dbt-<adapter>`
2. 初始化项目：`dbt init my_project`
3. 配置数据源：设置 `profiles.yml`
4. 第一个模型：创建 `stg_orders.sql`

**第 2-3 周：分层建模**
1. 建立 staging 层（标准化所有源表）
2. 识别复用逻辑（intermediate 层）
3. 创建业务表（marts 层）
4. 添加基础测试

**第 4 周：工程化**
1. 编写文档（yml 文件）
2. 增量优化（高频更新的表）
3. CI/CD 集成
4. 团队培训

### 常见陷阱

**陷阱 1：过度分层**
```
❌ 错误：
staging → intermediate_1 → intermediate_2 → intermediate_3 → marts

✅ 正确：
staging → intermediate → marts
(大部分情况 3 层足够)
```

**陷阱 2：staging 层做复杂逻辑**
```
❌ 错误：在 staging 层做 JOIN 和聚合
✅ 正确：staging 只做字段标准化
```

**陷阱 3：忽视性能**
```
❌ 错误：所有表都用 table 物化
✅ 正确：staging 用 view，intermediate 和 marts 用 table/incremental
```

**陷阱 4：测试覆盖不足**
```
❌ 错误：只测试 unique/not_null
✅ 正确：添加业务逻辑测试（如收入不为负）
```

---

## 结语：从工具到思维

dbt 不仅仅是一个数据转换工具，它代表了**现代数据工程的最佳实践**：

1. **软件工程思维**：版本控制、测试、文档、CI/CD
2. **声明式思维**：关注"是什么"而不是"怎么做"
3. **模块化思维**：分层架构、职责分离、代码复用
4. **质量思维**：测试驱动、自动化验证、数据 SLA

当你真正掌握了这种思维，会发现：
- 数据开发速度提升（小时 → 分钟）
- 数据质量提升（事前预防 → 事后补救）
- 团队协作提升（清晰的接口和文档）
- 可维护性提升（代码和业务逻辑分离）

**最后的建议**：
- 从小项目开始实践
- 理解分层哲学
- 建立测试文化
- 持续优化迭代

当你开始自然地用"模型"、"血缘"、"增量"这些概念思考数据问题时，你就已经完成了从 ETL 到 ELT、从脚本工程师到数据工程师的转变。
