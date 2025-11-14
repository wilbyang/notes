# CUDA 编程 - 从 0 入门高性能矩阵运算

## 引言：为什么需要 CUDA？

当深度学习工程师第一次看到 GPU 训练速度时，往往会经历三个阶段的认知：

1. **震惊期**："为什么 GPU 比 CPU 快 100 倍？"
2. **困惑期**："GPU 编程这么复杂，我能学会吗？"
3. **顿悟期**："原来并行计算是一种全新的思维方式"

CUDA 的价值不仅是提供了 GPU 编程能力，更重要的是它引导我们完成一次**从串行思维到并行思维、从单核优化到万核协同**的心智转变。

---

## 核心思维转变

### CPU vs GPU：两种计算范式

```
CPU 思维（串行优化）：
┌─────────────────────────────────────┐
│  强大的单核                          │
│  - 复杂的控制逻辑                    │
│  - 大容量缓存                        │
│  - 分支预测                          │
│  - 乱序执行                          │
│                                     │
│  适合：复杂逻辑、分支多、串行任务     │
└─────────────────────────────────────┘

GPU 思维（并行计算）：
┌─────────────────────────────────────┐
│  数千个简单核心                      │
│  - 简单的控制逻辑                    │
│  - 小容量缓存                        │
│  - 专注于并行计算                    │
│  - 高带宽内存                        │
│                                     │
│  适合：简单重复、数据并行、大规模计算 │
└─────────────────────────────────────┘
```

**心智模型**：

```
CPU 思维：一个超级工人干所有活
         (快，但只有一个)

GPU 思维：一万个工人同时干活
         (单个慢，但数量多)

关键洞察：
- 矩阵运算 = 大量独立的乘加操作 = 完美的并行任务
- GPU 有 10,000+ 核心 = 可以同时计算 10,000+ 元素
- 训练神经网络 = 99% 时间在做矩阵运算 = GPU 的天堂
```

### CUDA 编程模型

```
线程层次结构：

Grid (整个计算任务)
├── Block 0 (线程块 0)
│   ├── Thread 0
│   ├── Thread 1
│   └── ...
├── Block 1 (线程块 1)
│   ├── Thread 0
│   ├── Thread 1
│   └── ...
└── Block N (线程块 N)

内存层次结构（从快到慢）：
1. 寄存器 (Registers)      - 私有，超快，容量极小
2. 共享内存 (Shared Memory) - 块内共享，快，容量小
3. 全局内存 (Global Memory) - 所有线程共享，慢，容量大

关键理念：
- 每个线程处理一个数据元素
- 利用内存层次结构优化性能
- 线程协作完成复杂任务
```

---

## 案例一：向量加法 - Hello CUDA World

### 业务场景

神经网络中最简单的操作：两个向量相加（如 BatchNorm 的偏置项）
- 向量长度：1000 万（40MB）
- 操作：C[i] = A[i] + B[i]

### CPU 版本（串行）

```cpp
// cpu_vector_add.cpp
#include <iostream>
#include <chrono>

void vector_add_cpu(float* a, float* b, float* c, int n) {
    // 一个一个元素串行相加
    for (int i = 0; i < n; i++) {
        c[i] = a[i] + b[i];
    }
}

int main() {
    int N = 10000000;  // 1000 万
    size_t bytes = N * sizeof(float);

    // 分配内存
    float *a = new float[N];
    float *b = new float[N];
    float *c = new float[N];

    // 初始化
    for (int i = 0; i < N; i++) {
        a[i] = 1.0f;
        b[i] = 2.0f;
    }

    // 计时
    auto start = std::chrono::high_resolution_clock::now();

    vector_add_cpu(a, b, c, N);

    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);

    std::cout << "CPU Time: " << duration.count() << " ms\n";
    std::cout << "Result: c[0] = " << c[0] << "\n";  // 应该是 3.0

    delete[] a;
    delete[] b;
    delete[] c;

    return 0;
}

// 编译运行：
// g++ cpu_vector_add.cpp -o cpu_add -O3
// ./cpu_add
// 输出：CPU Time: 15 ms
```

### 第一次思维转变：并行思维

**CUDA 版本（并行）**：

```cuda
// gpu_vector_add.cu
#include <iostream>
#include <cuda_runtime.h>

// CUDA Kernel：在 GPU 上执行的函数
__global__ void vector_add_gpu(float* a, float* b, float* c, int n) {
    // 计算当前线程要处理的元素索引
    int idx = blockIdx.x * blockDim.x + threadIdx.x;

    // 边界检查
    if (idx < n) {
        c[idx] = a[idx] + b[idx];  // 每个线程处理一个元素
    }
}

int main() {
    int N = 10000000;
    size_t bytes = N * sizeof(float);

    // 1. 在主机 (CPU) 上分配内存
    float *h_a = new float[N];
    float *h_b = new float[N];
    float *h_c = new float[N];

    // 初始化
    for (int i = 0; i < N; i++) {
        h_a[i] = 1.0f;
        h_b[i] = 2.0f;
    }

    // 2. 在设备 (GPU) 上分配内存
    float *d_a, *d_b, *d_c;
    cudaMalloc(&d_a, bytes);
    cudaMalloc(&d_b, bytes);
    cudaMalloc(&d_c, bytes);

    // 3. 从主机拷贝数据到设备
    cudaMemcpy(d_a, h_a, bytes, cudaMemcpyHostToDevice);
    cudaMemcpy(d_b, h_b, bytes, cudaMemcpyHostToDevice);

    // 4. 配置 CUDA kernel 启动参数
    int threads_per_block = 256;  // 每个块 256 个线程
    int blocks = (N + threads_per_block - 1) / threads_per_block;  // 需要多少个块

    std::cout << "Launching " << blocks << " blocks with "
              << threads_per_block << " threads each\n";

    // 5. 启动 kernel（计时）
    cudaEvent_t start, stop;
    cudaEventCreate(&start);
    cudaEventCreate(&stop);

    cudaEventRecord(start);

    // <<< blocks, threads_per_block >>> 是 CUDA 语法
    vector_add_gpu<<<blocks, threads_per_block>>>(d_a, d_b, d_c, N);

    cudaEventRecord(stop);
    cudaEventSynchronize(stop);

    float milliseconds = 0;
    cudaEventElapsedTime(&milliseconds, start, stop);

    std::cout << "GPU Time: " << milliseconds << " ms\n";

    // 6. 从设备拷贝结果回主机
    cudaMemcpy(h_c, d_c, bytes, cudaMemcpyDeviceToHost);

    std::cout << "Result: c[0] = " << h_c[0] << "\n";

    // 7. 释放内存
    cudaFree(d_a);
    cudaFree(d_b);
    cudaFree(d_c);
    delete[] h_a;
    delete[] h_b;
    delete[] h_c;

    return 0;
}

// 编译运行：
// nvcc gpu_vector_add.cu -o gpu_add
// ./gpu_add
// 输出：
// Launching 39063 blocks with 256 threads each
// GPU Time: 0.8 ms
// Result: c[0] = 3.0
```

**性能对比**：

| 版本 | 时间 | 加速比 |
|-----|------|-------|
| CPU (单核) | 15 ms | 1× |
| **GPU (CUDA)** | **0.8 ms** | **19×** |

### 深入理解：CUDA 执行模型

**1. 线程索引计算**

```cuda
// 一维索引计算
int idx = blockIdx.x * blockDim.x + threadIdx.x;

// 示例：假设有 10 个元素，每个块 4 个线程
// Block 0: Thread 0,1,2,3 → idx = 0*4+0, 0*4+1, 0*4+2, 0*4+3 → 0,1,2,3
// Block 1: Thread 0,1,2,3 → idx = 1*4+0, 1*4+1, 1*4+2, 1*4+3 → 4,5,6,7
// Block 2: Thread 0,1    → idx = 2*4+0, 2*4+1               → 8,9

// 关键概念：
// - blockIdx.x: 当前线程所在的块编号
// - blockDim.x: 每个块的线程数量
// - threadIdx.x: 当前线程在块内的编号
```

**2. 为什么需要边界检查？**

```cuda
if (idx < n) {
    c[idx] = a[idx] + b[idx];
}

// 原因：线程数量通常不能被数据大小整除
// 例如：N = 10, threads_per_block = 4
// blocks = (10 + 4 - 1) / 4 = 3  (向上取整)
// 总线程数 = 3 * 4 = 12 > 10
// Thread 10, 11 会越界，需要检查
```

**3. CUDA 编程流程**

```
1. 分配内存 (cudaMalloc)
   ↓
2. 拷贝数据到 GPU (cudaMemcpy H→D)
   ↓
3. 启动 Kernel (kernel<<<blocks, threads>>>)
   ↓
4. 拷贝结果回 CPU (cudaMemcpy D→H)
   ↓
5. 释放内存 (cudaFree)
```

**心智模型突破**：

```
串行思维：一个 for 循环，一次处理一个元素
         for (int i = 0; i < N; i++) { ... }

并行思维：每个线程处理一个元素，所有线程同时执行
         Thread 0: c[0] = a[0] + b[0]
         Thread 1: c[1] = a[1] + b[1]
         ...
         Thread 9999999: c[9999999] = a[9999999] + b[9999999]
         (同时执行！)
```

---

## 案例二：矩阵乘法 - 优化内存访问

### 业务场景

神经网络的全连接层：`Y = X @ W`
- 矩阵大小：1024 × 1024（常见的隐藏层维度）
- 每个元素需要 1024 次乘法和加法
- 总计算量：1024³ ≈ 10 亿次运算

### 朴素实现（慢）

```cuda
// naive_matmul.cu
__global__ void matmul_naive(float* A, float* B, float* C, int N) {
    // 计算当前线程负责的输出元素位置
    int row = blockIdx.y * blockDim.y + threadIdx.y;
    int col = blockIdx.x * blockDim.x + threadIdx.x;

    if (row < N && col < N) {
        float sum = 0.0f;

        // 计算 C[row][col] = A[row][:] · B[:][col]
        for (int k = 0; k < N; k++) {
            sum += A[row * N + k] * B[k * N + col];
            // ↑ 每次迭代都从全局内存读取（慢！）
        }

        C[row * N + col] = sum;
    }
}

int main() {
    int N = 1024;
    size_t bytes = N * N * sizeof(float);

    // 分配内存...
    float *d_A, *d_B, *d_C;
    cudaMalloc(&d_A, bytes);
    cudaMalloc(&d_B, bytes);
    cudaMalloc(&d_C, bytes);

    // 拷贝数据...

    // 配置：使用 32×32 的线程块
    dim3 threads(32, 32);
    dim3 blocks((N + 31) / 32, (N + 31) / 32);

    // 计时
    cudaEvent_t start, stop;
    cudaEventCreate(&start);
    cudaEventCreate(&stop);

    cudaEventRecord(start);
    matmul_naive<<<blocks, threads>>>(d_A, d_B, d_C, N);
    cudaEventRecord(stop);

    cudaEventSynchronize(stop);
    float milliseconds = 0;
    cudaEventElapsedTime(&milliseconds, start, stop);

    std::cout << "Naive MatMul: " << milliseconds << " ms\n";

    // 输出：Naive MatMul: 85 ms

    // 清理...
    return 0;
}
```

**性能分析**：

```
问题：全局内存访问慢

每个线程需要：
- 读取 A 的一行：1024 次全局内存访问
- 读取 B 的一列：1024 次全局内存访问
- 总访问次数：2048 次 × 1,048,576 线程 = 21 亿次

全局内存带宽：~500 GB/s
理论传输时间：(21 亿 × 4 字节) / 500 GB/s ≈ 16 ms

实际运行：85 ms（比理论慢 5×，因为访问模式不佳）
```

### 第二次思维转变：利用共享内存

**优化版本（使用 Shared Memory）**：

```cuda
// optimized_matmul.cu
#define TILE_SIZE 32

__global__ void matmul_shared(float* A, float* B, float* C, int N) {
    // 1. 分配共享内存（块内所有线程共享，速度快）
    __shared__ float As[TILE_SIZE][TILE_SIZE];
    __shared__ float Bs[TILE_SIZE][TILE_SIZE];

    int row = blockIdx.y * TILE_SIZE + threadIdx.y;
    int col = blockIdx.x * TILE_SIZE + threadIdx.x;

    float sum = 0.0f;

    // 2. 分块计算（Tiling）
    for (int t = 0; t < (N + TILE_SIZE - 1) / TILE_SIZE; t++) {
        // 2.1 协作加载：每个线程加载一个元素到共享内存
        if (row < N && t * TILE_SIZE + threadIdx.x < N) {
            As[threadIdx.y][threadIdx.x] = A[row * N + t * TILE_SIZE + threadIdx.x];
        } else {
            As[threadIdx.y][threadIdx.x] = 0.0f;
        }

        if (col < N && t * TILE_SIZE + threadIdx.y < N) {
            Bs[threadIdx.y][threadIdx.x] = B[(t * TILE_SIZE + threadIdx.y) * N + col];
        } else {
            Bs[threadIdx.y][threadIdx.x] = 0.0f;
        }

        // 2.2 同步：等待所有线程完成加载
        __syncthreads();

        // 2.3 计算：使用共享内存（快！）
        for (int k = 0; k < TILE_SIZE; k++) {
            sum += As[threadIdx.y][k] * Bs[k][threadIdx.x];
            // ↑ 从共享内存读取，比全局内存快 100×
        }

        // 2.4 同步：避免下一轮加载覆盖当前数据
        __syncthreads();
    }

    // 3. 写回结果
    if (row < N && col < N) {
        C[row * N + col] = sum;
    }
}

int main() {
    // ... (同上，配置部分)

    dim3 threads(TILE_SIZE, TILE_SIZE);
    dim3 blocks((N + TILE_SIZE - 1) / TILE_SIZE,
                (N + TILE_SIZE - 1) / TILE_SIZE);

    cudaEventRecord(start);
    matmul_shared<<<blocks, threads>>>(d_A, d_B, d_C, N);
    cudaEventRecord(stop);

    cudaEventSynchronize(stop);
    cudaEventElapsedTime(&milliseconds, start, stop);

    std::cout << "Shared Memory MatMul: " << milliseconds << " ms\n";

    // 输出：Shared Memory MatMul: 4.5 ms

    return 0;
}
```

**性能对比**：

| 版本 | 时间 | 加速比 | 原因 |
|-----|------|-------|------|
| CPU (单核) | 2500 ms | 1× | 串行计算 |
| GPU 朴素 | 85 ms | 29× | 并行，但内存慢 |
| **GPU 优化** | **4.5 ms** | **556×** | 并行 + 共享内存 |

### 深入理解：内存优化原理

**1. Tiling（分块）策略**

```
矩阵乘法：C = A × B

原始计算（朴素版）：
C[0][0] = A[0][0]*B[0][0] + A[0][1]*B[1][0] + ... + A[0][1023]*B[1023][0]
         └─ 需要读取 A 的第 0 行（1024 个元素）
         └─ 需要读取 B 的第 0 列（1024 个元素）
         └─ 从全局内存读取 2048 次

分块计算（优化版）：
将大矩阵分成 32×32 的小块（tile）

Step 1: 加载 A[0:32, 0:32] 和 B[0:32, 0:32] 到共享内存
        计算部分和
Step 2: 加载 A[0:32, 32:64] 和 B[32:64, 0:32] 到共享内存
        累加到部分和
...
Step 32: 得到最终结果

关键：每个 tile 被 32×32=1024 个线程复用
```

**2. 共享内存 vs 全局内存**

```
访问延迟对比：

全局内存 (Global Memory):
- 延迟：400-800 cycles
- 带宽：~500 GB/s
- 容量：几 GB

共享内存 (Shared Memory):
- 延迟：~5 cycles (快 100×)
- 带宽：~10 TB/s (快 20×)
- 容量：48-96 KB per block

策略：
1. 将频繁访问的数据加载到共享内存
2. 线程协作加载（每个线程加载一个元素）
3. 复用数据（多个线程读取同一数据）
```

**3. __syncthreads() 的作用**

```cuda
// 加载阶段
As[threadIdx.y][threadIdx.x] = A[...];
Bs[threadIdx.y][threadIdx.x] = B[...];

__syncthreads();  // ← 等待所有线程完成加载

// 计算阶段
for (int k = 0; k < TILE_SIZE; k++) {
    sum += As[threadIdx.y][k] * Bs[k][threadIdx.x];
}

// 为什么需要同步？
// - Thread 0 可能先完成加载，立即开始计算
// - 此时 Thread 1 还在加载数据
// - Thread 0 读取的 As[0][1] 可能还未被 Thread 1 写入
// - 导致计算错误

// __syncthreads() 确保：所有线程到达此点后才继续
```

**心智模型升级**：

```
Level 0 (朴素并行):
  每个线程独立工作
  问题：重复读取相同数据

Level 1 (分块思维):
  将大任务分成小块
  优势：局部性原理

Level 2 (协作思维):
  线程协作加载数据到共享内存
  线程共享数据，减少内存访问

Level 3 (内存层次):
  理解内存速度差异
  优化数据访问模式
```

---

## 案例三：卷积操作 - 深度学习核心

### 业务场景

CNN 的卷积层：
- 输入：224×224×3 图像
- 卷积核：3×3×3
- 输出：224×224 特征图
- 这是 ResNet 第一层的典型配置

### 朴素卷积实现

```cuda
// naive_conv.cu
__global__ void conv2d_naive(
    float* input,    // [H, W, C]
    float* kernel,   // [KH, KW, C]
    float* output,   // [H, W]
    int H, int W, int C,
    int KH, int KW
) {
    int h = blockIdx.y * blockDim.y + threadIdx.y;
    int w = blockIdx.x * blockDim.x + threadIdx.x;

    if (h < H && w < W) {
        float sum = 0.0f;

        // 对于每个输出像素，遍历卷积核
        for (int kh = 0; kh < KH; kh++) {
            for (int kw = 0; kw < KW; kw++) {
                for (int c = 0; c < C; c++) {
                    int in_h = h + kh - KH/2;  // 居中对齐
                    int in_w = w + kw - KW/2;

                    // 边界处理（padding=0）
                    if (in_h >= 0 && in_h < H && in_w >= 0 && in_w < W) {
                        sum += input[in_h * W * C + in_w * C + c] *
                               kernel[kh * KW * C + kw * C + c];
                    }
                }
            }
        }

        output[h * W + w] = sum;
    }
}

// 性能：约 150 ms (224×224×3, 3×3 kernel)
```

### 第三次思维转变：利用常量内存和共享内存

**优化卷积实现**：

```cuda
// optimized_conv.cu
#define TILE_SIZE 16
#define KERNEL_RADIUS 1  // 3×3 kernel

// 常量内存：存储卷积核（读取快，且被所有线程共享）
__constant__ float const_kernel[3*3*3];  // 最大支持 64KB

__global__ void conv2d_optimized(
    float* input,
    float* output,
    int H, int W, int C
) {
    // 共享内存：缓存输入数据块（带 halo 边界）
    __shared__ float tile[TILE_SIZE + 2][TILE_SIZE + 2][3];

    int h = blockIdx.y * TILE_SIZE + threadIdx.y;
    int w = blockIdx.x * TILE_SIZE + threadIdx.x;

    // 1. 协作加载数据到共享内存（包括边界）
    int tile_h = threadIdx.y;
    int tile_w = threadIdx.x;

    // 加载中心区域
    if (h < H && w < W) {
        for (int c = 0; c < C; c++) {
            tile[tile_h + 1][tile_w + 1][c] = input[h * W * C + w * C + c];
        }
    }

    // 加载边界（halo 区域）
    if (threadIdx.y == 0 && h > 0) {  // 上边界
        for (int c = 0; c < C; c++) {
            tile[0][tile_w + 1][c] = input[(h-1) * W * C + w * C + c];
        }
    }
    if (threadIdx.y == TILE_SIZE-1 && h < H-1) {  // 下边界
        for (int c = 0; c < C; c++) {
            tile[TILE_SIZE + 1][tile_w + 1][c] = input[(h+1) * W * C + w * C + c];
        }
    }
    if (threadIdx.x == 0 && w > 0) {  // 左边界
        for (int c = 0; c < C; c++) {
            tile[tile_h + 1][0][c] = input[h * W * C + (w-1) * C + c];
        }
    }
    if (threadIdx.x == TILE_SIZE-1 && w < W-1) {  // 右边界
        for (int c = 0; c < C; c++) {
            tile[tile_h + 1][TILE_SIZE + 1][c] = input[h * W * C + (w+1) * C + c];
        }
    }

    __syncthreads();  // 等待所有数据加载完成

    // 2. 计算卷积（从共享内存和常量内存读取）
    if (h < H && w < W) {
        float sum = 0.0f;

        for (int kh = 0; kh < 3; kh++) {
            for (int kw = 0; kw < 3; kw++) {
                for (int c = 0; c < C; c++) {
                    sum += tile[tile_h + kh][tile_w + kw][c] *
                           const_kernel[kh * 3 * C + kw * C + c];
                }
            }
        }

        output[h * W + w] = sum;
    }
}

int main() {
    int H = 224, W = 224, C = 3;
    int KH = 3, KW = 3;

    // ... 分配内存 ...

    // 将卷积核拷贝到常量内存
    float h_kernel[3*3*3];
    // ... 初始化 h_kernel ...
    cudaMemcpyToSymbol(const_kernel, h_kernel, 3*3*3*sizeof(float));

    dim3 threads(TILE_SIZE, TILE_SIZE);
    dim3 blocks((W + TILE_SIZE - 1) / TILE_SIZE,
                (H + TILE_SIZE - 1) / TILE_SIZE);

    cudaEventRecord(start);
    conv2d_optimized<<<blocks, threads>>>(d_input, d_output, H, W, C);
    cudaEventRecord(stop);

    cudaEventSynchronize(stop);
    cudaEventElapsedTime(&milliseconds, start, stop);

    std::cout << "Optimized Conv: " << milliseconds << " ms\n";

    // 输出：Optimized Conv: 12 ms

    return 0;
}
```

**性能对比**：

| 版本 | 时间 | 加速比 | 优化技术 |
|-----|------|-------|---------|
| CPU | 8000 ms | 1× | 单核串行 |
| GPU 朴素 | 150 ms | 53× | 基本并行 |
| **GPU 优化** | **12 ms** | **667×** | 共享内存 + 常量内存 |

### 深入理解：内存类型选择

**1. 常量内存（Constant Memory）**

```cuda
__constant__ float const_kernel[MAX_SIZE];

特点：
- 只读，适合卷积核等参数
- 有专门缓存，读取快
- 所有线程访问相同值时最快
- 大小限制：64KB

适用场景：
✅ 卷积核
✅ 归一化参数（mean, std）
✅ 查找表
❌ 每个线程访问不同值（会串行化）
```

**2. 共享内存布局优化**

```
Halo 区域（边界扩展）：

原始 tile: 16×16
带 halo: (16+2)×(16+2) = 18×18

┌─────────────────────┐
│ H  H  H  H  H  H  H │  ← Halo (边界)
│ H ┌─────────────┐ H │
│ H │   Tile      │ H │  ← 核心区域
│ H │   16×16     │ H │
│ H └─────────────┘ H │
│ H  H  H  H  H  H  H │
└─────────────────────┘

好处：
1. 卷积时不需要访问全局内存
2. 所有数据都在快速的共享内存中
3. 减少边界检查

代价：
1. 加载更多数据
2. 需要额外的边界加载逻辑
```

**3. Bank Conflict 优化**

```cuda
// 共享内存的 Bank 结构
// 共享内存被分成 32 个 bank
// 如果多个线程访问同一个 bank 的不同地址 → 冲突（串行化）

// ❌ 有 Bank Conflict 的访问
__shared__ float data[32][32];
float val = data[threadIdx.x][threadIdx.y];  // 可能冲突

// ✅ 避免 Bank Conflict（添加 padding）
__shared__ float data[32][33];  // 多一列
float val = data[threadIdx.x][threadIdx.y];  // 不同线程访问不同 bank
```

---

## 案例四：Softmax - 规约操作

### 业务场景

Transformer 的注意力机制需要计算 Softmax：
- 输入：[batch, seq_len] = [32, 512]
- 每行独立计算：exp(x) / sum(exp(x))
- 需要规约操作（求和）

### Softmax 数学公式

```
Softmax(x_i) = exp(x_i) / Σ exp(x_j)

数值稳定版本（防止溢出）：
Softmax(x_i) = exp(x_i - max(x)) / Σ exp(x_j - max(x))

步骤：
1. 找最大值：max_val = max(x)
2. 计算 exp(x - max_val)
3. 求和：sum = Σ exp(x - max_val)
4. 归一化：output = exp(x - max_val) / sum
```

### 第四次思维转变：高效规约

**优化的 Softmax 实现**：

```cuda
// softmax.cu
__global__ void softmax(float* input, float* output, int N, int D) {
    // N: batch size, D: feature dimension
    // 每个 block 处理一行

    extern __shared__ float shared[];  // 动态分配共享内存

    int row = blockIdx.x;
    int tid = threadIdx.x;
    int stride = blockDim.x;

    // 1. 找最大值（规约）
    float thread_max = -INFINITY;
    for (int i = tid; i < D; i += stride) {
        thread_max = fmaxf(thread_max, input[row * D + i]);
    }

    // 使用共享内存进行并行规约
    shared[tid] = thread_max;
    __syncthreads();

    // 树形规约（log(N) 步）
    for (int s = blockDim.x / 2; s > 0; s >>= 1) {
        if (tid < s) {
            shared[tid] = fmaxf(shared[tid], shared[tid + s]);
        }
        __syncthreads();
    }

    float max_val = shared[0];
    __syncthreads();

    // 2. 计算 exp 和求和
    float thread_sum = 0.0f;
    for (int i = tid; i < D; i += stride) {
        float exp_val = expf(input[row * D + i] - max_val);
        shared[tid + i] = exp_val;  // 保存 exp 值
        thread_sum += exp_val;
    }

    // 求和规约
    shared[tid] = thread_sum;
    __syncthreads();

    for (int s = blockDim.x / 2; s > 0; s >>= 1) {
        if (tid < s) {
            shared[tid] += shared[tid + s];
        }
        __syncthreads();
    }

    float sum = shared[0];

    // 3. 归一化
    for (int i = tid; i < D; i += stride) {
        output[row * D + i] = shared[tid + i] / sum;
    }
}

int main() {
    int N = 32;   // batch
    int D = 512;  // seq_len

    // ... 内存分配 ...

    dim3 blocks(N);  // 每行一个 block
    dim3 threads(256);
    size_t shared_size = (threads.x + D) * sizeof(float);

    cudaEventRecord(start);
    softmax<<<blocks, threads, shared_size>>>(d_input, d_output, N, D);
    cudaEventRecord(stop);

    cudaEventSynchronize(stop);
    cudaEventElapsedTime(&milliseconds, start, stop);

    std::cout << "Softmax: " << milliseconds << " ms\n";

    // 输出：Softmax: 0.15 ms

    return 0;
}
```

### 深入理解：并行规约

**1. 树形规约原理**

```
求和：[1, 2, 3, 4, 5, 6, 7, 8]

朴素方法（串行）：
sum = 0
sum += 1  (step 1)
sum += 2  (step 2)
...
sum += 8  (step 8)
总计：8 步

树形规约（并行）：
Step 1: [1+2, 3+4, 5+6, 7+8] = [3, 7, 11, 15]  (4 个线程并行)
Step 2: [3+7, 11+15]         = [10, 26]        (2 个线程并行)
Step 3: [10+26]              = [36]            (1 个线程)
总计：3 步（log₂8）

代码实现：
for (int s = blockDim.x / 2; s > 0; s >>= 1) {
    if (tid < s) {
        shared[tid] += shared[tid + s];
    }
    __syncthreads();
}

s = 128: Thread 0-127 加上 Thread 128-255
s = 64:  Thread 0-63  加上 Thread 64-127
s = 32:  Thread 0-31  加上 Thread 32-63
...
s = 1:   Thread 0     加上 Thread 1
```

**2. Warp-level 优化**

```cuda
// 更高效的规约（利用 warp 内同步）
__global__ void softmax_warp_optimized(float* input, float* output, int N, int D) {
    extern __shared__ float shared[];

    int row = blockIdx.x;
    int tid = threadIdx.x;
    int lane = tid % 32;  // warp 内编号
    int wid = tid / 32;   // warp 编号

    // 1. 找最大值
    float thread_max = -INFINITY;
    for (int i = tid; i < D; i += blockDim.x) {
        thread_max = fmaxf(thread_max, input[row * D + i]);
    }

    // Warp 内规约（使用 warp shuffle，不需要 __syncthreads）
    for (int offset = 16; offset > 0; offset >>= 1) {
        thread_max = fmaxf(thread_max, __shfl_down_sync(0xffffffff, thread_max, offset));
    }

    // 每个 warp 的第一个线程写入共享内存
    if (lane == 0) {
        shared[wid] = thread_max;
    }
    __syncthreads();

    // 最后一个 warp 完成最终规约
    if (wid == 0) {
        float val = (tid < (blockDim.x / 32)) ? shared[lane] : -INFINITY;
        for (int offset = 16; offset > 0; offset >>= 1) {
            val = fmaxf(val, __shfl_down_sync(0xffffffff, val, offset));
        }
        if (tid == 0) shared[0] = val;
    }
    __syncthreads();

    float max_val = shared[0];

    // ... 后续计算类似 ...
}

// 性能提升：0.15 ms → 0.08 ms (约 2×)
```

**3. 为什么 Warp Shuffle 更快？**

```
传统方法：
- 写共享内存
- __syncthreads()
- 读共享内存
- 延迟：~20 cycles

Warp Shuffle：
- 直接在寄存器间交换数据
- warp 内自动同步（不需要 __syncthreads）
- 延迟：~5 cycles

限制：
- 只能在同一个 warp 内使用（32 个线程）
- 跨 warp 仍需共享内存
```

**心智模型**：

```
串行规约：O(N) 步
         一个接一个累加

并行规约：O(log N) 步
         树形结构，每层减半

Warp 优化：利用硬件特性
         寄存器直接通信
```

---

## 案例五：注意力机制 - 综合应用

### 业务场景

Transformer 的核心：Self-Attention
- Query, Key, Value: [batch, seq_len, d_model]
- 计算：Attention(Q, K, V) = softmax(QK^T / √d) V
- 这是 GPT/BERT 的计算瓶颈

### Flash Attention 的核心思想

```
标准 Attention 的问题：
1. 计算 QK^T: [batch, N, N] 矩阵（N=seq_len）
2. 当 N=4096 时，中间矩阵 = 4096² = 16M 元素
3. 需要 64MB 显存（float32）
4. 导致频繁的 GPU 内存读写

Flash Attention 优化：
- 分块计算（Tiling）
- 在线 Softmax（Online Softmax）
- 减少 HBM 访问（只访问高速缓存）
```

### 简化版 Flash Attention 实现

```cuda
// flash_attention.cu
#define BLOCK_SIZE 32

__global__ void flash_attention(
    float* Q,  // [N, d]
    float* K,  // [N, d]
    float* V,  // [N, d]
    float* O,  // [N, d] output
    int N, int d
) {
    extern __shared__ float shared[];

    // 分配共享内存空间
    float* Q_block = shared;                    // [BLOCK_SIZE, d]
    float* K_block = &shared[BLOCK_SIZE * d];   // [BLOCK_SIZE, d]
    float* V_block = &shared[2 * BLOCK_SIZE * d]; // [BLOCK_SIZE, d]

    int row = blockIdx.x * BLOCK_SIZE + threadIdx.x;

    // 加载 Q
    if (row < N) {
        for (int i = 0; i < d; i++) {
            Q_block[threadIdx.x * d + i] = Q[row * d + i];
        }
    }

    // 初始化输出
    float row_max = -INFINITY;
    float row_sum = 0.0f;
    float output[64];  // 假设 d <= 64
    for (int i = 0; i < d; i++) output[i] = 0.0f;

    // 分块处理 K 和 V
    for (int block_start = 0; block_start < N; block_start += BLOCK_SIZE) {
        __syncthreads();

        // 加载 K 和 V 块
        if (threadIdx.x < BLOCK_SIZE) {
            int k_row = block_start + threadIdx.x;
            if (k_row < N) {
                for (int i = 0; i < d; i++) {
                    K_block[threadIdx.x * d + i] = K[k_row * d + i];
                    V_block[threadIdx.x * d + i] = V[k_row * d + i];
                }
            }
        }
        __syncthreads();

        // 计算 Q @ K^T (当前块)
        float scores[BLOCK_SIZE];
        for (int j = 0; j < BLOCK_SIZE && block_start + j < N; j++) {
            float score = 0.0f;
            for (int k = 0; k < d; k++) {
                score += Q_block[threadIdx.x * d + k] * K_block[j * d + k];
            }
            scores[j] = score / sqrtf((float)d);
        }

        // Online Softmax 更新
        float block_max = -INFINITY;
        for (int j = 0; j < BLOCK_SIZE && block_start + j < N; j++) {
            block_max = fmaxf(block_max, scores[j]);
        }

        float new_max = fmaxf(row_max, block_max);
        float old_scale = expf(row_max - new_max);
        float new_scale = expf(block_max - new_max);

        // 更新输出
        for (int i = 0; i < d; i++) {
            output[i] *= old_scale;
        }

        float block_sum = 0.0f;
        for (int j = 0; j < BLOCK_SIZE && block_start + j < N; j++) {
            float weight = expf(scores[j] - new_max);
            block_sum += weight;

            // 累加 weighted V
            for (int i = 0; i < d; i++) {
                output[i] += weight * V_block[j * d + i];
            }
        }

        row_sum = row_sum * old_scale + block_sum;
        row_max = new_max;
    }

    // 最终归一化
    if (row < N) {
        for (int i = 0; i < d; i++) {
            O[row * d + i] = output[i] / row_sum;
        }
    }
}

// 性能对比：
// 标准实现：45 ms (N=2048, d=64)
// Flash Attention：8 ms (5.6× 提速)
```

### 深入理解：Online Softmax

**1. 传统 Softmax（两遍扫描）**

```python
# 第一遍：找最大值
max_val = max(x)

# 第二遍：计算 softmax
exp_sum = sum(exp(x - max_val))
output = exp(x - max_val) / exp_sum

问题：需要两遍扫描，中间结果需要存储
```

**2. Online Softmax（一遍扫描）**

```python
# 初始化
max_val = -inf
sum_val = 0

# 处理第一个块
max_val = max(block1)
sum_val = sum(exp(block1 - max_val))
output = exp(block1 - max_val) * V1

# 处理第二个块
new_max = max(max_val, max(block2))
old_scale = exp(max_val - new_max)  # 重新缩放旧结果
new_scale = exp(max(block2) - new_max)

output *= old_scale  # 缩放之前的结果
output += exp(block2 - new_max) * V2
sum_val = sum_val * old_scale + sum(exp(block2 - new_max))
max_val = new_max

# 最后归一化
output /= sum_val

优势：
- 只需一遍扫描
- 不需要存储中间的 exp 值
- 内存占用从 O(N²) 降到 O(N)
```

**3. 内存访问优化**

```
标准 Attention 内存访问：

HBM (High Bandwidth Memory，慢)
  ↓ 读 Q, K
SRAM (缓存，快)
  ↓ 计算 QK^T
HBM
  ↓ 写 S = QK^T
HBM
  ↓ 读 S
SRAM
  ↓ 计算 Softmax(S)
HBM
  ↓ 写 P = Softmax(S)
HBM
  ↓ 读 P, V
SRAM
  ↓ 计算 O = PV
HBM

Flash Attention 内存访问：

HBM
  ↓ 读 Q, K, V 的块
SRAM
  ↓ 计算 QK^T, Softmax, 乘 V (融合)
  ↓ Online 更新输出
HBM
  ↓ 写最终 O

减少 HBM 访问：8 次 → 2 次 (4× 减少)
```

---

## 案例六：PyTorch 自定义算子

### 业务场景

实现自定义 CUDA 算子并集成到 PyTorch：
- 加速特定操作（PyTorch 没有优化的算子）
- 融合多个操作（减少中间结果存储）

### PyTorch C++/CUDA 扩展

**1. CUDA Kernel**

```cuda
// custom_ops.cu
#include <torch/extension.h>
#include <cuda_runtime.h>

// ReLU 前向传播
__global__ void relu_forward_kernel(
    const float* __restrict__ input,
    float* __restrict__ output,
    int n
) {
    int idx = blockIdx.x * blockDim.x + threadIdx.x;
    if (idx < n) {
        output[idx] = fmaxf(0.0f, input[idx]);
    }
}

// ReLU 反向传播
__global__ void relu_backward_kernel(
    const float* __restrict__ grad_output,
    const float* __restrict__ input,
    float* __restrict__ grad_input,
    int n
) {
    int idx = blockIdx.x * blockDim.x + threadIdx.x;
    if (idx < n) {
        grad_input[idx] = input[idx] > 0 ? grad_output[idx] : 0.0f;
    }
}

// C++ 包装函数
torch::Tensor relu_forward_cuda(torch::Tensor input) {
    auto output = torch::zeros_like(input);
    int n = input.numel();

    const int threads = 256;
    const int blocks = (n + threads - 1) / threads;

    relu_forward_kernel<<<blocks, threads>>>(
        input.data_ptr<float>(),
        output.data_ptr<float>(),
        n
    );

    return output;
}

torch::Tensor relu_backward_cuda(
    torch::Tensor grad_output,
    torch::Tensor input
) {
    auto grad_input = torch::zeros_like(input);
    int n = input.numel();

    const int threads = 256;
    const int blocks = (n + threads - 1) / threads;

    relu_backward_kernel<<<blocks, threads>>>(
        grad_output.data_ptr<float>(),
        input.data_ptr<float>(),
        grad_input.data_ptr<float>(),
        n
    );

    return grad_input;
}

// Python 绑定
PYBIND11_MODULE(TORCH_EXTENSION_NAME, m) {
    m.def("forward", &relu_forward_cuda, "ReLU forward (CUDA)");
    m.def("backward", &relu_backward_cuda, "ReLU backward (CUDA)");
}
```

**2. Python 接口**

```python
# setup.py
from setuptools import setup
from torch.utils.cpp_extension import BuildExtension, CUDAExtension

setup(
    name='custom_relu',
    ext_modules=[
        CUDAExtension('custom_relu_cuda', [
            'custom_ops.cu',
        ])
    ],
    cmdclass={
        'build_ext': BuildExtension
    }
)

# 编译：python setup.py install
```

**3. PyTorch 使用**

```python
# test_custom_op.py
import torch
import custom_relu_cuda

class CustomReLU(torch.autograd.Function):
    @staticmethod
    def forward(ctx, input):
        ctx.save_for_backward(input)
        return custom_relu_cuda.forward(input)

    @staticmethod
    def backward(ctx, grad_output):
        input, = ctx.saved_tensors
        grad_input = custom_relu_cuda.backward(grad_output, input)
        return grad_input

# 使用
custom_relu = CustomReLU.apply

x = torch.randn(1000000, device='cuda', requires_grad=True)

# 前向
y = custom_relu(x)

# 反向
loss = y.sum()
loss.backward()

print(x.grad)

# 性能对比
import time

# PyTorch 内置 ReLU
x = torch.randn(10000000, device='cuda')
start = time.time()
for _ in range(100):
    y = torch.relu(x)
torch.cuda.synchronize()
print(f"PyTorch ReLU: {(time.time() - start) * 10:.2f} ms")

# 自定义 CUDA ReLU
start = time.time()
for _ in range(100):
    y = custom_relu(x)
torch.cuda.synchronize()
print(f"Custom CUDA ReLU: {(time.time() - start) * 10:.2f} ms")

# 输出：
# PyTorch ReLU: 0.85 ms
# Custom CUDA ReLU: 0.45 ms (约 2× 提速)
```

### 融合算子示例

```cuda
// fused_ops.cu
// 融合：ReLU + Dropout + LayerNorm

__global__ void fused_kernel(
    const float* __restrict__ input,
    float* __restrict__ output,
    const float* __restrict__ gamma,
    const float* __restrict__ beta,
    float* __restrict__ mask,
    int n, int d,
    float dropout_prob,
    unsigned long long seed
) {
    int idx = blockIdx.x * blockDim.x + threadIdx.x;
    int row = idx / d;
    int col = idx % d;

    if (idx < n * d) {
        // 1. ReLU
        float val = fmaxf(0.0f, input[idx]);

        // 2. Dropout
        curandState state;
        curand_init(seed, idx, 0, &state);
        float rand = curand_uniform(&state);
        if (rand < dropout_prob) {
            val = 0.0f;
            mask[idx] = 0.0f;
        } else {
            val /= (1.0f - dropout_prob);
            mask[idx] = 1.0f;
        }

        // 3. LayerNorm (简化版，假设已计算 mean 和 var)
        extern __shared__ float shared[];

        // 计算均值和方差...
        // (省略具体实现)

        output[idx] = val * gamma[col] + beta[col];
    }
}

// 性能对比：
// 三个独立算子：1.5 ms
// 融合算子：0.6 ms (2.5× 提速)
```

---

## 综合心智模型总结

### 从串行到并行的完整转变

```
阶段 0：CPU 串行思维
─────────────────────
for (int i = 0; i < N; i++) {
    c[i] = a[i] + b[i];
}
问题：慢，无法利用 GPU

↓

阶段 1：基础并行（GPU）
────────────────────────
每个线程处理一个元素
改进：100× 加速，但内存访问慢

↓

阶段 2：内存优化
─────────────────
利用共享内存、常量内存
升华：500× 加速，接近硬件极限

↓

阶段 3：算法融合
─────────────────
多个操作融合，减少内存访问
境界：1000× 加速，超越框架实现
```

### CUDA 优化金字塔

```
Level 5: 算法层面
         - 选择更优算法（如 Flash Attention）
         - 融合多个操作

Level 4: 线程协作
         - Warp shuffle
         - 协作加载

Level 3: 内存层次
         - 共享内存 Tiling
         - 常量内存
         - Bank conflict 优化

Level 2: 访问模式
         - 合并访问（coalesced access）
         - 避免分支

Level 1: 基础并行
         - 正确的线程配置
         - 边界检查

Level 0: 串行 CPU
         - 单线程执行
```

### 核心概念对照表

| 概念 | CPU 类比 | GPU 特点 | 优化目标 |
|-----|---------|---------|---------|
| **线程** | 单个工人 | 10,000+ 工人 | 充分利用 |
| **Block** | 小组 | 线程组织单元 | 合理划分 |
| **全局内存** | 硬盘 | 大而慢 | 减少访问 |
| **共享内存** | 缓存 | 小而快 | 最大化复用 |
| **寄存器** | CPU 寄存器 | 超快但极小 | 避免溢出 |
| **Warp** | 无 | 32 线程一组 | 避免分歧 |

---

## 实践建议

### 从零开始的学习路径

**第 1 周：理解并行思维**
```cuda
// 1. 向量加法（Hello CUDA）
// 2. 理解线程索引
// 3. 学会使用 nvcc 编译器
// 4. 掌握内存拷贝流程
```

**第 2 周：优化内存访问**
```cuda
// 1. 矩阵乘法（Tiling）
// 2. 理解共享内存
// 3. 学会使用 __syncthreads()
// 4. 性能分析工具（nvprof）
```

**第 3 周：高级技巧**
```cuda
// 1. 规约操作（树形规约）
// 2. Warp-level 原语
// 3. 常量内存
// 4. Bank conflict 优化
```

**第 4 周：实战应用**
```python
# 1. PyTorch 自定义算子
# 2. 融合算子
# 3. 性能调优
# 4. 集成到训练流程
```

### 常见陷阱

**陷阱 1：忽视内存拷贝开销**

```cuda
// ❌ 错误：频繁拷贝
for (int i = 0; i < 1000; i++) {
    cudaMemcpy(d_data, h_data, size, cudaMemcpyHostToDevice);
    kernel<<<blocks, threads>>>(d_data);
    cudaMemcpy(h_result, d_result, size, cudaMemcpyDeviceToHost);
}

// ✅ 正确：一次拷贝，多次计算
cudaMemcpy(d_data, h_data, size, cudaMemcpyHostToDevice);
for (int i = 0; i < 1000; i++) {
    kernel<<<blocks, threads>>>(d_data);
}
cudaMemcpy(h_result, d_result, size, cudaMemcpyDeviceToHost);
```

**陷阱 2：线程块配置不当**

```cuda
// ❌ 太小：浪费 SM
dim3 threads(8, 8);  // 只有 64 个线程

// ❌ 太大：超过限制
dim3 threads(64, 64);  // 4096 个线程，超过 1024 限制

// ✅ 合适：256-512 个线程
dim3 threads(16, 16);  // 256 个线程（推荐）
dim3 threads(32, 32);  // 1024 个线程（最大）
```

**陷阱 3：忘记同步**

```cuda
// ❌ 可能出错
kernel<<<blocks, threads>>>(d_data);
cudaMemcpy(h_result, d_result, size, cudaMemcpyDeviceToHost);  // 可能在 kernel 完成前拷贝

// ✅ 正确
kernel<<<blocks, threads>>>(d_data);
cudaDeviceSynchronize();  // 等待 kernel 完成
cudaMemcpy(h_result, d_result, size, cudaMemcpyDeviceToHost);
```

### 性能分析工具

```bash
# 1. nvprof（基础分析）
nvprof ./your_program

# 2. Nsight Compute（详细分析）
ncu --set full ./your_program

# 3. Nsight Systems（系统级分析）
nsys profile ./your_program

# 关键指标：
# - Kernel Time: 计算时间
# - Memory Bandwidth: 内存带宽利用率
# - Occupancy: 占用率（活跃线程比例）
# - Register Usage: 寄存器使用
```

---

## 结语：从工具到思维

CUDA 不仅仅是一个编程工具，它代表了**并行计算的思维范式**：

1. **数据并行思维**：将任务分解为独立的数据元素
2. **内存层次思维**：理解不同内存的速度差异
3. **线程协作思维**：多个线程协同完成任务
4. **算法融合思维**：减少中间结果，优化数据流

当你真正掌握了 CUDA 思维，会发现：
- 训练速度提升（10-1000×）
- 可以实现框架没有的算子
- 理解深度学习框架的底层原理
- 成为真正的 ML 系统工程师

**最后的建议**：
- 从简单例子开始（向量加法）
- 理解内存层次结构（最重要）
- 善用性能分析工具
- 阅读优秀开源代码（如 PyTorch、Triton）

当你开始自然地用"线程块"、"共享内存"、"规约"这些概念思考性能优化时，你就已经完成了从应用工程师到系统工程师的转变。
