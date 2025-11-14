竞争壁垒: competitive moat

现代操作系统通过这种机制实现进程地址隔离。每个进程关联到不同的CR3值，因此每个进
程都有⾃⼰特定的虚拟地址转换。

作为⼀般原则，Win32API函数都采⽤STDCALL调⽤惯例。stricmp函数采⽤CDECL调⽤惯例，所以第49⾏进⾏了栈清理

总结CDECL、STDCALL、THISCALL和FASTCALL调⽤惯例

APC（AsynchronousProcedureCall，异步过程调⽤）⽤于实现很多重要的操作，⽐如异步1/0
完成、线程挂起和进程关闭等。

调用 SetWindowsHookEx，将其第 4参数（dwThreadId）设为 0。这样，我们就可以对持有窗口过程的进程和线程应用钩子，也就是让这些进程加载我们的 DLL。

rundll32.exe shell32.dll,ShellExec_RunDLL calc.exe 弹出计算器
运行javascript

动态链接器在查找共享库的过程中，除了到系统默认的路径 (/lib、/usr/lib)下查找，也会到用户指定的一些路径下去查找， 用户可以在/etc/ld.so.conf文件中添加自己的共享库路径。为减少每 次查找文件的时间消耗，/etc/ld.so.conf修改后，我们也可以使用 ldconfig命令生成一个缓存/etc/ld.so.chche以提高查找效率。每当 我们新增、删除或修改共享库的路径时，使用ldconfig更新一下缓存 就可以了。系统中的所有程序在运行时，都会按照上面的这种方式查找共享 库。有时候我们也可以使用LD_LIBRARY_PATH环境变量临时改变共享库 的查找路径，而不会影响系统中的其他应用程序。我们可以将多个共 享库的路径添加到这个环境变量中，各个路径用冒号隔开。

On x86 architecture, TSC is implemented using CPU’s frequency. It means it may not be steady due to frequency changes caused by various reasons like power saving. Luckily, recent x86 CPUs supports constant TSC, which means TSC ticks at the processor’s nominal frequency.
在Linux中，内存实际上是通过页表寻址和映射的。可使用mmap（）在虚拟地址空间而不是物理空间中分配1GB的内存。只有读取或写入才会导致缺页错误，从而导致实际的物理内存分配
c++编程规范第32条提到，搞清楚你写的是哪一种class。明白value class、base class、trait class、policy class、exception class各有其作用，写法也不尽相同。

偏序集合的两个元素x和y可以处于四个相互排斥的关联中任何一个：要么x < y，要么x = y，要么x > y，要么x和y是“不可比较”的（三个都不是）。全序集合是用规则排除第四种可能的集合：所有元素对都是可比较的

Eq 和 PartialEq， Ord 和 PartialOrd 区别其实用离散数学一个性质就可以区分开。

** 类型T：Eq/PartialEq 的实例（instance) 是否具有自反性. **

用你的浮点数例子就可以说明：T = f32, f32::NAN == f32::NAN不成立。 不成立的原因是因为IEEE754只规定了NAN的阶码部分全为1即可，没有规定符号位和尾数部分。

Windows 核心编程,英文版叫Windows via C/C++。图书的作者是编写 Windows Sysinternals 套件的 Jeffrey Richter
王爽老师的《汇编语言(第 3 版)》
开发了一个服务器程序，手头上没有可用的客户端，如何使用 nc 命令模拟一个；或者反过来，开发了一个客户端程序，如果用 nc 模拟一个服务器端用于测试。

主线程创建一个新的工作线程，等待工作线程获取系统时间后写入文件；主线程从文件中读取时间内容显示出来。

这个例子中，我们用到了创建线程的 API、线程等待与通知 API、获取系统时间的 API、显示到控制台的 API

在 Windows 上，我们用到：

CreateThread 
WaitForSingelObject 
GetSystemTime 
CreateEvent/SetEvent 
std::cout

Windows 上常用的网络模型有 select、WSAEventSelect、WSAAsyncSelect、完成端口模型； 
Tanenbaum.A.S《现代操作系统》是一本讲解操作系统理论不错的书，作者 Tanenbaum.A.S 是 Linux 内核创始人 Linus Torvalds 的老师。
[Linux 高性能服务器编程

正如著名的编程大师 Charles Petzold 所说：

“显而易见，究竟用哪种方式编写应用程序最好，其实并无一定之规。应用程序本身的特性应该是决定采用何种编程工具的最主要因素，但是无论将来你采用什么样的编程工具，通过了解操作系统 API 从而深入理解操作系统的工作原理，这本身就有很重要的意义。操作系统是一个非常复杂的系统，在 API 之上加一层编程语言并不能消除其复杂性，最多不过是把复杂性隐藏起来而已。说不定什么时候，复杂的那一面迟早会蹦出来拖你的后腿，懂得系统 API 能让你到时候可以更快地挣脱困境。




在基本操作系统 API 之上的任何软件层或多或少都会限制你使用操作系统的全部功能。比如，你或许发现采用 Visual Basic 来编写你的应用程序非常理想，但是就有那么一两项非常基本的功能 Visual Basic 无法支持。往往这个时候你得非要调用基本 API 。作为直接使用操作系统 API 的程序员，我们的活动空间完全由 API 来规范，再没有什么其他方式比直接调用 API 更有效、更灵活多样了。”

https://cppguide.cn/pages/41538a/ 服务器开发精髓


用户态协议栈是非常有潜力的技术。

在内核TCP/IP协议栈上开发服务器，受限于内核的各种spin lock，吞吐达到600w pps差不多就到头了，网卡再强，cpu再多都无法线性提升吞吐。

用户态协议栈可以做到，在网卡上限内，吞吐随cpu的个数线性增长，不会有cpu浪费在内核spin lock里。scalability是用户态协议栈最吸引人的地方。PolarDB在IDC内部通讯就使用了RDMA之上的用户态协议栈来提高性能。

可预见用户态协议栈结合用户态壅塞算法，是跨IDC通讯的利器。

seastar是用户态TCP/IP协议栈的一种实现，性能上没啥毛病，cpu polling也没啥毛病，但基于future/promise的开发范式个人不太喜欢，对lambda引入的额外代价比较担心。

用户态TCP/IP协议栈的缺陷就是和内核TCP/IP协议栈相比，正确性和完备性缺少保证，多个开源实现版本很大程度上还都是toy。



直到卷积神经网络的出现，它的两个优秀特点：稀疏连接与平移不变性，这让计算机视觉的研究取得了长足的进步。什么是稀疏连接与平移不变性呢？简单来说，就是稀疏连接可以让学习的参数变得很少，而平移不变性则不关心物体出现在图像中什么位置。

即使是只有4 字节的头也需要重复的recv 调用才能确保完全读人，因为recv 不是原子的。
ELB是AZ服务，不能跨区域分散负载。

Nt*系列API将直接调用对应的函数代码，而Zw* 系列API则通过KiSystemServiee 最終跳转到 对应的函数代码处。重要的是两种调用对内核中Previous Mode 的改变:如果从用户模式调用Native API，则PreviousMode是用户态:如果从内核模式调用Native API，则PreviousNode是内核态。当 Previous 为用户态时，NativeAPI将对传递的参数进行严格的检查，而Previous为内核态时则不会
在调用用户模式Nt*APT时，不会改变PreviousMode的状态;在调用Zw*API时，会将Previous Mode改为内核态。因此，在进行KernelModeDriver开发时，使用Zw*系列API 可以避免额外的参数列表检查，从而提高效率

假设虚拟 机执行了很多次obi.equals0 发现obi的类型都是String，那么虚拟机可以乐观地认为 ob 就是String类型，继而直接调用String.cquals，省去了在询o5j避两数表的开销。

Truffle 将AST 节点编译为机器代码使用的编译器是Graal ，这是一个用Java缋 写 的即时编译器。前面提到，truffle是一个Java框架，那么一个用Java语言编写的即时 编译器要如何编译Java 代码呢?答染足通过JEP 243 的JVMC1。JVM是用C++语言 编写的，在J M中内登了两个用C++编写的即时编译器，C1和C2。一般初繁的代码 先用C1 编洋，这些代码即热点。如果热点继续，則使用C2编译。JVMCI 相当于把本 该交给C2编译的代码交给Gr a al 编译，然后使用编译后的代码。用Java 写即时编译器 香起来很神奇，其安很正常，因为即时编译说到底就是将一段byte[]代码在运行时转 换为另一段byte[]，可以用任何语言实现，只是实現过程中的难易程度不同。

以前是直接内核inline HOOK、SSDT HOOK一起上。后来有了PatchGuard，大都改成事件回调了
再加一些文件filter驱动

对Android的Frida来讲，实现Hook需要在手机端运行一个frida-server，frida-server本身集成了Google的V8解释器（新版的好像换成了Duktape），用于解析JavaScript代码。frida-server的实现原理是使用ptrace系统调用在目标进程中注入一段代码，随后断开ptrace调试，该代码与frida-server之间使用pipe管道进行通信，frida-server与客户端的代码之间使用adb的端口转发进行通信，因此frida-server需要使用Root权限运行。


不是。FPGA加速在于它和GPU和CPU的体系结构就不同。FPGA不是冯·诺伊曼结构的，而是一个代码描述的逻辑电路。FPGA只要片上逻辑门和引脚够多，全部输入、运算和输出都在一个时钟周期内完成。FPGA因为一个时钟周期执行一次全部烧好的电路，某种角度来说它一个模块就一句超复杂“指令”，所以不同模块也算是不同逻辑序列，并且这个序列里就一条指令。单元间通信这些冯·诺伊曼结构的东西对于FPGA都不是问题，不同运算单元间本身就硬件直连，所以才能做到数据并行和流水线并行共存（GPU流水线并行能力约为0），真的要比单算浮点运算能力，当前常见的FPGA不比GPU好。因此，如果是需要低延迟的预测推理，每批大小比较小时，FPGA更合适

要向内核驱动发送IOCTL，Windows用户空间应用程序必须调用DeviceIoControl()
。这些对DeviceIoControl()
的调用导致Windows的I/O管理器生成一个IRP_MJ_DEVICE_CONTROL
请求，发送给最顶层驱动程序。驱动程序实现一个特殊的调度例程来处理IRP_MJ_DEVICE_CONTROL
请求，而这个调度例程被一个MajorFunction[]
数组引用。这个数组是DRIVER_OBJECT
数据结构的成员，此结构体可在WDK（Windows Driver Kit）[6]
的ntddk.h中找到。

NX位（No eXecute bit）是CPU的一种特性，可防止在进程的数据内存页面上执行代码。许多现代操作系统都采用了NX位。在微软的Windows上，硬件强制的DEP（Data Execution Prevention）会开启兼容CPU的NX位，将进程中除明确包含可执行代码的其余内存位置全都标记为不可执行。DEP是Windows XP SP2以及Windows Server 2003 SP1引入的。在Linux上，NX由支持AMD和Intel 64位CPU的内核强制实施。对于在老的32位x86CPU上运行的Linux，ExecShield 和PaX[3]
模拟了NX功能

如果没有将移动构造函数和移动赋值运算符声明为 noexcept，std::vector 会使用比较低 效的复制构造函数。当发生这种情况时，编译器可能不会给出警告，代码仍然可以正常运 行，不过会变慢。

Windows下的反调试方法有很多，网上也有很多文章对其进行了总结，而且在Windows下通过OD的StringOD插件可以过滤掉大多数的反调试方法。

Windows操作系统从一开始（NT开始）就在运行设备驱动程序时进行动态加载。
有些操作系统还提供了一些其所特有的通信机制，例如Windows支持的进程通信方式就有所谓的剪贴板（clipboard）、COM/DCOM、动态数据交换（DDE）、邮箱（mailslots

我们希望这个类的对象拥有句柄的独占所有权，因此通过将以下内容放在该类的公共部分
来删除拷贝构造函数和拷贝赋值运算符:
     void operator=(intptr_t p) { handle = p; }
     search_handle(search_handle& h) = delete;
     void operator=(search_handle& h) = delete;


现代操作系统，如Windows，为自旋锁提供了更有效的替代方案。例如，基于异步过程调用（Asynchronous Procedure Call，APC）的互斥锁允许线程在互斥锁上等待并进入等待状态。一旦互斥锁变得可用，操作系统就会唤醒等待线程并交出互斥锁的所有权。这允许其他线程在CPU上执行生产性工作，否则这些CPU将被自旋锁占用。
成功需要机遇和运气，甚至能力变强也需要机遇和运气。做个普通人也没有什么不好，本质上成功的人也仍然是一个普通人。

移位经常被用于对乘法运算的优化。由于不需要像乘法那样设置寄存器、移动数据，移位会更简单、更快。shl eax，1的计算结果和将EAX乘以2一样:左移2位则相当于乘以4，左移3位相当于乘以8。一般的，左移n位相当于乘以2。

在反汇编时，经常会看到xor指令。例如，xor eax, eax就是一种将EAx寄存器快速置0的方法。这么做是为了优化，因为这条指令只需要2个字节，而mov eax，a需要5个字节。

资产定价模型是单因子模型，是市场风险因子；套利定价理论（ArbitragePricing Theory，APT）模型是多因子模型，假设证券收益与一组未知因子（特征）线性相关，不仅与市场相关，还与某些特征相关，但没有指出这些因子是哪些；FF（Fama & French）三因子模型由Fama和French提出，于1992年对美国股票市场决定不同股票回报率差异的因素进行了研究发现，即市值较小、市场账面较低的两类公司更有可能取得优于市场水平的平均回报率，超额回报率可由其对三类因子（市场风险溢价因子、规模因子和价值因子）来解释；在过去20年里，研究者对三因子模型进行了实证分析，发现有些股票的Alpha显著不为0，说明这三类因子并不能解释所有超额收益，为此Fama和French于2015年又提出了五因子模型，增加了盈利因子和成长因子这两类。大家公认这些类别的因子肯定能够获得比市场基础回报要多的收益（风险因子收益），可以自己寻找更多的没有被公认的额外因子收益（Alpha收益），即广义Alpha收益=公认的风险因子收益+Alpha收益。

有一次，我跟唐琦去拜访苹果中国区的市场部经理，对方对我们极其热情，先是给我们展示各种样子的苹果电脑，然后又打开OS X，告诉我们这个新的操作系统有多好用、支持多少重要的软件和游戏。听说我也会编程，他索性送了一台Mac桌面电脑给我们，让我一定要看看苹果的Aqua界面有多漂亮、Objective C这个语言有多优雅。我把电脑搬回编辑部，试着玩了几天，着实也想不出该怎么帮他们推广，因为在一个既缺用户又缺应用的平台上，编程语言和开发环境的好与不好，其实根本就无关紧要。后来我经常想，当年这位市场部经理要是一直熬下来可就修成正果了。这两年有人问我，要是能倒退回去十年会干什么，我就打趣说：我会削尖脑袋去应聘苹果市场部，然后把所有闲钱都拿来买房子。


阿克曼函数在高等数学中有一些应用，但它出名主要的原因还是它的递归程度很深。只要把它那两个整型参数的值稍微提升一点点，函数执行递归调用的次数就会大幅增加。阿克曼函数以两个（非负的）整数 m 与 n作为参数。如果 m 为 0，那么该函数就遇到了基本情况，它返回 n + 1。否则，函数进入递归情况：若n 为 0，函数返回 ackermann(m - 1, 1)；若 n 大于 0，函数返回 ackermann(m - 1, ackermann(m, n - 1))。你可能并不觉得这些规则有什么特别的地方，但是注意，这个函数的调用次数增长得相当快。ackermann(1, 1)的值需要执行 3 次递归调用才能求出。计算ackermann(2, 3)需要执行 43 次，计算ackermann(3, 5)需要 42437 次，至于计算 ackermann(5, 7)需要多少次，我也不知道，因为计算这个值所花的时间是宇宙年龄的好几倍。


从本质上说，大量元素之间的排列与组合问题可以通过小量元素之间的排列与组合而得以解决。这个特征意味着，此类问题很适合用递归来解决。

在Java中，可以使用==运算符来比较基本数据类型和引用类型。如果应用在基本数据类型上，Java的==比较的是值，然而在引用类型上==比较的是引用。因此，在Java中，众所周知的实践是总是调用equals，如果忘记了这样做当然也会导致众所周知的问题。
在Kotlin中，==运算符是比较两个对象的默认方式：本质上说它就是通过调用equals来比较两个值的。因此，如果equals在你的类中被重写了，你能够很安全地使用==来比较实例。要想进行引用比较，可以使用===运算符，这与Java中的==比较对象引用的效果一模一样

这里需要指出的是，编程语言的语法规则能够将源代码拆分成树状结构，而这种结构与我们以自然语言（如英语）的语法规则拆分句子所得到的树形图其实是相似的。因此，递归这种编程技巧很适合用在编译器上面。

在几何学中，求图形面积的方法很多，只要知道曲线的方程，可以通过多种方法求解，对于不规则图形曲线可以求得近似解。蒙特卡罗方法则另辟蹊径 :将纸 挂到墙 上，然后往纸 上投掷 飞镖， 飞镖有的会投 中在封闭的图形内，有的会投掷在外面。 这样连续投掷，统计投掷在图形内部的投掷次数和总的投掷次数，计算投中的概率，然后再乘以总的纸张面，可以作为不规则图形区域的面积的近似值。这就是蒙特卡罗方法的问题求解思想 。

CPU跳转指令的实现其实也很简单：根据ALU的运算结果和输出的Flags标志位，直接修改PC寄存器的地址即可，控制单元会自动到PC指针指向的内存地址取指令、翻译指令和运行指令。

哥德尔不完备性定理都没有说明，存在数学家能证明但计算机无法证明的数学命题。据我们所知，凡是能由人类证明的定理，计算机也能证明。计算机无法解决的不可计算问题，人类也无法解决。

A human hair is approximately 80,000- 100,000 nanometers wide
TCP SYN扫描（-sS参数）。因为不必全部打开一个TCP连接，所以这项技术通常称为半开扫描（Half-Open）。可以发出一个TCP同步包（SYN），然后等待回应。如果对方返回SYN-ACK（响应）包就表示目标端口正在监听；如果返回RST数据包，就表示目标端口没有监听程序。如果收到一个SYN/ACK包，源主机就会马上发出一个RST（复位）数据包断开和目标主机的连接。此时Nmap转入下一个端口。这实际上是由操作系统内核自动完成的。这项技术最大的好处是，很少有系统能够把这些记入系统日志。不过，需要root权限来定制SYN数据包。

指令缓存和数据缓存的拆分，使得我们的CPU在进行数据访问和取指令的时候，不会再发生资源冲突的问题了。
在利率互换交易中，为了对冲利率风险或进行投机，交易双方根据名义本金金额(这个金额实际上没有被交换)交换现金流。例如，假设甲公司刚刚发行了 100 万美元的五年期债券，其年利率为伦敦银行同业拆息(LIBOR)加1.3%(即130个基点)。另外，假设伦敦银行同业拆借利率(LIBOR)为2.5%，甲管理层担心利率会上升。管理团队找到了另一家公司乙，该公司愿意以伦敦银行同业拆借利率 (LIBOR) 外加1.3%的年利率向甲支付名义本金为 100 万美元的 5 年期贷款。换句话说，乙将为甲最新发行债券的利息买单。作为交换，甲以 100 万美元的名义价值的 5% 的固定年利率向乙支付五年。如果未来 5 年利率大幅上升，甲将从互换交易中受益。如果利率下降、保持不变或只逐步上升，乙就会受益。

make_sentient(HolmesIV*):
        mov     BYTE PTR [rdi], 1
        ret
make_sentient(HolmesIV&):
        mov     BYTE PTR [rdi], 1
        ret

undefined runtime behavior: it might crash, it might give you an error, or it might do something completely unexpected.
#include <cstdio>
constexpr int isqrt(int n) {
  int i=1;
  while (i*i<n) ++i;
  return i-(i*i!=n);
}
int main() {
constexpr int x = isqrt(1764);u printf("%d", x);
}

x86和x64体系结构中的页目录基地址寄存器 ( page directory base register ) 是CR3，ARM中则是转换表基址寄存器( TTBR, Translation Table Base Register )

计数器的准确程度由高到低排列如下:
RDTSC>NtQueryPerformanceCounter0>GetTickCount0 NtQueryPerformanceCounter0 与 GetTickCountO使用相同硬件 ( PerformanceCounter ) ，但二者准确程度不同( NtQueryPerformanceCounterQ准确度更高 ) 。而 RDTSC是CPU 内部的计数器，其准确程度最高。基于时间的方法与基于计数器的方法在实现 过程上比较类似，原理也差不多。
葛立恒数门就大到描述不 清楚了，而且它又是一个实实在在的有限的数，而不是无穷大。

cmake -G "Visual Studio 17 2022" -A Win32|ARM64|x64|ARM
-DCMAKE_BUILD_TYPE=Release

There are different ways for the DBMS to allocate timestamps for transactions [5]. Each have their own
performance trade-offs.
• Mutex: This is the worst option. Mutexes are always a terrible idea.
• Atomic Addition: Use compare-and-swap to increment a single global counter. Requires cache
invalidation on write.
• Batched Atomic Addition: Use compare-and-swap to increment a single global counter in batches.
Needs a back-off mechanism to prevent fast burn.
• Hardware Clock: The CPU maintains an internal clock (not wall clock) that is synchronized across
all cores. Intel only. Not sure if it will exist in future CPUs.
• Hardware Counter: Single global counter maintained in hardware. Not implemented in any existing
CPUs.

分、秒、毫秒（ms）、微秒（µs）、纳秒（ns）、皮秒（ps）、飞秒（fs）、阿秒（as）、仄秒（zs）

printf 'int main(){printf("hello world\\n");}' \
| gcc -w -x c - && strace ./a.out

跳转指令需要依赖自身的执行结果来决定到底要不要跳转，那么在跳转指令没有执行完的情况下 CPU 怎么知道后面哪个分支的指令能进入到流水线呢?
Around the web you will commonly find using namespace std; to avoid std::. Setting the global namespace is a pretty bad practice, try to avoid it.
unhook dll中利用新映射的dll的.text区块复制到原本被hook的虚拟地址，实现覆盖，解除edr对dll的hook，实现edr的检测规避。

use std::cmp::Ordering::Less; 
fn main() {
    let mut floats = vec![3.1, 1.2, 4.5, 0.3, std::f32::INFINITY, std::f32::NAN ];
    floats.sort_by(|a, b| a.partial_cmp(b).unwrap_or(Less));
    for f in floats  {
        println!("{}", f);

        
    }
    println!("{}", "xxhh");
}


在正常程式上新增一個惡意區段用於存放惡意程式碼、並將程式入又指向惡意程式碼之上，使感染後的程式執行後會直接觸發我們的惡意程式碼內容。

不過Shellcode 有別於PE程式檔案運行起來時會有Kernel協助做檔案映射或者程式裝載修正等 (因此PE 檔案有引入函數表可以拿任意想要的系統函數位址)因此對初學者而言撰寫Shellcode 上比起直接C/C++開發來得困難許多:困難點主要在於如何不仰賴引入函數表之下呼叫系統函數 。

The 𝗻𝗼𝗲𝘅𝗰𝗲𝗽𝘁 specifier is a method through which the developer can inform the compiler that certain function 𝘄𝗶𝗹𝗹 𝗻𝗼𝘁 𝘁𝗵𝗿𝗼𝘄 𝗮𝗻 𝗲𝘅𝗰𝗲𝗽𝘁𝗶𝗼𝗻. This information can be potentially used by the compiler to improve the assembly code by doing certain optimizations. One of the examples could be the fact that the STL containers prefer move operations (e.g. when resizing) on your custom types only when they have a move constructors defined with the 𝗻𝗼𝗲𝘅𝗰𝗲𝗽𝘁 specifier.

当在C程序中看到goto时，多半是与资源的分配与释放有关。
Of what possible legitimate use are functions like CreateRemoteThread, WriteProcessMemory, and VirtualProtectEx?

There are a bunch of functions that allow you to manipulate the address space of other processes, like WriteProcessMemory and VirtualAllocEx. Of what possible legitimate use could they be? Why would one process need to go digging around inside the address space of another process, unless it was up to no good? These functions exist for debuggers. For example, when you ask the debugger to inspect the memory of the process being debugged, it uses ReadProcessMemory to do it. Similarly, when you ask the debugger to update the value of a variable in your process, it uses WriteProcessMemory to do it. And when you ask the debugger to set a breakpoint, it uses the VirtualProtectEx function to change your code pages from read-execute to read-write-execute so that it can patch an int 3 into your program. If you ask the debugger to break into a process, it can use the CreateRemoteThread function to inject a thread into the process that immediately calls DebugBreak. (The DebugBreakProcess was subsequently added to make this simpler.) But for general-purpose programming, these functions don’t really have much valid use. They tend to be used for nefarious purposes like DLL injection and cheating at video games.


There are several different hash map implementations available – std::unordered_map, absl::flat_hash_map, boost:: hash maps, emhash7::HashMap, folly::AtomicHashmap, robin_hood::unordered_map, tsl::hopscotch_map, and many more.

G1相关概念非常多，有一个重点就是Remembered Set，用于记录和维护region之间对象的引用关系。为什么需要这么做呢？试想，新生代GC是复制算法，也就是说，类似对象从Eden或者Survivor到to区域的“移动”，其实是“复制”，本质上是一个新的对象。在这个过程中，需要必须保证老年代到新生代的跨区引用仍然有效。

与整体系统架构设计相比，编程语言的影响并没有那么大。交易公司也会租用交易所的机位，用光纤直连，以及把不需要经常变动的部分用硬件实现等等来降低延迟。


小彭老师推出新系列之“C++辟谣行动"，旨在收集一些群友常见的误区。今天小彭老师在网上冲浪时发现，许多C++博客（甚至百度百科也）声称"inline的作用是把插入到调用者体内”，甚至还认为“inline函数不能包含复杂的结构控制例如while和switch”更是逆天。小彭老师立即出面澄清：inline关键字的作用早已不是内联优化，而是为了突破“唯一定义原则”。如今的编译器早已高度智能，只要开启-O3优化开关，内联优化是否必要他自有分寸，用不着你提醒（如果要强制内联优化也是用__forceinline这类编译器内置的特殊指令，而不是C++标准的inline关键字）。本期课程中，小彭老师首先详细介绍了static的作用和痛点，并趁热打铁介绍了inline真正的作用：让多个翻译单元共享同一个定义，最终，利用inline解决了头文件中定义函数或变量会导致链接器报错“符号冲突”的问题。
### knowledge
tokio-util，它提供了 tokio 下的 trait 和 futures 下的 trait 的兼容能力。

cookie venders, to check online-ad domain
https://text-compare.com/

webrtc信令服务器
1. 房间管理。即每个用户都要加入到一个具体的房间里，比如两个用户 A 与 B 要进行通话，那么它们必须加入到同一个房间里。
2. 信令的交换。即在同一个房间里的用户之间可以相互发送信令。


https://github.com/snowplow
https://www.mautic.org/
一个完整的 eBPF 程序通常包含用户态和内核态两部分：用户态程序通过 BPF 系统调用，完成 eBPF 程序的加载、事件挂载以及映射创建和更新，而内核态中的 eBPF 程序则需要通过 BPF 辅助函数完成所需的任务。
第一步，使用 C 语言开发一个 eBPF 程序；
第二步，借助 LLVM 把 eBPF 程序编译成 BPF 字节码；
第三步，通过 bpf 系统调用，把 BPF 字节码提交给内核；
第四步，内核验证并运行 BPF 字节码，并把相应的状态保存到 BPF 映射中；
第五步，用户程序通过 BPF 映射查询 BPF 字节码的运行状态
---
在编写简单的 eBPF 程序，特别是编写的 eBPF 程序用于临时的调试和排错时，你可以考虑直接使用 bpftrace ，而不需要用 C 或 Python 去开发一个复杂的程序。
---
使用套接字映射转发网络包需要以下几个步骤：

创建套接字映射；
在 BPF_PROG_TYPE_SOCK_OPS 类型的 eBPF 程序中，将新创建的套接字存入套接字映射中；
在流解析类的 eBPF 程序（如 BPF_PROG_TYPE_SK_SKB 或 BPF_PROG_TYPE_SK_MSG ）中，从套接字映射中提取套接字信息，并调用 BPF 辅助函数转发网络包；
加载并挂载 eBPF 程序到套接字事件。

https://korkortonline.se/inloggad/fraga/48/
PDO exception: SQLSTATE[HY000] [2002] Permission denied
---前端流程图，见 https://github.com/mautic/mautic/blob/5.x/LICENSE.txt
jsplumbtoolkit

---
利用httpx快速检测Confluence CVE-2023–22515
httpx -l hosts.txt -follow-redirects -title -path “server-info.action?bootstrapStatusProvider.applicationConfig.setupComplete=false” -ms "success" -o CVE-2023–22515.txt ​​​
### todo pools

- 使用 Ultimate SMS v3.7.0 自建用于营销的群发短信应用程序
- 机器人检测 浏览器指纹
- 弄清楚tracardi
- lingo项目的爬虫和自动翻译
- copy交友项目
- clickhouse的研究使用
- 数据湖
- 在线广告体系


在 Linux 上使用 tcmalloc [1] 来取代默认的分配器 [2]，我们可以得到更好的测试结果：
export PATH="$PATH:$(python3 -m site --user-base)/bin"


nim c hook.nim 
TPU并没有设计成一个独立的“CPU“，而是设计成一块像显卡一样，插在主板PCI-E接口上的板卡。
虚函数使用起来比较繁琐，程序的可读性也不够清晰明朗，而std::function、std::bind等新标准的出现可以完全替代虚函数，

在几何学中，求图形面积的方法很多，只要知道曲线的方程，可以通过多种方法求解，对于不规则图形曲线可以求得近似解。蒙特卡罗方法则另辟蹊径 :将纸 挂到墙 上，然后往纸 上投掷 飞镖， 飞镖有的会投 中在封闭的图形内，有的会投掷在外面。 这样连续投掷，统计投掷在图形内部的投掷次数和总的投掷次数，计算投中的概率，然后再乘以总的纸张面，可以作为不规则图形区域的面积的近似值。这就是蒙特卡罗方法的 问题 求解 思想 。


使用 TCP 的时候，数据包 如果在发送过程中丢失了，就会再次发送。由于在该数据发送完毕之 前下一个数据无法送达，所以会造成相当大的通信延迟。

fuzz
	code like a pro in rust
	C++23 Best Practices
valgrind
	c现代编程
x86 汇编
	自制编译器
	From Source Code To Machine Code
simd
	Performance Analysis and Tuning on Modern CPUs
perf
	Performance Analysis and Tuning on Modern CPUs

----
book pearls
[tcp sockets编程]
标准Ruby实现(MRI)包含了一个全局解释器锁(Global Interpreter Lock，GIL)。它确保Ruby解释器一次只做一件有潜在危险的事。在 多线程环境中，它才能真正发挥作用。当一个线程进行活动时，其他 线程全部处于阻塞状态。这使得MRI可以使用更安全、更简单的代码来编写。好在GIL能够理解阻塞式IO。如果有一个线程在进行阻塞式IO(例如 一个阻塞式read)，MRI会释放GIL并让另一个线程继续执行。当阻 塞式IO调用完成后，线程就等待下一次运行。
——————————
1 Matz’s Ruby Interpreter. ——译者注


[数据库事务处理的艺术]
MVCC技术，因为同一个数据项存在多个版本，使得不同事务的对同一个数据项的读操作可以根据其读时刻的快照作用在不同的版本上，因而避免了1.1节讨论的三种读数据异常现象，所以对于读-写、写-读操作允许并发，提高了并发度

创建ksql表，股票分析应用

CREATE TABLE stock_txn_table (symbol VARCHAR, sector VARCHAR, 
　　　　　　　　　　　　　　　industry VARCHAR, shares BIGINT, 
　　　　　　　　　　　　　　　sharePrice DOUBLE, 
　　　　　　　　　　　　　　　customerId VARCHAR, transactionTimestamp 
　　　　　　　　　　　　　　　STRING, purchase BOOLEAN) 
　　　　　　　　　　　　　　　WITH (KEY='symbol', VALUE_FORMAT = 'JSON', 
　　　　　　　　　　　　　　　KAFKA_TOPIC = 'stock-transactions');

SELECT symbol, sum(shares) FROM stock_txn_stream ➥   WINDOW TUMBLING (SIZE 10 SECONDS) GROUP BY symbol


[智能计算实验系统教程]
tensorflow/core/kernels/cwise_op_power_difference.cc  REGISTER_OP REGISTER_KERNEL_BUILDER重新编译TensorFlow源码
Q:how to add new operator to tensorflow? like using macro REGISTER_KERNEL_BUILDER?

[计算机系统开发与优化实战]
获取设备信息
include <iostream>
#include <cuda_runtime_api.h>
bool InitCUDA(){   
int count;   cudaGetDeviceCount(&count);// 获得CUDA设备的数量   if(count == 0)   {      std::cout<<"T
here is no device.\n" ;      return false;
   }   int i;   for(i = 0; i < c
ount; i++)   {      cudaDevicePro
p prop;//CUDA设备的属性对象      if(cudaGetDev
iceProperties(&prop, i) == cud
aSuccess)      {         std::cout<
<"设备名称："<<prop.name<<"\n" ;         std::cout<
<"计算能力的主代号："<<prop.major<<"\t"
<<"计算能力的次代号："<<p
ro
p.         minor<<"\n
" ;         std::cout<
<"时钟频率："<<prop.clockRate<<"\n"
 ;         std::cout<
<"设备上多处理器的数量："<<prop.multiProc
essorCount<<"\n"
 ;
         std::cout<
<"GPU是否支持同时执行多个核心程序:"<<prop.co
ncurrentKernels<
<"
\n" ;      }


《企业信息安全体系建设之道》
反垃圾邮件技术（Anti-spam Technique）是指针对垃圾邮件的对抗技术，主要的反垃圾邮件技术如下。
● 发送方策略框架（Sender Policy Framework，SPF）协议是为了防范垃圾邮件而提出来的一种DNS记录类型。SPF是一种TXT类型的记录，其本质是告诉接收方的邮件服务器，当前域名列表清单上所列IP地址发出的电子邮件都是合法的，并非冒充的垃圾邮件。
如果SPF配置正确，并且使用SPF中的邮件服务器发送电子邮件，接收方的邮件服务器检查SPF返回结果正确，此邮件被接收。若发送方的邮件服务器并非在SPF中配置的，SPF返回结果错误，则判定此邮件为垃圾邮件，那么将被拒收，这样就在一定程度防止接收到伪造的垃圾邮件。
● 域密钥识别邮件标准（DomainKeys Identified Mail，DKIM）协议是为了防范垃圾邮件而提出来的一种DNS记录类型。DKIM是一种TXT类型的记录，其本质是通过在每封电子邮件上增加加密标志，接收方的邮件服务器通过非对称加密算法解密并对哈希值进行比对，从而判断电子邮件是不是伪造的。
DKIM的基本工作原理是基于密钥认证方式生成一组密钥对：公钥和私钥。公钥将发布在DNS中，私钥会存放在发送方的邮件服务器中。发送电子邮件时，发送方会在电子邮件标头插入DKIM签名。而接收方的邮件服务器收到电子邮件后，会通过DNS获得DKIM公钥，利用公钥解密电子邮件标头中的DKIM信息中的哈希值，接收方的邮件服务器计算收到的电子邮件的哈希值，两个值进行比较，如果一致则证明此邮件合法，此邮件被接收。如果验证为不合法，则判定为垃圾邮件，此邮件被拒收。由于数字签名是无法仿造的，因此这项技术对于垃圾邮件的判别效果极好。
● 基于域的消息身份验证、报告和一致性（Domain-based Message Authentication、Reporting and Conformance，DMARC）协议是为了防范垃圾邮件而提出来的一种DNS记录类型。DMARC同样也是一种TXT类型的记录，是基于现有SPF和DKIM协议的可扩展电子邮件认证协议，目的是给电子邮件域名所有者提供保护他们的域名的能力。SPF和DKIM缺少反馈机制，这两个协议未定义如何处理伪造邮件。DMARC的主要用途在于设置相应的处理策略，当接收方的邮件服务接收到来自某个域未通过身份验证的电子邮件时，应执行规定的处理机制（如拒绝电子邮件或标记为垃圾邮件等）。
DMARC协议基于现有的DKIM和SPF两大主流电子邮件安全协议，由发送方在DNS里声明自己采用该协议。当接收方（需支持DMARC协议）收到发送过来的邮件时，则进行DMARC校验，若校验失败还需发送一封报告到指定电子邮箱地址



[编程之美：微软技术面试心得]
能帮助你了解当前线程/进程/系统效能的API大致有以下这些。
1.Sleep（）——这个方法能让当前线程“停”下来。
2.WaitForSingleObject（）——自己停下来，等待某个事件发生。
3.GetTickCount（）——有人把Tick翻译成“嘀嗒”，很形象。
4.QueryPerformanceFrequency（）、QueryPerformanceCounter（）——让你访问到精度更高的CPU数据。
5.timeGetSystemTime（）——另一个得到高精度时间的方法。
6.PerformanceCounter——效能计数器。
7.GetProcessorInfo（）/SetThreadAffinityMask（）。遇到多核的问题怎么办呢？这两个方法能够帮你更好地控制CPU。
8.GetCPUTickCount（）。想拿到CPU核心运行周期数吗？用用这个方法吧。
了解并应用了上面的API，就可以考虑在简历中写上“精通Windows”了


[普林斯顿计算机公开课]
panopticlick.eff.org可以让你估计自己到底有多独特。

「实时流计算系统设计与实现」
https://github.com/alain898/real_time_stream_computing_book_source_code

「mysql」
业务优化写法：
采用这种写法，在页面上只能通过点击More来获得更多数据，而不是纯粹的翻页。

题
在x86架构中，int 3
指令会生成一个软中断，用户空间的程序（如SBI库或调试器）能够通过操作系统提供的SIGTRAP信号（在Linux操作系统上）捕获该中断。int 3
的关键在于它的长度只有1字节，所以你可以用它覆盖任何指令，而不必担心覆盖相邻的指令。int 3
的操作码是0xcc

[c 语言最佳实践]
“杀鸡用牛刀”的情形经常表现为滥用标准C库接口或者某些第三方函数库的接口。最为常见的便是滥用STDIO接口。比如下面的两个函数调用：
sprintf(a_buffer, "%s%s", a_string, another_string);
sscanf(a_string, "%d", &i);
这两个函数调用分别完成了串接两个字符串以及将字符串转换为一个整型值的功能。
如第8章所述，在内存中执行格式化输入输出的STDIO接口，如sprintf()和sscanf()函数，首先会调用fmemopen()函数构造一个基于内存的FILE对象，然后调用fprintf()、fscanf()等函数完成最终的格式化输入输出功能，最后销毁临时构建的FILE对象。因此，这些接口的开销较大：不论从空间复杂度看还是从时间复杂度看，都远大于strcat()、strcpy()、atoi()等函数。因此，如果只是想完成字符串的串接或者单个字符串转整数的功能，大可不必调用STDIO接口，而应该调用其他的标准库函数，如下所示：
// sprintf(a_buffer, "%s%s", a_string, another_string);
strcpy(a_buffer, a_string);
strcat(a_buffer, another_string);　
// sscanf(a_string, "%d", &i);
i = atoi(a_string);
如果对性能仍不满足，则可以进一步优化以上字符串串接代码。strcat()首先会找到a_buffer的尾部，然后复制another_string的内容直到字符串末尾。但实际上，strcpy()函数在其内部实现中一定已经循环到了a_string的尾部，故而也知道a_buffer中字符串的尾部地址。然而，strcpy()函数并没有返回a_buffer中指向字符串尾部的指针，返回的却是a_buffer本身。幸好，为满足这一需求，标准库特意增加了一个接口stpcpy()，该接口复制字符串并返回指向其尾部的指针：
#include <string.h>
char *stpcpy(char *dest, const char *src);
故而，我们可以进一步优化以上用来串接两个字符串的代码：
// sprintf(a_buffer, "%s%s", a_string, another_string);
char *p = stpcpy(a_buffer, a_string);
strcpy(p, another_string);

[java程序性能优化实战]
选择一些现成的无锁并行框架就成为解决这个问题的最好方法。

Amino框架就是其中之一。它是Apache下的一个分支项目，提供了可用于线程安全且基于无锁算法的一些数据结构，同时还内置了一些多线程调度模式。


常用服务保留的端口号，可以在Linux下使用cat/etc/services进行查看。

《Linux多线程服务器编程》（2013年），陈硕著，讲述了多线程编程的各种方法。其中提到，几乎全部的线程问题都可以通过mutex和CountDownLauch完美解决。

在Windows上，我们使用DirectShow技术来实现设备枚举，这基于C++的组件对象模型技术。在macOS和iOS上，我们使用AVFoundation的AVCaptureDevice来实现枚举，这基于Objective-C的OC类。在Android上，我们使用android.hardware.Camera2来实现枚举，这基于Java的对象包。不论在哪个平台上，我们最终都需要将数据转换到C++层，以便后续统一处理。

[Optimizing Cloud Native Java Practical Techniques for Improving JVM Application Performance]
apache/pekko Build highly concurrent, distributed, and resilient message-driven applications using Java/Scala
echo 0 > /proc/sys/kernel/randomize_va_space

#include <stdio.h>
#include <stdlib.h>
unsigned long get_sp(void)
{
    __asm__("movl %esp, %eax");
}
int main(void)
{
    printf("malloc: %p\n", malloc(16));
    printf(" stack: 0x%lx\n", get_sp());
    return 0;
}
在启用 ASLR 的状态下，反复运行这个程序，我们会发现每次显示的地 址都不同。
https://github.com/kenjiaiko/binarybook

---

#include <pthread.h>
int Global;
void *Thread1(void *x) {
  Global = 42;
  return x;
}
int main() {
  pthread_t t;
  pthread_create(&t, NULL, Thread1, NULL);
  Global = 43;
  pthread_join(t, NULL);
  return Global;
}

clang -fsanitize=thread -g -O1 tiny_race.c & ./a.out


如果把线程池写成 busy-loop，那程序一运行起来就把 CPU 占满，你还怎么监控程序和操作系统的负载？如何知道是不是由于客户请求过多造成系统负担过重，要不要分流或截流？如何知道是不是某个非法请求触发了程序中的 bug，导致死循环？如果用正常的写法，线程池没有任务时就等在条件变量上，那么我监控一下 CPU 使用率就知道系统有没有过异常。

多CPU共享一块内存的结构很难再有大的发展，各个核之间的数据同步和控制协议的复杂度随着核的数量上升而成几何级数上升，并发访问性能却不断下降，传统的SMP结构如今碰到了很大瓶颈，因此同物理主机内部也出现了 NUMA结构，让不同核心访问各自独立的内存区域，由此核心数量可以大大提升，Linux内核已早已支持这样的结构。而很多程序至今仍然用SMP的方式进行编码。倘若哪天NUMA逐步取代SMP时，要写高性能服务端代码，共享内存这玩意儿，估计你想用都用不了了。




C:/Program Files/Microsoft Visual Studio/2022/Community/VC/Tools/MSVC/14.39.33519/bin/Hostx64/x64/cl.exe

Windows 核心编程,英文版叫Windows via C/C++。图书的作者是编写 Windows Sysinternals 套件的 Jeffrey Richter
王爽老师的《汇编语言(第 3 版)》
开发了一个服务器程序，手头上没有可用的客户端，如何使用 nc 命令模拟一个；或者反过来，开发了一个客户端程序，如果用 nc 模拟一个服务器端用于测试。

主线程创建一个新的工作线程，等待工作线程获取系统时间后写入文件；主线程从文件中读取时间内容显示出来。

这个例子中，我们用到了创建线程的 API、线程等待与通知 API、获取系统时间的 API、显示到控制台的 API

在 Windows 上，我们用到：

CreateThread 
WaitForSingelObject 
GetSystemTime 
CreateEvent/SetEvent 
std::cout

Windows 上常用的网络模型有 select、WSAEventSelect、WSAAsyncSelect、完成端口模型； 
Tanenbaum.A.S《现代操作系统》是一本讲解操作系统理论不错的书，作者 Tanenbaum.A.S 是 Linux 内核创始人 Linus Torvalds 的老师。
[Linux 高性能服务器编程

正如著名的编程大师 Charles Petzold 所说：

“显而易见，究竟用哪种方式编写应用程序最好，其实并无一定之规。应用程序本身的特性应该是决定采用何种编程工具的最主要因素，但是无论将来你采用什么样的编程工具，通过了解操作系统 API 从而深入理解操作系统的工作原理，这本身就有很重要的意义。操作系统是一个非常复杂的系统，在 API 之上加一层编程语言并不能消除其复杂性，最多不过是把复杂性隐藏起来而已。说不定什么时候，复杂的那一面迟早会蹦出来拖你的后腿，懂得系统 API 能让你到时候可以更快地挣脱困境。




在基本操作系统 API 之上的任何软件层或多或少都会限制你使用操作系统的全部功能。比如，你或许发现采用 Visual Basic 来编写你的应用程序非常理想，但是就有那么一两项非常基本的功能 Visual Basic 无法支持。往往这个时候你得非要调用基本 API 。作为直接使用操作系统 API 的程序员，我们的活动空间完全由 API 来规范，再没有什么其他方式比直接调用 API 更有效、更灵活多样了。”

https://cppguide.cn/pages/41538a/ 服务器开发精髓


用户态协议栈是非常有潜力的技术。

在内核TCP/IP协议栈上开发服务器，受限于内核的各种spin lock，吞吐达到600w pps差不多就到头了，网卡再强，cpu再多都无法线性提升吞吐。

用户态协议栈可以做到，在网卡上限内，吞吐随cpu的个数线性增长，不会有cpu浪费在内核spin lock里。scalability是用户态协议栈最吸引人的地方。PolarDB在IDC内部通讯就使用了RDMA之上的用户态协议栈来提高性能。

可预见用户态协议栈结合用户态壅塞算法，是跨IDC通讯的利器。

seastar是用户态TCP/IP协议栈的一种实现，性能上没啥毛病，cpu polling也没啥毛病，但基于future/promise的开发范式个人不太喜欢，对lambda引入的额外代价比较担心。

用户态TCP/IP协议栈的缺陷就是和内核TCP/IP协议栈相比，正确性和完备性缺少保证，多个开源实现版本很大程度上还都是toy。


QueueUserAPC function, which allows a thread to post an APC entry to another thread.

我告诉你一个识别 C++ 代码质量的诀窍：找几个 class，如果其 dtor 有 delete 或释放资源的操作，看看作者是否同时正确禁用了 copy ctor 和 assignment operator（或者正确实现了它们，如果 class 确实应该是 copyable 的话），这反映了作者设计 C++ class 的基本功：正确管理内存和其他资源，以及他有没有认真读过 Effective C++。
If a type were allowed to implement both Copy and Drop, it would create a contradiction. When a value goes out of scope, should it be dropped (using the Drop trait) or should it be left alone (because the Copy trait means it could have been copied somewhere else)?

--- asio
cmake_minimum_required(VERSION 3.12)
project(MyProject)
include(FetchContent)
set(CMAKE_CXX_STANDARD 20)

find_package(Threads REQUIRED)

find_package(Asio 1.21.0 QUIET)
if (NOT Asio_FOUND)
    FetchContent_Declare(asio GIT_REPOSITORY https://github.com/chriskohlhoff/asio.git GIT_TAG asio-1-21-0)
    FetchContent_GetProperties(asio)
    if (NOT asio_POPULATED)
        FetchContent_Populate(asio)
        add_library(asio INTERFACE)
        target_include_directories(asio INTERFACE ${asio_SOURCE_DIR}/asio/include)
        target_compile_definitions(asio INTERFACE ASIO_STANDALONE ASIO_NO_DEPRECATED)
        target_link_libraries(asio INTERFACE Threads::Threads)
    endif ()
endif()



add_executable(executors executors.cpp)
target_link_libraries(executors asio)

--- libevent

include(FetchContent)
FetchContent_Declare(cpr GIT_REPOSITORY https://github.com/libcpr/cpr.git
                         GIT_TAG 0817715923c9705e68994eb52ef9df3f6845beba) # The commit hash for 1.10.x. Replace with the latest from: https://github.com/libcpr/cpr/releases
FetchContent_MakeAvailable(cpr)
target_link_libraries(your_target_name PRIVATE cpr::cpr)


---proto
protoc --cpp_out=. test2.proto
g++ pb2.cc test2.pb.cc -std=c++17 -lprotobuf
ntp: RFC1305

code generation: /Users/boya/Library/Application Support/Sublime Text/Packages/User/cppsnippets/portal
powercfg -hibernate on
rundll32.exe powrprof.dll,SetSuspendState 

/Users/boya/workplace/heads/reverse-proxy/src/load_balancer.rs

spf /Users/boya/projects/xxhh4/
vectordb projects/vdb/
gocache /Users/boya/projects/goca/main.go (aslo csv enrichment with querying dynamodb)
editor /Users/boya/workplace/emailsms/myproject/static/index.html
expression evaluation /Users/boya/workplace/rust-tmp/xxhh2/src/main.rs
log /Users/boya/Library/Application Support/Sublime Text/Packages/User/rustsnippets/tmp/src/main.rs

wally: Distributed Stream Processing ⭐️ 1471 #golang

cmake+vcpkg: /Users/boya/Documents/cppweeklynews/tmp
--flink:
cd /Users/boya/Downloads/flink-1.20.0
bin/flink run ~/Library/Application\ Support/Sublime\ Text/Packages/User/cppsnippets/ttmp/app/build/libs/app.jar

--go汇编
/Users/boya/projects/go_plan9

越界、栈溢出、double free 等memory safety 问题只是安全的一小部分，而使用现代 c++配合编码规范、最佳实践、使用工具等，也能极大缓解 c++的内存安全问题。但 rust 绝不是仅仅只有内存安全特性，它从 haskell 和ocaml借鉴学了类型系统设计和函数式编程，代数数据类型极其自然地支持将领域知识被编码到类型里，由 type-checker进行约束，从而在编译器就拒绝错误代码，减少不必要的防御性逻辑及其运算开销，并且代码更加容易阅读和维护。不得无说使用 rust 的模式匹配和错误处理是极大的享受。完善的包管理和工具链，一键格式化与lint，再也不用人工配置clang-format和clang-tidy了。C++非官方包管理工具conan目前有1472个包，cargo的包管理平台有106672个包。同时Rust支持unsafe模式，完全跳过bound check。这让Rust的上限可以和C++持平。


现代操作系统通过这种机制实现进程地址隔离。每个进程关联到不同的CR3值，因此每个进
程都有⾃⼰特定的虚拟地址转换。

作为⼀般原则，Win32API函数都采⽤STDCALL调⽤惯例。stricmp函数采⽤CDECL调⽤惯例，所以第49⾏进⾏了栈清理

总结CDECL、STDCALL、THISCALL和FASTCALL调⽤惯例

APC（AsynchronousProcedureCall，异步过程调⽤）⽤于实现很多重要的操作，⽐如异步1/0
完成、线程挂起和进程关闭等。

write a reverse proxy for the purpose of anti-bot traffic
- use golang
- setup the golang project structure follwing the best practice
- write down the dns setup for the client who is going to use the proxy
- use memory to map the request to target
- support multiple, swappable, anti-bot traffic strategy
- one strategy is to dispath a proof of work challenge to browser, and validate the pow before forward the request to target
- design an another strategy 
- write the readme file 


write a simple database on top of kv storage
- with rust
- setup the golang project structure follwing the best practice
- use rocksdb for key value storage
- map table data to key-value storage, use combination of table id, index id, primary key id and column id
- support index when creating table
- use sqlparser for sql parsing, support crate table, insert, and select. leave update and delete for TODO
- use SCAN when executing select AST
- support repl so I can play with it
- support server mode so I can play with it with tcp 
- write a readme for the project



write a group chat application 
- with elixir
- support PWA client and native iOS/Android client
- access control to create group and join group
- message can be sent to group
- message can tag names specific
- group users can bookmark a message with notes
- group users can listen to messages meeting conditions specified, like if it containing a word, the stakehold will be notified
- bot can be added when creating a group
- bot can be consulted on its capablitiy like translating message into different language
- store messages in mysql 
- with a good way to partion messages for scaling


write a email campaign and marketing automation platform
- with elixir
- software as a service model
- customer can define lists
- customer can design form and the form will be embed in html page to collect contacts to list
- customer can import contacts into list
- for a sending, configuration options include: target list, template, sending time
- for marketing automation, a workflow can be defined, like when user registered, a welcome mail will be send. Desgin a flexible and powerful machanism for this kind of workflow




使用elixir 构建一个高可伸缩的 transcoding engine，可以把大的视频文件切割成上千个 chunk，每个（或者若干个）chunk 被分配到有数百个 core 的集群里的一个 core 上去单独转码，然后做后续处理。
Avoid duplicate requests while filling cache!

aws application discovery 

s3 requester pay


Index Condition Pushdown

I have written a resume skeleton in  markdown and I like you to replace the placehoders inside it with detailed and descriptive text to demonstrate the qualification for the job requirement. Please note placehoders might already has some hint with it and please follow and extend
Now I will give the text as follow: 
`
# Skill Set
{{}}


# Projects
- **Consents Hub**: A consent management system, which can manage the consent of users to receive emails, SMS, and other forms of communication.
    - Technical hilights: {{}}
    - My value: {{tunning for database query performance}}

- **BatchMail Engine**: A batch mail engine, which can send emails to multiple recipients in batch
  
  - Technical hilights: {{}}
  - My value: {{grpc and compression, improve throughput}}

- **

- **Audience 360**: A SaaS platform which collects 360 profiles information and provide interfaces with dsl for querying for insights
    - Technical hilights: {{}}
    - My value: {{high performant dynamic expression evaluation engine for audience targeting}}

- **Gastronomy Go**: allow user to sit at a table without coming to the counter, and order dishes online and will be served
    - Technical hilights: {{high availabiliity}}
    - My value: {{Independently design and deliver, Managed full development lifecycle}}
`
and Job Requirement as follows:
`
- Experience in Elixir and/or Erlang programming.
- Proficiency in Phoenix / LiveView and/or OTP is desirable.
- Distributed systems experience.
- Experience developing APIs.
`
please reply with the raw markdown text

用 Rust 并不意味着高性能和节约资源，反而很容易写出低效的代码 为了内部可变性引入不恰当的同步锁
没有注意所有权转移引入过多的 clone 和 copy，为了内部可变性引入不恰当的同步锁，并发原语的性能陷阱 etc.

creata a rust application for task execution
- task has input and output, both of them are strong typed and can be  serialized
- task execution logic is defined as a function 
- tasks can be submitted to a job queue and be pick up by workers
- support multiple queues so different task types are supported
- workers can be managed regarding concurrency level
- execution can track the progress and other necessary information
- create the README.md file for the project
- use proper persistent storage if needed

rust
	https://github.com/rust-lang/portable-simd
#![feature(portable_simd)]
use std::simd::f32x4;
fn main() {
    let a = f32x4::splat(10.0);
    let b = f32x4::from_array([1.0, 2.0, 3.0, 4.0]);
    println!("{:?}", a + b);
}

·第一个部分为_mm或_mm256。_mm表示其为SSE指令，操作的向量长度为64位或128位。_mm256表示AVX指令，操作的向量长度为256位。本节只介绍128位的SSE指令和256位的AVX指令。
·第二个部分为操作函数名称，如_add、_load、mul等，一些函数操作会增加修饰符，如loadu表示不对齐到向量长度的存储器访问。
·第三个部分为操作的对象名及数据类型，_ps表示操作向量中所有的单精度数据；_pd表示操作向量中所有的双精度数据；_pixx表示操作向量中所有的xx位的有符号整型数据，向量寄存器长度为64位；_epixx表示操作向量中所有的xx位的有符号整型数据，向量寄存器长度为128位；_epuxx表示操作向量中所有的xx位的无符号整型数据，向量寄存器长度为128位；_ss表示只操作向量中第一个单精度数据；si128表示操作向量寄存器中的第一个128位有符号整型

float sum(float *data, size_t N) {
  __m512d counter = _mm512_setzero_pd();
  for (size_t i = 0; i < N; i += 16) {
    __m512 v = _mm512_loadu_ps((__m512 *)&data[i]);
    __m512d part1 = _mm512_cvtps_pd(_mm512_extractf32x8_ps(v, 0));
    __m512d part2 = _mm512_cvtps_pd(_mm512_extractf32x8_ps(v, 1));
    counter = _mm512_add_pd(counter, part1);
    counter = _mm512_add_pd(counter, part2);
  }
  double sum = _mm512_reduce_add_pd(counter);
  for (size_t i = N / 16 * 16; i < N; i++) {
    sum += data[i];
  }
  return sum;
}


double compute_pi_naive(size_t dt) // version 1
{
    double pi = 0.0;
    double delta = 1.0 / dt;
    for (size_t i = 0; i < dt; i++)
    {
        double x = (double)i / dt;
        pi += delta / (1.0 + x * x);
    }
    return pi * 4.0;
}

double compute_pi_omp_avx(size_t dt)
{
    double pi = 0.0;
    double delta = 1.0 / dt;
    __m256d ymm0, ymm1, ymm2, ymm3, ymm4;
    ymm0 = _mm256_set1_pd(1.0);
    ymm1 = _mm256_set1_pd(delta);
    ymm2 = _mm256_set_pd(delta * 3, delta * 2, delta, 0.0);
    ymm4 = _mm256_setzero_pd();
    for (int i = 0; i <= dt - 4; i += 4)
    {
        ymm3 = _mm256_set1_pd(i * delta);
        ymm3 = _mm256_add_pd(ymm3, ymm2);
        ymm3 = _mm256_mul_pd(ymm3, ymm3);
        ymm3 = _mm256_add_pd(ymm0, ymm3);
        ymm3 = _mm256_div_pd(ymm1, ymm3);
        ymm4 = _mm256_add_pd(ymm4, ymm3);
    }
    double tmp[4] __attribute__((aligned(32)));
    _mm256_store_pd(tmp, ymm4);
    pi += tmp[0] + tmp[1] + tmp[2] + tmp[3];
    return pi * 4.0;
}


using System.Numerics;  // 引入System.Numerics命名空间以使用Vector类

class Program {  
  static void Main(string[] args) {  
    int[] buffer = new int[1000000];  // 创建一个包含100万个整数的数组  
    for (int i = 0; i < buffer.Length; i++) {  
      buffer[i] = i + 1;  // 初始化数组  
    }  
    MultiplyEachSIMD(buffer, 2);  // 调用MultiplyEachSIMD方法，将每个元素乘以2  
    // 输出前10个元素以验证结果  
    for (int i = 0; i < 10; i++) {  
      Console.WriteLine(buffer[i]);  // 输出结果  
    }  
  }  

  // 定义MultiplyEachSIMD方法，使用SIMD加速对数组中的每个元素进行乘法操作  
static void MultiplyEachSIMD(int[] buffer, int value) { 
    int length = buffer.Length;  // 获取数组长度  
    int i = 0;  // 初始化索引  
    // 使用Vector类进行SIMD操作  
    Vector<int> vectorValue = new Vector<int>(value);  // 创建一个包含value的Vector  
    // 使用循环处理数组中的元素  
    for (; i <= length - Vector<int>.Count; i += Vector<int>.Count) {  
      Vector<int> vectorBuffer = new Vector<int>(buffer, i);  // 从数组中加载一个Vector  
      vectorBuffer *= vectorValue;  // 将Vector中的每个元素乘以value  
      vectorBuffer.CopyTo(buffer, i);  // 将结果存回数组  
    }  
    // 处理剩余的元素（如果有）  
    for (; i < length; i++) {  
      buffer[i] *= value;  // 对剩余的元素进行普通乘法操作  
    }  
  }
}
---
float dot512fma(float *x1, float *x2, size_t length) {
  // create a vector of 16 32-bit floats (zeroed)
  __m512 sum = _mm512_setzero_ps();
  size_t i = 0;
  for (; i + 16 <= length; i+=16) {
    // load 16 32-bit floats
    __m512 v1 = _mm512_loadu_ps(x1 + i);
    // load 16 32-bit floats
    __m512 v2 = _mm512_loadu_ps(x2 + i);
    // do sum[0] += v1[i]*v2[i] (fused multiply-add)
    sum = _mm512_fmadd_ps(v1, v2, sum);
  }
  if  (i  < length) {
    // load 16 32-bit floats, load only the first length-i floats
    // other floats are automatically set to zero
    __m512 v1 = _mm512_maskz_loadu_ps((1<<(length-i))-1, x1 + i);
    // load 16 32-bit floats, load only the first length-i floats
    __m512 v2 = _mm512_maskz_loadu_ps((1<<(length-i))-1, x2 + i);
    // do sum[0] += v1[i]*v2[i] (fused multiply-add)
    sum = _mm512_fmadd_ps(v1, v2, sum);
  }
  // reduce: sums all elements
  return _mm512_reduce_add_ps(sum);
}


---cpp simd using highway



wget https://raw.githubusercontent.com/llvm-mirror/test-suite/master/SingleSource/Benchmarks/Shootout/sieve.c

gcc sieve.c -O3 -o sieve

perf stat -e branches,branch-misses,cache-references,cache-misses,cycles,instructions,idle-cycles-backend,idle-cycles-frontend,task-clock ./sieve


用bool 做index的jump table

```c++
term[2] = {expr1, epxr2};
sum += term[bool(cond)];
```


---csharp /Users/boya/sites/tmp/csharpconsole
int Add(int a, int b)
{
    Span<int> numbers = stackalloc int[2];
    numbers[0] = a;
    numbers[1] = b;
    // Using the numbers span to perform addition
    // This is just an example, in practice you would use the span for more complex operations
    return numbers[0] + numbers[1];
}
---nvida的 gpu 每次调度32 个线程到一个 warp 上运行
aws 导入的时候用export name


class Animal { val sound = "rustle" }
class Bird extends Animal { override val sound = "call" }
class Chicken extends Bird { override val sound = "cluck" }
contravariant:
val getTweet: (Bird => String) = ((a: Animal) => a.sound )

kt: 
interface Comparable<in T> {
    operator fun compareTo(other: T): Int
}

fun demo(x: Comparable<Number>) {
    x.compareTo(1.0) // 1.0 has type Double, which is a subtype of Number
    // Thus, you can assign x to a variable of type Comparable<Double>
    val y: Comparable<Double> = x // OK!
}
covariant:
scala:
val hatch: (() => Bird) = (() => new Chicken )
kt:
interface Source<out T> {
    fun nextT(): T
}

fun demo(strs: Source<String>) {
    val objects: Source<Any> = strs // This is OK, since T is an out-parameter
    // ...
}
---rust
struct City<'a> {
    name: &'a str,
    population: u32,
    country: String,
}
it will only take a reference for name if  it lives as long as city


 SFINAE 设计的最初用法：如果模板实例化中发生了失败，没有理由编译就此出错终止，因为还是可能有其他可用的函数重载的。很快人们就发现 SFINAE 可以用于其他用途。比如，根据某个实例化的成功或失败来在编译期检测类的特性。
任何组织都是一样的。并不存在一个统一意识体，只有一个个局部的 KPI。
今天看到一篇文章，讲了企业里风控和营销之间的矛盾。风控要打击羊毛党，而营销则欢迎羊毛党。为什么呢？因为羊毛党刷出来的数据一样算营销的业绩。风控打羊毛党，就是打营销的 KPI。所以营销恨风控，恨之入骨。

微博上有一些金 V 的号，明显是靠僵尸粉+刷转发做起来专门用于接广告的。所以，向这种号投放广告实际收益极小。因为转发量是假的。按理说，这种号并不难识别。但为什么还会有公司找他们投广告呢？因为假的转发量也是转发量，是可以做成折线图放到 PPT 上的业绩。

那篇文章中说：“毕竟 KPI 和年终奖是自己的，亏损是公司的，岂不美哉？”]

Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy Re moteSigned -Force
This execution policy allows us to run unsigned PowerShell scripts if they’re created locally but will not allow us to execute unsigned scripts downloaded in a web browser or attached to emails.

EYAN-193 Aina Namiki

559235-8948
2024-05-01 - 2025-04-30
22831+2250=25081

Alpaca Data API - Unlimited access to real-time US stock market data . 访问美股实时交易数据的 API 服务



abb/fe7b43f9-a8a7-4638-a67a-32f1722c4fbc(parent) 3727a17d-71c4-43aa-93c6-48919978244a scheduledTime="2025-07-22T14:21:13Z
115cmx240cm

 I’m passionate about leveraging data to create hyper-personalized and automated customer journeys. I look forward to contributing my expertise in CDP-driven marketing automation to
 connect, model and make sense of your data

Marknadsvägen 289,Täby, Stockholm
xialiu 8707124742
Guangwei Yang20150723-1478
Jiatong Yang20220413-7026
Felix Yang20240627-0674
Insurance number: DTR376, 0046254515
Folktandvården



Byggmax Bauhaus K-rauta Hornbach XL-BYGG Beijer Byggmaterial Woody Bygghandel

https://u.geekbang.org/subject/rust/1007544

疏肝益阳胶囊的成分比较复杂，它们分别是蒺藜、柴胡、蜂房、地龙、水蛭、九香虫、紫梢花、蛇床子、远志、肉苁蓉、菟丝子、五味子、巴戟天、蜈蚣、石菖蒲。
麒麟丸（淫羊藿、菟丝子、锁阳、墨早莲、桑葚、枸杞子、覆盆子、党参、山药、黄芪、白芍、青皮、丹参、郁金、制何首
Ashwagandha
testosteron
Ginseng
oyster
https://www.apotea.se/biosalma-testolibido-120-kapslar

https://www.markdownguide.org/basic-syntax/

708898441
shahab
mokarizadeh
phone ending 3268
payment: paypal - shahab.mokari@gmail.com

10 dgr vid barns födelse   孩子出生后10天
Arbetstidsförkortning   减少工作时间
Föräldraledighet   育婴假
Semester   假期
Sjuk   患病的
Tjänstledighet   请假
VAB   VAB
Vård av närstående   亲戚的照顾
Övrig frånvaro med lön   其他带薪缺勤
Övrig frånvaro utan lön   其他无薪缺勤

Aina Namiki

ngrok:?
MN3KQ5YAPX
A63TW2CBGD
6KT3SHYE4C
MRT4QAB6WW
D8FBH7KGD6
KPFPNZF8NC
DJH8Z372CK
A4VC5BZ9V2
HWGHT7BNWA
6HVDB3B4CR


ln -s /path/to/original /path/to/link

8327 99447039257
708898441
#Mo21vzAaPm78Jek
bookszlibb74ugqojhzhg2a63w5i2atv5bqarulgczawnbmsb6s6qead.onion
https://my.freenom.com/ libwy.tk ga ml
name.com libwy.site

"AIDAV5XTZ2J4BFOKUGC5X",
ANS7_9?AW8#5wLT
reishi@202111250236

/opt/homebrew/Cellar/awscli/2.13.15/share/awscli/examples/
/Users/boya/projects/itblog/ansible-playbooks/inventory
docker run --name=mysql58 --env="MYSQL_ROOT_PASSWORD=123456" --publish 3310:3306 --volume=/Users/boya/program/mysql58/data:/var/lib/mysql --detach mysql/mysql-server:latest

代码里面用到的std::execution::par, Apple Clang不支持C++17标准的 Parallel algorithms and execution policies, github.com/xiexiexx/PPLA/blob/main/billionsort/parallel_billionsort.cpp

今天我们分析了一个商品搜索的应用程序。我们先是通过 top、iostat 分析了系统的 CPU 和磁盘使用情况，发现了磁盘的 I/O 瓶颈。

接着，我们借助 pidstat ，发现瓶颈是 mysqld 导致的。紧接着，我们又通过 strace、lsof，找出了 mysqld 正在读的文件。同时，根据文件的名字和路径，我们找出了 mysqld 正在操作的数据库和数据表。综合这些信息，我们判断，这是一个没有利用索引导致的慢查询问题。

于是，我们登录到 MySQL 命令行终端，用数据库分析工具进行验证，发现 MySQL 查询语句访问的字段，果然没有索引。所以，增加索引，就可以解决案例的性能问题了。


一场电影就能轻易激起的意志力，会被一罐可乐轻松的浇灭掉，如果不够，再加一根鸡翅。
虚函数表 「c现代编程」3.2.7
汇编和体系结构 「自制编译器」

GCC的__asm__标签来编写内联汇编代码。
boost compute


SRM: security reference monitoring


tracking and report
	bounce
	open
	click
smtp server
layout builder 
editor middleware
email api
audience profile

gpu

compgain
dkim

meta
consent
one api
ma
activity
reports

FSDSS-070


info source -basename /home/boya/huadb/*
info break
delete 1
info frame

list 查看文件内容

algorithm
database
security/qt/windows/hpc/cuda/rust
IBM Planning Analytics
kdb in uk


web3 ntf defi dao 
	avalanche
	EIP-721为例（721就是提案编号，NFT是提案的名称）
	Polkadot
	solana 手机加密货币钱包
	Substrate
	区块链应用(协议) https://github.com/AudiusProject/audius-protocol


297-实现一个简 单 的 数 据 包 过 滤 器
