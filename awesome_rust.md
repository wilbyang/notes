# Awesome Rust (非官方精选中文版)

> 本清单旨在为学习与使用 Rust 的开发者提供一个结构化、简洁、避免信息噪音的导航。所有描述均为原创概述，力求概括项目定位与适用场景，而非复制社区版本。欢迎按文末贡献指引补充改进。 

## 目录

1. 为什么选 Rust
2. 学习与入门资源
3. 社区与资讯
4. 工具链与辅助工具
5. 基础核心 Crate（必备积木）
6. 领域精选库
7. 架构/模式/最佳实践要点
8. 性能与调试分析
9. 安全与可靠性
10. Web / 后端开发
11. 数据库与存储
12. 异步与并发
13. 序列化 / 配置 / 数据交换
14. 测试与质量保障
15. DevOps / 构建与发布
16. FFI & 多语言集成
17. WASM / 前端与边缘运行时
18. 嵌入式 / IoT
19. 游戏与图形
20. 科学计算 / 数据 / AI
21. 分布式与网络协议
22. 安全 / 加密 / 零信任
23. 观察性（日志 / 指标 / Trace）
24. 生态地图速览
25. 贡献与维护说明

---

## 1. 为什么选 Rust

- 零成本抽象：编译器帮助你写出高性能而仍保持可读性的代码。
- 内存与数据竞争安全：所有权 + 借用检查在编译期发现大量潜在缺陷。
- 现代化工具链：`cargo` 统一包管理、构建、测试、发布体验。
- 可渗透全栈：系统编程、后端服务、WebAssembly、嵌入式、数据工程、AI。
- 生态成熟度提升中：主流基础设施（数据库、云、浏览器组件）积极采用。

## 2. 学习与入门资源

- 官方文档：The Rust Programming Language（俗称“Rust Book”） https://doc.rust-lang.org/book/
- Rust By Example：交互式示例驱动 https://doc.rust-lang.org/rust-by-example/
- rustlings：小练习集，帮助熟悉语法 https://github.com/rust-lang/rustlings
- Async Book：理解 async/await 与运行时模型 https://rust-lang.github.io/async-book/
- Rustonomicon：深入 unsafe 语义与底层约束 https://doc.rust-lang.org/nomicon/
- Embedded Rust Book：嵌入式开发指南 https://docs.rust-embedded.org/book/
- Unsafe Code Guidelines：规划中，理解底层行为 https://rust-lang.github.io/unsafe-code-guidelines/
- Edition Guide：版本迁移策略 https://doc.rust-lang.org/edition-guide/
- Crates.io：包索引 https://crates.io
- Docs.rs：文档托管 https://docs.rs

## 3. 社区与资讯

- 官方论坛：https://users.rust-lang.org
- Reddit：r/rust 技术讨论
- Rust Weekly / This Week in Rust：周摘要 https://this-week-in-rust.org/
- Zulip：编译器/语言设计协作 https://rust-lang.zulipchat.com/
- RustConf / Rust Nation / EuroRust：大会与演讲视频（YouTube 搜索）
- GitHub 组织：rust-lang, tokio-rs, servo, paritytech

## 4. 工具链与辅助工具

- rustup：安装与多 toolchain 管理
- cargo-edit：`cargo add/remove/update` 简化依赖操作
- cargo-watch：文件变更自动构建/测试
- cargo-udeps：检测未使用依赖
- cargo-outdated：检查版本更新
- cargo-deny：许可/安全/重复依赖审计
- cargo-audit：已知安全漏洞扫描（Advisory DB）
- cargo-nextest：并行测试执行加速
- just：更友好的命令任务脚本（替代 Makefile）
- cross：跨平台交叉编译（Docker 驱动）
- cargo-binstall：快速安装二进制 crate
- mold / lld：更快的链接器，加速构建
- sccache：远程/本地编译缓存

## 5. 基础核心 Crate（必备积木）

- `anyhow` / `eyre`：应用层错误聚合与简化处理
- `thiserror`：定义结构化错误类型的派生宏
- `serde`：通用序列化框架（JSON/YAML/Bincode 等生态核心）
- `tokio`：主流异步运行时（IO、任务调度、定时器）
- `tracing`：结构化事件与分布式追踪指标收集
- `reqwest`：易用的 HTTP 客户端（基于 hyper）
- `hyper`：低层高性能 HTTP 库
- `parking_lot`：更快的锁实现集合
- `dashmap`：并发 HashMap
- `crossbeam`：MPSC/工具集合，队列、内存管理辅助
- `rayon`：数据并行与工作窃取线程池
- `futures`：Stream/Async combinators 与核心 traits
- `prometheus` / `metrics`：指标收集接口
- `regex`：基于 DFA/NFA 优化的正则引擎
- `chrono` / `time`：时间日期处理
- `uuid`：通用唯一标识生成
- `log`：传统日志 facade（与 env_logger 等搭配）
- `clap`：命令行参数解析（derive 宏简洁）
- `cfg-if`：条件编译简化宏
- `once_cell`：惰性静态初始化
- `serde_json` / `toml` / `yaml-rust`：主流配置格式支持

## 6. 领域精选库

按常用场景快速定位：

- 配置：`config`, `figment`
- 缓存：`moka`, `cached`
- 搜索：`tantivy`（类 Lucene 全文检索）
- 压缩：`flate2`, `xz2`, `zstd`
- 消息队列：`lapin`(AMQP), `rdkafka`(Kafka)
- 事件流：`nats`, `redis`(PubSub), `hazelcast-rs`(社区)
- 金融行情：`ta-lib`（技术分析 C 库绑定）, `rust_decimal`
- 图：`petgraph`
- 任务调度：`cronback`(服务), Crate: `cron`, `job_scheduler`
- 国际化：`icu4x`, `rust-unic`
- PDF / 文档：`pdf`, `lopdf`
- 图像处理：`image`, `fast_image_resize`, `imageproc`
- 音视频：`ffmpeg-next`, `gstreamer-rs`
- 终端 UI：`ratatui` (原 tui-rs), `crossterm`, `indicatif`(进度条)

## 7. 架构 / 模式 / 最佳实践要点

- 数据结构清晰：区分输入 DTO、内部模型、持久化实体。
- 错误分层：库内使用 `thiserror`; 顶层聚合为 `anyhow` 并添加上下文。
- 配置加载：多阶段（默认 -> 文件 -> 环境变量 -> CLI）。
- 可观测性：统一使用 `tracing` + 层 (subscriber) 注入日志、指标、Trace ID。
- 资源生命周期：通过 RAII + Drop 确保连接/文件句柄释放。
- 零拷贝：借用切片 & `Cow`；必要时考虑 `bytes` crate。
- 并发策略：IO 用 async；CPU 密集用 `rayon` 或自建线程池隔离。
- Feature gate：细粒度可选功能减小编译与攻击面。
- Unsafe 隔离：`unsafe` 封装在最小模块并文档化不变量。

## 8. 性能与调试分析

- Benchmark：`criterion`（稳定统计分布）
- Flamegraph：`cargo flamegraph` (perf + 火焰图渲染)
- Heap/Alloc：`dhat-rs`, `jemalloc`, `mimalloc`（替换分配器）
- Profiling：`pprof-rs`（集成采样分析）
- 分析步骤：基准 -> 火焰图热点 -> 算法/数据结构调整 -> 再验证。
- 常见优化：减少锁争用、批量 IO、避免不必要的 clone、使用 `&str` vs `String`。

## 9. 安全与可靠性

- 漏洞审计：`cargo audit` 定期执行 CI。
- 依赖许可：`cargo deny` 控制副本与许可证策略。
- 输入验证：结合 `validator` / 自定义 `FromStr`。避免盲目 `unwrap`。
- Fuzz：`cargo fuzz` (LibFuzzer)；补充 property-based (`proptest`).
- 安全随机：`rand` + `getrandom`；密码学使用专业库，避免自实现。
- Threat Modeling：对边界 (网络/文件/外部命令) 做最小权限与时限控制。

## 10. Web / 后端开发

- 框架：`axum`（组合式 + tower 服务栈），`actix-web`（成熟 Actor 生态），`warp`（filter 链），`poem`（全家桶），`salvo`。
- 中间层：`tower`（Service/Layer 抽象，可用于限流/重试/熔断）。
- GraphQL：`async-graphql`, `juniper`。
- gRPC：`tonic`（基于 tower + prost）。
- WebSocket：`tokio-tungstenite`, `axum` 内集成。
- 模板：`askama`, `tera`, `maud`。
- 身份认证：`jsonwebtoken`, `argon2`/`bcrypt` 密码哈希。

## 11. 数据库与存储

- ORM / 查询：`sqlx`（编译期 SQL 校验 async），`diesel`（静态类型查询 DSL），`sea-orm`（动态/实体风格），`prisma-client-rust`。
- KV & 嵌入式：`sled`, `rocksdb`, `redb`, `sqlite` via `rusqlite`。
- 文档存储：`mongodb` 官方驱动。
- 内存数据结构：`redis`（异步/集群支持）。
- 时间序列/列式：`influxdb`(client), `arrow`(列式内存格式), `parquet`。
- 搜索：`tantivy`。
- 分布式实验：`etcd-client`, `consul`(HTTP API), `surrealdb`.

## 12. 异步与并发

- 运行时：`tokio`, `async-std`。轻量：`smol`，嵌入：`embassy`(嵌入式 async)。
- Channel：`tokio::sync`, `crossbeam-channel`, `flume`, `async-channel`。
- 同步原语：`parking_lot`, `tokio::sync::Mutex/RwLock`, `once_cell`。
- Actor：`actix`, `riker`，实验：`xactor`。
- 调度与限速：`tokio::time`, `governor` (令牌桶)。
- 并行：`rayon` (CPU data parallel), `jobsteal`。

## 13. 序列化 / 配置 / 数据交换

- 通用：`serde` 框架 + `serde_json`、`toml`, `yaml-rust`。
- 二进制：`bincode`, `postcard`(嵌入式), `rmp-serde`(MessagePack), `cbor`。
- IDL / RPC：`prost`(gRPC), `flatbuffers`, `capnproto-rust`, `thrift`(生成器)。
- Config 合并：`config`, `figment`。
- 环境注入：`dotenvy`。

## 14. 测试与质量保障

- 单元测试：内置 `cargo test`。
- 属性测试：`proptest`, `quickcheck`。
- 基准：`criterion`。
- Mock：`mockall`。
- 快照测试：`insta` (文本/结构对比)
- 测试组织：多 crate 工作区，重用公共测试辅助模块；使用 `nextest` 加速。
- 覆盖率：`cargo tarpaulin`。

## 15. DevOps / 构建与发布

- 打包分发：`cargo-dist` 自动生成安装包、发布工件。
- 交叉编译：`cross` / 目标 triples + musl 静态。
- Release 流程：CI 中运行 lint + test + audit + deny + build -> Tag -> dist。
- 镜像构建：多阶段 Docker，用 `cargo chef` 缓存依赖层。
- 二进制安装：`cargo-binstall` / GitHub Release + 签名校验。
- 版本管理：SemVer + `cargo set-version`。

## 16. FFI & 多语言集成

- C/C++：`bindgen` 自动生成绑定，`cxx` 安全桥接。
- 输出头文件：`cbindgen`。
- Python：`pyo3` (原生扩展)，`maturin` 打包发行。
- Node.js：`napi-rs`，`neon`。
- Swift/ObjC：通过 `cbindgen` + bridging header。
- Kotlin/JVM：通过 JNA/JNI（工具：`jni` crate, `j4rs`）。
- 多语言统一：`uniffi`（自动生成跨语言绑定）。

## 17. WASM / 前端与边缘运行时

- 运行时：`wasmtime`(本地执行), `wasmer`。
- 绑定：`wasm-bindgen`，DOM 交互与 JS 桥接。
- 前端框架：`yew`, `leptos`, `dioxus`, `seed`。
- 构建：`trunk`（打包/静态资源），`wasm-pack`（发布 npm 包）。
- 服务边缘：Cloudflare Workers (Rust -> wasm), Fastly Compute@Edge。
- 图形：`wgpu`（跨平台 GPU 抽象，兼容 wasm）。

## 18. 嵌入式 / IoT

- HAL 抽象：`embedded-hal`。
- async 嵌入：`embassy`。
- Cortex-M：`cortex-m`, `rtic`（实时任务框架）。
- 固件加载：`probe-rs` 调试与烧录。
- 嵌入式性能：最小化动态分配，使用 `postcard`/`heapless`。

## 19. 游戏与图形

- 引擎 / 框架：`bevy`（ECS + 渲染管线），`macroquad`（简洁 2D/3D），`ggez`, `fyrox`。
- 渲染：`wgpu`, `glium`, `vulkano`。
- 物理：`rapier`（2D/3D 物理），`nphysics`（旧）。
- 数学：`glam`, `nalgebra`。
- ECS：`hecs`, `legion`。

## 20. 科学计算 / 数据 / AI

- DataFrame：`polars`（高速列式+lazy），`datafusion`（SQL 查询引擎）。
- 数组与线性代数：`ndarray`, `nalgebra`。
- 算法/机器学习：`linfa`（经典 ML 算法集合）。
- 深度学习：`tch-rs`（LibTorch 绑定），`burn`, `candle`。
- 优化：`argmin`。
- 数值：`rug`(任意精度)，`statrs`(统计分布)，`rand`(随机)。
- 可视化：`plotters`，`egui`（交互 GUI）。

## 21. 分布式与网络协议

- RPC：`tonic`(gRPC), `tarpc`。
- QUIC：`quinn`, `s2n-quic`。
- P2P：`libp2p`。
- 序列化高效：`prost`, `flatbuffers`, `capnproto`。
- 服务发现：`etcd-client`, `consul`。
- 协议实现：`mqtt` crates, `coap`, `tls` via `rustls`。
- 负载均衡/中间件：`tower` 生态 + 自定义 Layer。

## 22. 安全 / 加密 / 零信任

- TLS：`rustls`（纯 Rust），`native-tls`（系统库封装）。
- 密码学原语：`ring`, `orion`, `dalek` 系列 (ed25519, curve25519)。
- Key 管理：`age`（文件加密格式），`openssl` 绑定（必要时）。
- 密码哈希：`argon2`, `bcrypt`, `scrypt`。
- 机密内存：`secrecy` crate 包装敏感数据。
- 安全对比：`subtle` 防时序攻击操作。

## 23. 观察性（日志 / 指标 / Trace）

- 结构化事件：`tracing` + `tracing-subscriber`。
- OTLP：`opentelemetry` + `tracing-opentelemetry`。
- 指标：`metrics` 框架或 `prometheus` 直接暴露。
- 日志兼容：`tracing-log` 与传统 `log` 库桥接。
- 分析面板：Prometheus + Grafana；OpenTelemetry Collector；Jaeger/Tempo。

## 24. 生态地图速览

```
语言层：编译器 rustc / 标准库 / cargo
运行时：tokio / async-std / smol
Web：axum / actix-web / warp / poem / salvo
数据：polars / arrow / parquet / datafusion
数据库：sqlx / diesel / sea-orm / sled / rocksdb
网络协议：hyper / tonic / quinn / libp2p
分布式：etcd-client / tarpc / s2n-quic
安全：rustls / ring / orion / argon2
前端 & Wasm：wasm-bindgen / yew / leptos / wgpu
游戏：bevy / macroquad / rapier
AI/ML：linfa / tch-rs / candle / burn
工具：cargo-* 家族 / just / cross / sccache
观测：tracing / opentelemetry / metrics
```

## 25. 贡献与维护说明

欢迎提交补充：

1. 保持简洁——一句话写清用途与特色。
2. 避免主观宣传语；关注事实（性能特征、生态定位、可替代性）。
3. 提交前检查是否已有同类库，如重复请说明差异点。
4. 安全或低维护状态的库需标注（如归档、无人维护）。
5. 引用链接优先：官方主页 / GitHub / docs.rs。

维护策略：

- 定期（季度）审视：移除不再维护或被更优方案取代的库。
- 标记风险：对存在安全公告未修复的依赖加入警告。
- 版本更新：跟踪 Rust Edition 与主流生态变动（async 改进、稳定特性）。

## 附录：常见选择对比速览

| 场景 | 推荐首选 | 替代方案 | 备注 |
|------|----------|----------|------|
| HTTP 服务器 | axum | actix-web / warp | tower 生态友好、组合式中间件 |
| ORM vs 原生 | sqlx(异步+原生 SQL) | diesel / sea-orm | sqlx 编译期校验；diesel 类型安全 DSL |
| 异步运行时 | tokio | async-std / smol | tokio 生态最广；smol 极简 |
| 并行计算 | rayon | crossbeam + 自建池 | rayon 适合数据迭代 |
| 日志/追踪 | tracing | log + env_logger | tracing 支持结构化 & span |
| gRPC | tonic | tarpc | tonic 与 prost + tower 深度集成 |
| 序列化 | serde + serde_json | bincode / postcard | 二进制更紧凑；postcard 适嵌入式 |
| 测试加速 | nextest | 默认 cargo test | 并发+失败隔离更好 |
| Fuzz | cargo-fuzz | honggfuzz-rs | LibFuzzer 主流集成 |
| Wasm 前端 | leptos | yew / dioxus | SSR + 状态管理现代化 |

## 免责声明

本清单不保证绝对完整或绝对最新；选择库仍需结合项目需求、维护状态与安全审计结果。请在生产前评估内存、并发、许可与社区活跃度。

---

若你觉得该列表有价值，可以：
- 点赞 / 收藏 / 分享给正在选择 Rust 技术栈的同事
- 提 Pull Request 补充遗漏或新星项目

祝编码顺利！⚙️🚀

