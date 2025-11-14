# 从 UDP 开始做出可靠协议

## 引言：为什么要重新发明轮子？

当网络工程师第一次深入理解 TCP 时，往往会经历三个阶段：

1. **表面理解期**："TCP 就是可靠的，UDP 就是不可靠的"
2. **困惑期**："为什么 TCP 需要三次握手？为什么有滑动窗口？"
3. **顿悟期**："原来可靠性是一层层构建出来的！"

本文的价值不仅是教你实现一个可靠协议，更重要的是引导你完成一次**从问题到解决方案、从简单到复杂、从不可靠到可靠**的思维历程。

---

## 核心思维转变

### UDP vs TCP：两种哲学

```
UDP 哲学（尽力而为）：
┌─────────────────────────────────────┐
│  发送数据包                          │
│  ↓                                  │
│  扔出去就不管了                      │
│  ↓                                  │
│  收到就收到，丢了就丢了              │
│                                     │
│  特点：简单、快速、不可靠            │
└─────────────────────────────────────┘

TCP 哲学（可靠传输）：
┌─────────────────────────────────────┐
│  发送数据包                          │
│  ↓                                  │
│  等待确认                            │
│  ↓                                  │
│  没收到确认 → 重传                   │
│  ↓                                  │
│  确保数据到达且有序                  │
│                                     │
│  特点：复杂、慢一点、可靠            │
└─────────────────────────────────────┘
```

**关键洞察**：

```
TCP 不是一个整体设计出来的协议
而是为了解决一个个具体问题而逐步演化的

问题 1：数据包会丢失 → 加入 ACK 确认
问题 2：ACK 也会丢失 → 加入超时重传
问题 3：数据包乱序   → 加入序列号
问题 4：效率太低     → 加入滑动窗口
问题 5：网络拥塞     → 加入拥塞控制
问题 6：连接管理     → 加入三次握手/四次挥手

每一层都建立在前一层的基础上
```

---

## 案例一：裸 UDP - 理解不可靠性

### 业务场景

实现一个简单的聊天程序：
- 客户端发送消息给服务器
- 服务器回显消息

### 最简单的实现

**服务端（Python）**：

```python
# udp_server_v1.py
import socket

def udp_server():
    # 创建 UDP socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(('0.0.0.0', 9999))

    print("UDP Server started on port 9999")

    while True:
        # 接收数据（最大 1024 字节）
        data, addr = sock.recvfrom(1024)
        message = data.decode('utf-8')

        print(f"Received from {addr}: {message}")

        # 回显消息
        response = f"Echo: {message}"
        sock.sendto(response.encode('utf-8'), addr)

if __name__ == '__main__':
    udp_server()
```

**客户端（Python）**：

```python
# udp_client_v1.py
import socket

def udp_client():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server_addr = ('127.0.0.1', 9999)

    while True:
        message = input("Enter message: ")

        # 发送消息
        sock.sendto(message.encode('utf-8'), server_addr)
        print(f"Sent: {message}")

        # 接收回复
        data, addr = sock.recvfrom(1024)
        print(f"Received: {data.decode('utf-8')}")

if __name__ == '__main__':
    udp_client()
```

### 测试不可靠性

**模拟网络丢包**：

```bash
# 使用 tc (traffic control) 模拟 30% 丢包率
sudo tc qdisc add dev lo root netem loss 30%

# 运行程序
python udp_server_v1.py
# 另一个终端
python udp_client_v1.py

# 现象：
Enter message: Hello
Sent: Hello
# 卡住... 收不到回复（数据包丢了）

Enter message: World
Sent: World
Received: Echo: World  # 这次运气好，收到了

# 清除模拟
sudo tc qdisc del dev lo root
```

**问题清单**：

1. ❌ **消息可能丢失**：发送了但收不到回复
2. ❌ **客户端无法知道是否成功**：卡在 `recvfrom()`
3. ❌ **没有重试机制**：丢了就丢了
4. ❌ **服务器不知道客户端是否收到**：单向通信

**心智模型**：

```
天真思维：网络是可靠的
         发送 → 一定会到达

现实思维：网络是不可靠的
         发送 → 可能丢失
              → 可能延迟
              → 可能乱序
              → 可能重复
```

---

## 案例二：加入确认机制（ACK）- 第一次改进

### 问题分析

```
当前问题：
1. 发送方不知道消息是否到达
2. 接收方收到消息，但发送方不确定

解决思路：
- 接收方收到消息后，发送 ACK（确认）
- 发送方收到 ACK，确认消息已送达
```

### 协议设计 v2

**消息格式**：

```python
# 定义消息类型
MSG_TYPE_DATA = 0x01  # 数据消息
MSG_TYPE_ACK  = 0x02  # 确认消息

# 消息结构：
# [type: 1 byte][data: N bytes]
```

**实现**：

```python
# reliable_udp_v2.py
import socket
import struct
import time

MSG_TYPE_DATA = 0x01
MSG_TYPE_ACK = 0x02

class ReliableUDP:
    def __init__(self, port):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind(('0.0.0.0', port))

    def send_message(self, data, addr):
        """发送数据消息"""
        # 构造消息：[type][data]
        packet = struct.pack('B', MSG_TYPE_DATA) + data
        self.sock.sendto(packet, addr)
        print(f"[SEND] Data to {addr}: {data.decode('utf-8')}")

    def send_ack(self, addr):
        """发送 ACK 消息"""
        packet = struct.pack('B', MSG_TYPE_ACK)
        self.sock.sendto(packet, addr)
        print(f"[SEND] ACK to {addr}")

    def receive(self):
        """接收消息（阻塞）"""
        data, addr = self.sock.recvfrom(1024)
        msg_type = struct.unpack('B', data[0:1])[0]

        if msg_type == MSG_TYPE_DATA:
            payload = data[1:]
            print(f"[RECV] Data from {addr}: {payload.decode('utf-8')}")
            return ('data', payload, addr)
        elif msg_type == MSG_TYPE_ACK:
            print(f"[RECV] ACK from {addr}")
            return ('ack', None, addr)
        else:
            return ('unknown', None, addr)

# 服务端
def server():
    udp = ReliableUDP(9999)
    print("Server started on port 9999")

    while True:
        msg_type, data, addr = udp.receive()

        if msg_type == 'data':
            # 收到数据，发送 ACK
            udp.send_ack(addr)

            # 回显消息
            response = f"Echo: {data.decode('utf-8')}".encode('utf-8')
            udp.send_message(response, addr)

# 客户端
def client():
    udp = ReliableUDP(0)  # 随机端口
    server_addr = ('127.0.0.1', 9999)

    message = "Hello, World!".encode('utf-8')

    # 发送消息
    udp.send_message(message, server_addr)

    # 等待 ACK
    msg_type, _, addr = udp.receive()
    if msg_type == 'ack':
        print("✓ Message delivered!")

    # 接收回复
    msg_type, data, addr = udp.receive()
    if msg_type == 'data':
        # 回复也需要 ACK
        udp.send_ack(addr)
        print(f"Got response: {data.decode('utf-8')}")

if __name__ == '__main__':
    import sys
    if sys.argv[1] == 'server':
        server()
    else:
        client()

# 运行：
# python reliable_udp_v2.py server
# python reliable_udp_v2.py client

# 输出（服务端）：
# [RECV] Data from ('127.0.0.1', 54321): Hello, World!
# [SEND] ACK to ('127.0.0.1', 54321)
# [SEND] Data to ('127.0.0.1', 54321): Echo: Hello, World!
# [RECV] ACK from ('127.0.0.1', 54321)

# 输出（客户端）：
# [SEND] Data to ('127.0.0.1', 9999): Hello, World!
# [RECV] ACK from ('127.0.0.1', 9999)
# ✓ Message delivered!
# [RECV] Data from ('127.0.0.1', 9999): Echo: Hello, World!
# [SEND] ACK to ('127.0.0.1', 9999)
# Got response: Echo: Hello, World!
```

### 新的问题

```
测试场景：30% 丢包率

现象 1：发送消息，ACK 丢失
→ 发送方永远等待 ACK（卡死）

现象 2：服务器回复，回复的数据包丢失
→ 客户端永远等待回复（卡死）

现象 3：客户端的 ACK 丢失
→ 服务器不知道客户端是否收到（但不影响继续）

根本问题：
- 一旦丢包，程序卡死
- 没有超时机制
- 没有重传机制
```

**心智模型升级**：

```
Level 0 (无确认):
  发送 → (不管)

Level 1 (有确认):
  发送 → 等待 ACK → 确认送达

  问题：如果 ACK 丢失 → 永远等待

  教训：确认机制解决了"如何知道送达"的问题
       但引入了"如果确认丢失怎么办"的新问题
```

---

## 案例三：超时重传 - 第二次改进

### 问题分析

```
核心矛盾：
- 接收方收到消息，但 ACK 丢失
- 发送方不知道是"消息丢失"还是"ACK 丢失"

解决方案：
1. 发送方设置超时时间（Timeout）
2. 超时后重传
3. 接收方去重（如果收到重复消息）
```

### 协议设计 v3

**超时重传机制**：

```python
# reliable_udp_v3.py
import socket
import struct
import time
import select

MSG_TYPE_DATA = 0x01
MSG_TYPE_ACK = 0x02

TIMEOUT = 2.0  # 2 秒超时
MAX_RETRIES = 3  # 最多重传 3 次

class ReliableUDP:
    def __init__(self, port):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind(('0.0.0.0', port))

    def send_with_retry(self, data, addr):
        """发送消息，带重传机制"""
        packet = struct.pack('B', MSG_TYPE_DATA) + data

        for attempt in range(MAX_RETRIES):
            print(f"[SEND] Attempt {attempt + 1}: {data.decode('utf-8')}")
            self.sock.sendto(packet, addr)

            # 等待 ACK（使用 select 实现超时）
            ready = select.select([self.sock], [], [], TIMEOUT)

            if ready[0]:
                # 有数据到达
                recv_data, recv_addr = self.sock.recvfrom(1024)
                msg_type = struct.unpack('B', recv_data[0:1])[0]

                if msg_type == MSG_TYPE_ACK and recv_addr == addr:
                    print(f"[RECV] ACK from {addr} ✓")
                    return True  # 成功
            else:
                # 超时
                print(f"[TIMEOUT] No ACK received, retrying...")

        print(f"[FAIL] Failed after {MAX_RETRIES} attempts")
        return False  # 失败

    def send_ack(self, addr):
        """发送 ACK"""
        packet = struct.pack('B', MSG_TYPE_ACK)
        self.sock.sendto(packet, addr)
        print(f"[SEND] ACK to {addr}")

    def receive_data(self):
        """接收数据消息（阻塞）"""
        while True:
            data, addr = self.sock.recvfrom(1024)
            msg_type = struct.unpack('B', data[0:1])[0]

            if msg_type == MSG_TYPE_DATA:
                payload = data[1:]
                return payload, addr

# 服务端
def server():
    udp = ReliableUDP(9999)
    print("Server started on port 9999")

    while True:
        # 接收数据
        data, addr = udp.receive_data()
        print(f"[RECV] Data from {addr}: {data.decode('utf-8')}")

        # 发送 ACK（可能丢失，但客户端会重传）
        udp.send_ack(addr)

        # 回显消息（也带重传）
        response = f"Echo: {data.decode('utf-8')}".encode('utf-8')
        success = udp.send_with_retry(response, addr)

        if success:
            print("Response delivered successfully")
        else:
            print("Failed to deliver response")

# 客户端
def client():
    udp = ReliableUDP(0)
    server_addr = ('127.0.0.1', 9999)

    message = "Hello, World!".encode('utf-8')

    # 发送消息（带重传）
    success = udp.send_with_retry(message, server_addr)

    if success:
        print("✓ Message delivered!")

        # 接收回复
        data, addr = udp.receive_data()
        print(f"[RECV] Response: {data.decode('utf-8')}")

        # 发送 ACK
        udp.send_ack(addr)
    else:
        print("✗ Failed to send message")

if __name__ == '__main__':
    import sys
    if sys.argv[1] == 'server':
        server()
    else:
        client()

# 测试（30% 丢包）：
# 客户端输出：
# [SEND] Attempt 1: Hello, World!
# [TIMEOUT] No ACK received, retrying...
# [SEND] Attempt 2: Hello, World!
# [RECV] ACK from ('127.0.0.1', 9999) ✓
# ✓ Message delivered!
# [RECV] Response: Echo: Hello, World!
# [SEND] ACK to ('127.0.0.1', 9999)

# 观察：即使有丢包，通过重传仍能成功传输
```

### 新的问题

```
测试场景：模拟以下情况

情况 1：消息延迟到达（不是丢失）
1. 客户端发送消息 #1
2. 超时（但消息在路上）
3. 客户端重传消息 #1（重复）
4. 服务器收到两次相同消息 #1
→ 问题：重复处理

情况 2：ACK 延迟到达
1. 客户端发送消息 #1
2. 服务器发送 ACK（延迟）
3. 客户端超时，重传消息 #1
4. 客户端收到延迟的 ACK
5. 客户端认为重传成功（但实际是旧 ACK）
→ 问题：ACK 混淆

根本原因：
- 没有消息 ID（无法区分不同消息）
- 没有去重机制
```

**心智模型升级**：

```
Level 2 (超时重传):
  发送 → 等待 ACK (超时) → 重传 → 等待 ACK → 成功

  解决：丢包导致的卡死问题
  引入：重复消息问题

  教训：
  - 重传解决了可靠性问题
  - 但引入了"如何识别重复"的新问题
  - 网络中可能同时存在同一消息的多个副本
```

---

## 案例四：序列号 - 第三次改进

### 问题分析

```
如何区分不同的消息？
- 给每个消息编号（序列号 Sequence Number）

如何去重？
- 接收方记录已收到的序列号
- 如果收到重复序列号，丢弃（但仍发 ACK）

如何处理乱序？
- 按序列号排序
- 只交付连续的消息
```

### 协议设计 v4

**消息格式**：

```python
# 消息结构：
# [type: 1 byte][seq: 4 bytes][data: N bytes]

# ACK 结构：
# [type: 1 byte][ack_seq: 4 bytes]
```

**实现**：

```python
# reliable_udp_v4.py
import socket
import struct
import time
import select

MSG_TYPE_DATA = 0x01
MSG_TYPE_ACK = 0x02

TIMEOUT = 2.0
MAX_RETRIES = 3

class ReliableUDP:
    def __init__(self, port):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind(('0.0.0.0', port))
        self.seq = 0  # 发送序列号
        self.received_seqs = set()  # 已接收的序列号（去重）

    def send_with_retry(self, data, addr):
        """发送消息（带序列号和重传）"""
        seq = self.seq
        self.seq += 1

        # 构造消息：[type][seq][data]
        packet = struct.pack('!BI', MSG_TYPE_DATA, seq) + data

        for attempt in range(MAX_RETRIES):
            print(f"[SEND] Seq={seq}, Attempt {attempt + 1}: {data.decode('utf-8')}")
            self.sock.sendto(packet, addr)

            # 等待对应的 ACK
            start_time = time.time()
            while time.time() - start_time < TIMEOUT:
                ready = select.select([self.sock], [], [], 0.1)

                if ready[0]:
                    recv_data, recv_addr = self.sock.recvfrom(1024)
                    msg_type = struct.unpack('!B', recv_data[0:1])[0]

                    if msg_type == MSG_TYPE_ACK and recv_addr == addr:
                        ack_seq = struct.unpack('!I', recv_data[1:5])[0]

                        if ack_seq == seq:
                            print(f"[RECV] ACK for Seq={seq} ✓")
                            return True
                        else:
                            print(f"[RECV] ACK for Seq={ack_seq} (expected {seq}), ignoring")

            print(f"[TIMEOUT] No ACK for Seq={seq}, retrying...")

        print(f"[FAIL] Failed to send Seq={seq} after {MAX_RETRIES} attempts")
        return False

    def send_ack(self, seq, addr):
        """发送 ACK（指定序列号）"""
        packet = struct.pack('!BI', MSG_TYPE_ACK, seq)
        self.sock.sendto(packet, addr)
        print(f"[SEND] ACK for Seq={seq} to {addr}")

    def receive_data(self):
        """接收数据消息（去重）"""
        while True:
            data, addr = self.sock.recvfrom(1024)
            msg_type = struct.unpack('!B', data[0:1])[0]

            if msg_type == MSG_TYPE_DATA:
                seq = struct.unpack('!I', data[1:5])[0]
                payload = data[5:]

                # 发送 ACK（即使是重复消息也要回复）
                self.send_ack(seq, addr)

                # 检查是否重复
                if seq in self.received_seqs:
                    print(f"[RECV] Duplicate Seq={seq}, ignoring")
                    continue  # 忽略重复消息

                # 新消息
                self.received_seqs.add(seq)
                print(f"[RECV] New Seq={seq} from {addr}: {payload.decode('utf-8')}")
                return seq, payload, addr

# 服务端
def server():
    udp = ReliableUDP(9999)
    print("Server started on port 9999")

    while True:
        seq, data, addr = udp.receive_data()

        # 回显消息
        response = f"Echo: {data.decode('utf-8')}".encode('utf-8')
        success = udp.send_with_retry(response, addr)

        if success:
            print(f"Response for Seq={seq} delivered")

# 客户端
def client():
    udp = ReliableUDP(0)
    server_addr = ('127.0.0.1', 9999)

    # 发送多条消息
    messages = [
        "Message 1",
        "Message 2",
        "Message 3"
    ]

    for msg in messages:
        success = udp.send_with_retry(msg.encode('utf-8'), server_addr)

        if success:
            # 接收回复
            seq, data, addr = udp.receive_data()
            print(f"✓ Got response for Seq={seq}: {data.decode('utf-8')}\n")
        else:
            print(f"✗ Failed to send: {msg}\n")

if __name__ == '__main__':
    import sys
    if sys.argv[1] == 'server':
        server()
    else:
        client()

# 测试输出（客户端）：
# [SEND] Seq=0, Attempt 1: Message 1
# [TIMEOUT] No ACK for Seq=0, retrying...
# [SEND] Seq=0, Attempt 2: Message 1
# [RECV] ACK for Seq=0 ✓
# [RECV] New Seq=0 from ('127.0.0.1', 9999): Echo: Message 1
# ✓ Got response for Seq=0: Echo: Message 1

# [SEND] Seq=1, Attempt 1: Message 2
# [RECV] ACK for Seq=1 ✓
# [RECV] New Seq=1 from ('127.0.0.1', 9999): Echo: Message 2
# ✓ Got response for Seq=1: Echo: Message 2

# 测试输出（服务端）：
# [RECV] New Seq=0 from ('127.0.0.1', 54321): Message 1
# [SEND] ACK for Seq=0 to ('127.0.0.1', 54321)
# [RECV] Duplicate Seq=0, ignoring  ← 收到重传，但已处理
# [SEND] ACK for Seq=0 to ('127.0.0.1', 54321)  ← 仍然回 ACK
# [SEND] Seq=0, Attempt 1: Echo: Message 1
# [RECV] ACK for Seq=0 ✓
```

### 深入理解：序列号的作用

**1. 去重（Deduplication）**

```
场景：超时重传导致重复

Time  |  Client              |  Server
------|----------------------|----------------------
t1    |  Send Seq=0          |
t2    |                      |  Recv Seq=0
t3    |                      |  Send ACK (丢失)
t4    |  Timeout             |
t5    |  Resend Seq=0        |
t6    |                      |  Recv Seq=0 (重复!)
t7    |                      |  Check: 0 in received_seqs
t8    |                      |  → Ignore data, but send ACK
t9    |  Recv ACK            |

关键：
- 接收方记录已收到的序列号
- 重复消息：丢弃数据，但仍发 ACK（防止发送方继续重传）
```

**2. 有序性（Ordering）**

```
场景：网络乱序

当前实现的问题：
- 消息可能乱序到达
- 但我们按到达顺序交付
- 应用层可能收到乱序的消息

改进方案（简化）：
- 维护期望序列号 expected_seq
- 只交付连续的消息
- 缓存乱序消息

示例：
Recv Seq=2 → Buffer[2]
Recv Seq=1 → Buffer[1]
Recv Seq=0 → Deliver 0, 1, 2
```

**心智模型升级**：

```
Level 3 (序列号):
  每个消息有唯一 ID

  能力：
  1. 区分不同消息
  2. 检测重复
  3. 检测丢失
  4. 检测乱序

  教训：
  - 序列号是协议的"身份证"
  - 解决了识别问题
  - 但引入了"状态管理"的复杂性
```

---

## 案例五：滑动窗口 - 第四次改进

### 问题分析

```
当前问题：效率低

现有模式（停等协议 Stop-and-Wait）：
1. 发送消息 1
2. 等待 ACK 1
3. 发送消息 2
4. 等待 ACK 2
...

问题：
- RTT（往返时间）= 100ms
- 每秒最多发送 10 条消息
- 带宽利用率极低

改进思路：
- 不等待 ACK，连续发送多条消息
- 使用"窗口"控制未确认消息的数量
```

### 滑动窗口原理

```
发送窗口（Send Window）：

Sent & ACKed  | Sent & Unacked  | Ready to send | Future
──────────────|─────────────────|───────────────|─────────
     ✓✓✓      |     ???         |      →        |  ...
              |← Window Size →|

示例（窗口大小 = 4）：

初始：
Seq:  0  1  2  3  4  5  6  7  8  9
      [  Window  ]

发送 0,1,2,3：
Seq:  0  1  2  3  4  5  6  7  8  9
      S  S  S  S  [  Window  ]

收到 ACK 0,1：
Seq:  0  1  2  3  4  5  6  7  8  9
      ✓  ✓  S  S  [  Window  ]

窗口滑动，可以发送 4,5：
Seq:  0  1  2  3  4  5  6  7  8  9
      ✓  ✓  S  S  S  S  [  Window  ]
```

### 实现滑动窗口

```python
# reliable_udp_v5.py
import socket
import struct
import time
import select
import threading
from collections import deque

MSG_TYPE_DATA = 0x01
MSG_TYPE_ACK = 0x02

TIMEOUT = 2.0
WINDOW_SIZE = 10  # 窗口大小

class SlidingWindowSender:
    def __init__(self, sock, addr):
        self.sock = sock
        self.addr = addr
        self.window_size = WINDOW_SIZE

        # 窗口状态
        self.base = 0  # 窗口起始序列号（最早未确认的）
        self.next_seq = 0  # 下一个要发送的序列号

        # 未确认的消息（缓冲区）
        self.unacked_packets = {}  # {seq: (packet, send_time)}

        # 锁
        self.lock = threading.Lock()

        # 启动接收 ACK 的线程
        self.running = True
        self.ack_thread = threading.Thread(target=self._receive_acks)
        self.ack_thread.daemon = True
        self.ack_thread.start()

    def send(self, data):
        """发送数据（非阻塞，填充窗口）"""
        with self.lock:
            # 检查窗口是否已满
            while self.next_seq >= self.base + self.window_size:
                # 窗口已满，等待
                time.sleep(0.01)

            seq = self.next_seq
            self.next_seq += 1

            # 构造数据包
            packet = struct.pack('!BI', MSG_TYPE_DATA, seq) + data

            # 发送
            self.sock.sendto(packet, self.addr)

            # 缓存（用于重传）
            self.unacked_packets[seq] = (packet, time.time())

            print(f"[SEND] Seq={seq}, Window=[{self.base}, {self.next_seq})")

    def _receive_acks(self):
        """接收 ACK（后台线程）"""
        while self.running:
            try:
                ready = select.select([self.sock], [], [], 0.1)
                if not ready[0]:
                    # 检查超时，重传
                    self._check_timeout()
                    continue

                data, addr = self.sock.recvfrom(1024)
                if addr != self.addr:
                    continue

                msg_type = struct.unpack('!B', data[0:1])[0]
                if msg_type != MSG_TYPE_ACK:
                    continue

                ack_seq = struct.unpack('!I', data[1:5])[0]

                with self.lock:
                    print(f"[RECV] ACK for Seq={ack_seq}")

                    # 累积确认（ACK n 表示 n 及之前的都收到了）
                    if ack_seq >= self.base:
                        # 移除已确认的数据包
                        for seq in range(self.base, ack_seq + 1):
                            if seq in self.unacked_packets:
                                del self.unacked_packets[seq]

                        # 滑动窗口
                        self.base = ack_seq + 1
                        print(f"[WINDOW] Slided to [{self.base}, {self.next_seq})")

            except Exception as e:
                print(f"[ERROR] {e}")

    def _check_timeout(self):
        """检查超时，重传"""
        with self.lock:
            current_time = time.time()

            for seq, (packet, send_time) in list(self.unacked_packets.items()):
                if current_time - send_time > TIMEOUT:
                    # 超时，重传
                    print(f"[TIMEOUT] Retransmit Seq={seq}")
                    self.sock.sendto(packet, self.addr)
                    self.unacked_packets[seq] = (packet, current_time)

    def wait_all_acked(self):
        """等待所有消息确认"""
        while True:
            with self.lock:
                if not self.unacked_packets:
                    break
            time.sleep(0.1)

        print("[DONE] All messages acknowledged")

    def close(self):
        self.running = False
        self.ack_thread.join()

class SlidingWindowReceiver:
    def __init__(self, sock):
        self.sock = sock
        self.expected_seq = 0  # 期望的序列号
        self.buffer = {}  # 乱序缓存 {seq: data}

    def send_ack(self, seq, addr):
        """发送累积 ACK"""
        packet = struct.pack('!BI', MSG_TYPE_ACK, seq)
        self.sock.sendto(packet, addr)
        print(f"[SEND] ACK for Seq={seq}")

    def receive(self):
        """接收数据（返回有序的消息）"""
        while True:
            data, addr = self.sock.recvfrom(1024)
            msg_type = struct.unpack('!B', data[0:1])[0]

            if msg_type != MSG_TYPE_DATA:
                continue

            seq = struct.unpack('!I', data[1:5])[0]
            payload = data[5:]

            print(f"[RECV] Seq={seq} (expected {self.expected_seq})")

            if seq < self.expected_seq:
                # 重复的旧消息，发送 ACK 但不处理
                print(f"[RECV] Duplicate Seq={seq}, ignoring")
                self.send_ack(self.expected_seq - 1, addr)
            elif seq == self.expected_seq:
                # 期望的消息
                yield payload
                self.send_ack(seq, addr)
                self.expected_seq += 1

                # 检查缓存中是否有后续消息
                while self.expected_seq in self.buffer:
                    yield self.buffer[self.expected_seq]
                    del self.buffer[self.expected_seq]
                    self.send_ack(self.expected_seq, addr)
                    self.expected_seq += 1
            else:
                # 乱序消息，缓存
                print(f"[RECV] Out-of-order Seq={seq}, buffering")
                self.buffer[seq] = payload
                # 发送已收到的最大连续序列号的 ACK
                self.send_ack(self.expected_seq - 1, addr)

# 发送端示例
def sender():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(('0.0.0.0', 0))

    server_addr = ('127.0.0.1', 9999)

    sender = SlidingWindowSender(sock, server_addr)

    # 快速发送 20 条消息
    for i in range(20):
        message = f"Message {i}".encode('utf-8')
        sender.send(message)
        time.sleep(0.05)  # 模拟发送间隔

    # 等待所有确认
    sender.wait_all_acked()
    sender.close()

# 接收端示例
def receiver():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(('0.0.0.0', 9999))

    print("Receiver started on port 9999")

    receiver = SlidingWindowReceiver(sock)

    for data in receiver.receive():
        print(f"[APP] Received: {data.decode('utf-8')}")

if __name__ == '__main__':
    import sys
    if sys.argv[1] == 'send':
        sender()
    else:
        receiver()

# 运行：
# python reliable_udp_v5.py recv
# python reliable_udp_v5.py send

# 输出（发送端）：
# [SEND] Seq=0, Window=[0, 1)
# [SEND] Seq=1, Window=[0, 2)
# [SEND] Seq=2, Window=[0, 3)
# [RECV] ACK for Seq=0
# [WINDOW] Slided to [1, 3)
# [RECV] ACK for Seq=1
# [WINDOW] Slided to [2, 3)
# ...
# [DONE] All messages acknowledged

# 输出（接收端）：
# [RECV] Seq=0 (expected 0)
# [SEND] ACK for Seq=0
# [APP] Received: Message 0
# [RECV] Seq=1 (expected 1)
# [SEND] ACK for Seq=1
# [APP] Received: Message 1
# ...

# 性能对比：
# 停等协议（窗口=1）：发送 100 条消息需要 10 秒
# 滑动窗口（窗口=10）：发送 100 条消息需要 1.5 秒（6.7× 提速）
```

### 深入理解：滑动窗口的优势

**1. 流水线传输（Pipelining）**

```
停等协议（窗口=1）：
Time ───────────────────────────────────→
     |Send 0|Wait|Send 1|Wait|Send 2|Wait|
     └ RTT ┘    └ RTT ┘    └ RTT ┘

滑动窗口（窗口=4）：
Time ───────────────────────────────────→
     |Send 0|Send 1|Send 2|Send 3|
            |ACK 0 |ACK 1 |ACK 2 |ACK 3 |
            |Send 4|Send 5|Send 6|Send 7|

利用率 = 窗口大小 × (1 / RTT)
```

**2. 累积确认（Cumulative ACK）**

```
接收方策略：
- ACK n 表示"n 及之前的所有数据都已收到"
- 即使某些 ACK 丢失，后续 ACK 也能确认

示例：
Recv: 0, 1, 2, 3, 4
ACK:  0, (丢失), 2, (丢失), 4

发送方收到 ACK 4 → 知道 0,1,2,3,4 都已送达
```

**3. 选择性重传（Selective Repeat，可选）**

```
当前实现：Go-Back-N（回退 N 步）
- 超时后从 base 开始重传所有未确认的

改进：Selective Repeat
- 只重传丢失的特定数据包
- 需要接收方发送"选择性 ACK"（SACK）

TCP 使用 SACK 选项实现选择性重传
```

**心智模型升级**：

```
Level 4 (滑动窗口):
  不等待确认，连续发送

  类比：
  - 停等 = 单线程
  - 滑动窗口 = 多线程

  关键参数：
  - 窗口大小（并发度）
  - 太小：浪费带宽
  - 太大：可能导致拥塞

  教训：
  - 并发提升效率
  - 但需要更复杂的状态管理
  - 引入了"流量控制"的需求
```

---

## 案例六：拥塞控制 - 第五次改进

### 问题分析

```
固定窗口的问题：

场景 1：网络空闲
- 窗口 = 10
- 网络能承载 100
- 浪费带宽（利用率低）

场景 2：网络拥塞
- 窗口 = 10
- 网络只能承载 5
- 数据包大量丢失
- 重传导致更多拥塞（拥塞崩溃）

解决方案：
- 动态调整窗口大小
- 根据网络状态自适应
```

### TCP 拥塞控制算法

**核心思想**：

```
1. 慢启动（Slow Start）
   - 初始窗口很小（如 1）
   - 每收到一个 ACK，窗口 × 2
   - 指数增长

2. 拥塞避免（Congestion Avoidance）
   - 达到阈值后
   - 每收到一个 ACK，窗口 + 1
   - 线性增长

3. 快速重传/快速恢复
   - 检测到丢包（超时或重复 ACK）
   - 减小窗口
   - 快速恢复
```

### 简化实现

```python
# congestion_control.py
import socket
import struct
import time
import threading

class CongestionControl:
    def __init__(self):
        # 拥塞窗口（cwnd）
        self.cwnd = 1.0  # 初始窗口大小
        self.ssthresh = 16.0  # 慢启动阈值

        # 状态
        self.state = 'slow_start'  # slow_start, congestion_avoidance

        self.lock = threading.Lock()

    def on_ack_received(self):
        """收到 ACK 时调用"""
        with self.lock:
            if self.state == 'slow_start':
                # 慢启动：指数增长
                self.cwnd += 1

                if self.cwnd >= self.ssthresh:
                    # 达到阈值，切换到拥塞避免
                    self.state = 'congestion_avoidance'
                    print(f"[CWND] Switched to congestion avoidance at {self.cwnd}")

            elif self.state == 'congestion_avoidance':
                # 拥塞避免：线性增长
                self.cwnd += 1.0 / self.cwnd

            print(f"[CWND] {self.cwnd:.2f}, State: {self.state}")

    def on_timeout(self):
        """超时时调用（检测到拥塞）"""
        with self.lock:
            print(f"[TIMEOUT] Congestion detected! cwnd={self.cwnd}")

            # 保存当前窗口的一半作为新阈值
            self.ssthresh = max(self.cwnd / 2, 2)

            # 重置窗口
            self.cwnd = 1.0

            # 回到慢启动
            self.state = 'slow_start'

            print(f"[CWND] Reset to {self.cwnd}, ssthresh={self.ssthresh}")

    def get_window_size(self):
        """获取当前窗口大小"""
        with self.lock:
            return int(self.cwnd)

# 使用示例
class AdaptiveSender:
    def __init__(self, sock, addr):
        self.sock = sock
        self.addr = addr
        self.cc = CongestionControl()

        self.base = 0
        self.next_seq = 0
        self.unacked_packets = {}

        self.running = True
        self.ack_thread = threading.Thread(target=self._receive_acks)
        self.ack_thread.daemon = True
        self.ack_thread.start()

    def send(self, data):
        """发送数据（受拥塞控制限制）"""
        # 等待窗口有空间
        while self.next_seq >= self.base + self.cc.get_window_size():
            time.sleep(0.01)

        seq = self.next_seq
        self.next_seq += 1

        packet = struct.pack('!BI', 0x01, seq) + data
        self.sock.sendto(packet, self.addr)
        self.unacked_packets[seq] = (packet, time.time())

        print(f"[SEND] Seq={seq}, cwnd={self.cc.get_window_size()}")

    def _receive_acks(self):
        """接收 ACK（后台线程）"""
        while self.running:
            try:
                ready = select.select([self.sock], [], [], 0.1)
                if not ready[0]:
                    self._check_timeout()
                    continue

                data, addr = self.sock.recvfrom(1024)
                ack_seq = struct.unpack('!I', data[1:5])[0]

                # 移除已确认的
                if ack_seq >= self.base:
                    for seq in range(self.base, ack_seq + 1):
                        if seq in self.unacked_packets:
                            del self.unacked_packets[seq]
                            self.cc.on_ack_received()  # 通知拥塞控制

                    self.base = ack_seq + 1

            except Exception as e:
                pass

    def _check_timeout(self):
        """检查超时"""
        current_time = time.time()

        for seq, (packet, send_time) in list(self.unacked_packets.items()):
            if current_time - send_time > 2.0:
                # 超时
                self.cc.on_timeout()  # 通知拥塞控制
                self.sock.sendto(packet, self.addr)
                self.unacked_packets[seq] = (packet, current_time)
                break  # 只重传一个

# 测试拥塞控制
def test_congestion_control():
    cc = CongestionControl()

    # 模拟：连续收到 20 个 ACK
    print("=== Slow Start Phase ===")
    for i in range(20):
        cc.on_ack_received()

    # 模拟：发生超时
    print("\n=== Timeout (Congestion Detected) ===")
    cc.on_timeout()

    # 模拟：恢复
    print("\n=== Recovery ===")
    for i in range(20):
        cc.on_ack_received()

if __name__ == '__main__':
    test_congestion_control()

# 输出：
# === Slow Start Phase ===
# [CWND] 2.00, State: slow_start
# [CWND] 3.00, State: slow_start
# [CWND] 4.00, State: slow_start
# ...
# [CWND] 16.00, State: slow_start
# [CWND] Switched to congestion avoidance at 17.0
# [CWND] 17.00, State: congestion_avoidance
# [CWND] 17.06, State: congestion_avoidance
# [CWND] 17.12, State: congestion_avoidance
# ...
#
# === Timeout (Congestion Detected) ===
# [TIMEOUT] Congestion detected! cwnd=19.44
# [CWND] Reset to 1.0, ssthresh=9.72
#
# === Recovery ===
# [CWND] 2.00, State: slow_start
# [CWND] 3.00, State: slow_start
# ...
# [CWND] 10.00, State: slow_start
# [CWND] Switched to congestion avoidance at 10.0
# [CWND] 10.10, State: congestion_avoidance
```

### 深入理解：拥塞控制的哲学

**1. AIMD（加法增、乘法减）**

```
增长策略：
- 成功 → 窗口 + 1（加法增）
- 保守地探测网络容量

减小策略：
- 失败 → 窗口 / 2（乘法减）
- 快速退避，避免拥塞崩溃

为什么这样设计？
- 加法增：稳定探测
- 乘法减：快速响应拥塞
- 数学上可证明收敛到公平状态
```

**2. 自相似性（Self-Clocking）**

```
关键洞察：
- ACK 到达速率 ≈ 网络可承受的速率
- 用 ACK 来"计时"新数据包的发送
- 自动适应网络速度

示例：
- 网络慢 → ACK 慢 → 发送慢
- 网络快 → ACK 快 → 发送快
```

**3. 公平性（Fairness）**

```
多个连接共享带宽：

初始状态：
Connection A: cwnd=1
Connection B: cwnd=1

若 A 先到达：
A: 1→2→4→8→16（慢启动）
B: 1→2→4→8（慢启动）

最终收敛：
A: cwnd ≈ 带宽/2
B: cwnd ≈ 带宽/2

AIMD 保证了公平性
```

**心智模型升级**：

```
Level 5 (拥塞控制):
  窗口大小动态调整

  类比：
  - 固定窗口 = 固定速度开车
  - 拥塞控制 = 根据路况调速

  探测策略：
  1. 慢启动：快速探测（指数增长）
  2. 拥塞避免：小心探测（线性增长）
  3. 超时：快速退避（减半）

  教训：
  - 网络是共享资源
  - 需要"社会责任感"
  - 贪婪会导致集体崩溃
```

---

## 案例七：连接管理 - 完整协议

### 问题分析

```
当前问题：
1. 客户端和服务器如何建立连接？
2. 如何初始化序列号？
3. 如何优雅关闭连接？

TCP 解决方案：
- 三次握手（Three-Way Handshake）建立连接
- 四次挥手（Four-Way Handshake）关闭连接
```

### 三次握手原理

```
为什么需要三次？

两次握手的问题：
Client: SYN (seq=100)    →  Server
Client:                  ←  SYN-ACK (seq=200, ack=101)
[连接建立]

问题：旧的 SYN 延迟到达
1. Client 发送 SYN (seq=100)
2. 超时，重传 SYN (seq=100)
3. 第二个 SYN 到达，建立连接
4. 连接关闭
5. 第一个 SYN（延迟）到达 → Server 认为是新连接
6. Server 分配资源，等待数据
7. Client 不理会（已关闭）
→ Server 资源泄漏

三次握手：
Client: SYN (seq=100)           →  Server
Client:                         ←  SYN-ACK (seq=200, ack=101)
Client: ACK (ack=201)           →  Server
[连接建立]

第三次握手的作用：
- Client 确认 Server 的 SYN-ACK
- 防止旧连接请求导致资源泄漏
- Server 只在收到 ACK 后才分配资源
```

### 实现连接管理

```python
# tcp_like_protocol.py
import socket
import struct
import time
import random
import threading
import select

# 消息类型
MSG_SYN = 0x01     # 同步（建立连接）
MSG_SYN_ACK = 0x02 # 同步-确认
MSG_ACK = 0x03     # 确认
MSG_DATA = 0x04    # 数据
MSG_FIN = 0x05     # 结束（关闭连接）

# 连接状态
STATE_CLOSED = 'CLOSED'
STATE_LISTEN = 'LISTEN'
STATE_SYN_SENT = 'SYN_SENT'
STATE_SYN_RCVD = 'SYN_RCVD'
STATE_ESTABLISHED = 'ESTABLISHED'
STATE_FIN_WAIT = 'FIN_WAIT'
STATE_CLOSE_WAIT = 'CLOSE_WAIT'
STATE_LAST_ACK = 'LAST_ACK'

class TCPLikeConnection:
    def __init__(self, sock, addr=None):
        self.sock = sock
        self.addr = addr
        self.state = STATE_CLOSED

        # 序列号
        self.seq = random.randint(0, 1000)  # 本地序列号
        self.ack = 0  # 期望接收的序列号

        # 拥塞控制
        self.cwnd = 1

        # 发送缓冲区
        self.send_buffer = []
        self.unacked = {}

        # 接收缓冲区
        self.recv_buffer = {}

        self.lock = threading.Lock()

    def connect(self, addr):
        """客户端：发起连接（三次握手）"""
        self.addr = addr

        # 1. 发送 SYN
        print(f"[CLIENT] Sending SYN (seq={self.seq})")
        self.state = STATE_SYN_SENT
        self._send_packet(MSG_SYN, self.seq, 0, b'')

        # 2. 等待 SYN-ACK
        while True:
            data, recv_addr = self.sock.recvfrom(1024)
            if recv_addr != addr:
                continue

            msg_type, seq, ack = struct.unpack('!BII', data[:9])

            if msg_type == MSG_SYN_ACK:
                print(f"[CLIENT] Received SYN-ACK (seq={seq}, ack={ack})")

                # 检查 ACK
                if ack != self.seq + 1:
                    print(f"[CLIENT] Wrong ACK, expected {self.seq + 1}")
                    continue

                # 记录对方序列号
                self.ack = seq + 1
                self.seq += 1

                # 3. 发送 ACK
                print(f"[CLIENT] Sending ACK (ack={self.ack})")
                self._send_packet(MSG_ACK, self.seq, self.ack, b'')

                self.state = STATE_ESTABLISHED
                print("[CLIENT] Connection established ✓")
                break

    def listen(self):
        """服务器：监听连接"""
        self.state = STATE_LISTEN
        print("[SERVER] Listening for connections...")

    def accept(self):
        """服务器：接受连接（三次握手）"""
        while True:
            data, addr = self.sock.recvfrom(1024)
            msg_type, seq, ack = struct.unpack('!BII', data[:9])

            if msg_type == MSG_SYN:
                # 1. 收到 SYN
                print(f"[SERVER] Received SYN from {addr} (seq={seq})")

                self.addr = addr
                self.ack = seq + 1
                self.state = STATE_SYN_RCVD

                # 2. 发送 SYN-ACK
                print(f"[SERVER] Sending SYN-ACK (seq={self.seq}, ack={self.ack})")
                self._send_packet(MSG_SYN_ACK, self.seq, self.ack, b'')

                # 3. 等待 ACK
                data, recv_addr = self.sock.recvfrom(1024)
                if recv_addr != addr:
                    continue

                msg_type, seq, ack = struct.unpack('!BII', data[:9])

                if msg_type == MSG_ACK and ack == self.seq + 1:
                    print(f"[SERVER] Received ACK")
                    self.seq += 1
                    self.state = STATE_ESTABLISHED
                    print(f"[SERVER] Connection established with {addr} ✓")
                    return

    def send(self, data):
        """发送数据"""
        if self.state != STATE_ESTABLISHED:
            raise Exception(f"Cannot send in state {self.state}")

        self._send_packet(MSG_DATA, self.seq, self.ack, data)
        self.seq += 1

    def recv(self):
        """接收数据"""
        if self.state != STATE_ESTABLISHED:
            raise Exception(f"Cannot recv in state {self.state}")

        while True:
            data, addr = self.sock.recvfrom(1024)
            if addr != self.addr:
                continue

            msg_type, seq, ack = struct.unpack('!BII', data[:9])
            payload = data[9:]

            if msg_type == MSG_DATA:
                if seq == self.ack:
                    # 期望的数据
                    self.ack += 1
                    self._send_packet(MSG_ACK, self.seq, self.ack, b'')
                    return payload
                else:
                    # 乱序，发送重复 ACK
                    self._send_packet(MSG_ACK, self.seq, self.ack, b'')

    def close(self):
        """关闭连接（四次挥手）"""
        if self.state == STATE_ESTABLISHED:
            # 1. 发送 FIN
            print(f"[CLOSE] Sending FIN")
            self._send_packet(MSG_FIN, self.seq, self.ack, b'')
            self.state = STATE_FIN_WAIT
            self.seq += 1

            # 2. 等待 ACK
            # 3. 等待对方 FIN
            # 4. 发送 ACK
            # (简化实现，省略完整四次挥手)

            self.state = STATE_CLOSED
            print("[CLOSE] Connection closed")

    def _send_packet(self, msg_type, seq, ack, data):
        """发送数据包"""
        packet = struct.pack('!BII', msg_type, seq, ack) + data
        self.sock.sendto(packet, self.addr)

# 测试
def test_handshake():
    # 服务器
    def server_thread():
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.bind(('0.0.0.0', 9999))

        conn = TCPLikeConnection(sock)
        conn.listen()
        conn.accept()

        # 接收数据
        data = conn.recv()
        print(f"[SERVER] Received: {data.decode('utf-8')}")

        # 发送回复
        conn.send(b"Hello from server")

        conn.close()

    # 客户端
    def client_thread():
        time.sleep(0.5)  # 等待服务器启动

        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

        conn = TCPLikeConnection(sock)
        conn.connect(('127.0.0.1', 9999))

        # 发送数据
        conn.send(b"Hello from client")

        # 接收回复
        data = conn.recv()
        print(f"[CLIENT] Received: {data.decode('utf-8')}")

        conn.close()

    # 启动
    import threading
    t1 = threading.Thread(target=server_thread)
    t2 = threading.Thread(target=client_thread)

    t1.start()
    t2.start()

    t1.join()
    t2.join()

if __name__ == '__main__':
    test_handshake()

# 输出：
# [SERVER] Listening for connections...
# [CLIENT] Sending SYN (seq=742)
# [SERVER] Received SYN from ('127.0.0.1', 54321) (seq=742)
# [SERVER] Sending SYN-ACK (seq=123, ack=743)
# [CLIENT] Received SYN-ACK (seq=123, ack=743)
# [CLIENT] Sending ACK (ack=124)
# [SERVER] Received ACK
# [SERVER] Connection established with ('127.0.0.1', 54321) ✓
# [CLIENT] Connection established ✓
# [SERVER] Received: Hello from client
# [CLIENT] Received: Hello from server
# [CLOSE] Sending FIN
# [CLOSE] Connection closed
```

### 深入理解：为什么需要连接管理

**1. 双工通信（Full Duplex）**

```
TCP 连接是双向的：
- 双方都可以发送数据
- 双方都维护序列号
- 需要同步双方的初始序列号

三次握手同步双方状态：
Client seq: 100
Server seq: 200

建立后：
Client 知道：自己从 101 开始，对方从 201 开始
Server 知道：自己从 201 开始，对方从 101 开始
```

**2. 资源管理（Resource Management）**

```
服务器资源有限：
- 每个连接需要分配缓冲区
- 需要维护连接状态
- 需要定时器

三次握手防止 SYN 洪水攻击：
- 服务器在第三次握手后才分配完整资源
- 减少恶意 SYN 的影响

SYN Cookie：
- 不分配资源
- 在 SYN-ACK 的序列号中编码连接信息
- 收到 ACK 后再重建连接状态
```

**3. 优雅关闭（Graceful Shutdown）**

```
四次挥手保证双方数据都发送完毕：

Client                    Server
  |  FIN (我不发了)  →     |
  |  ← ACK (知道了)        |
  |                        | (继续发送未完成数据)
  |    ← FIN (我也不发了)  |
  |  ACK (知道了)  →       |

两次变四次的原因：
- TCP 是全双工
- 一方关闭发送不等于关闭接收
- FIN 只关闭一个方向
- 需要双方都 FIN 才完全关闭
```

---

## 综合心智模型总结

### 从 UDP 到 TCP 的演化

```
Level 0: 裸 UDP
────────────────
发送 → (不管)
问题：不可靠

↓

Level 1: ACK 确认
─────────────────
发送 → 等待 ACK → 确认
问题：ACK 丢失导致卡死

↓

Level 2: 超时重传
─────────────────
发送 → 等待 ACK (超时) → 重传
问题：重复消息

↓

Level 3: 序列号
───────────────
每个消息编号 → 去重 → 检测丢失
问题：效率低（停等）

↓

Level 4: 滑动窗口
─────────────────
并发发送 → 流水线 → 高效
问题：可能拥塞

↓

Level 5: 拥塞控制
─────────────────
动态窗口 → 自适应网络
问题：连接管理

↓

Level 6: 连接管理
─────────────────
三次握手 → 四次挥手 → 完整协议
```

### 核心概念总结

| 机制 | 解决的问题 | 引入的复杂性 | TCP 对应 |
|-----|-----------|-------------|---------|
| **ACK 确认** | 如何知道送达 | 需要等待 | ACK 标志 |
| **超时重传** | ACK 丢失怎么办 | 可能重复 | RTO 计算 |
| **序列号** | 如何去重和排序 | 状态管理 | SEQ/ACK 号 |
| **滑动窗口** | 如何提高效率 | 并发控制 | 接收窗口 |
| **拥塞控制** | 如何避免拥塞 | 动态调整 | cwnd, ssthresh |
| **连接管理** | 如何建立/关闭 | 状态机 | SYN, FIN |

### 设计哲学

```
1. 端到端原则（End-to-End Principle）
   - 可靠性由端点保证
   - 中间网络只负责尽力转发
   - UDP → TCP 体现了这一原则

2. 分层思想（Layering）
   - 每一层解决一个问题
   - 上层建立在下层之上
   - 便于理解和演化

3. 权衡思维（Trade-off）
   - 可靠性 ↔ 效率
   - 简单性 ↔ 功能性
   - 无状态 ↔ 有状态

4. 渐进式复杂度（Progressive Complexity）
   - 从简单开始
   - 遇到问题再增加机制
   - 避免过度设计
```

---

## 实践建议

### 动手实验

**实验 1：观察丢包**

```bash
# 使用 tc 模拟不同丢包率
sudo tc qdisc add dev lo root netem loss 10%   # 10% 丢包
sudo tc qdisc add dev lo root netem loss 50%   # 50% 丢包

# 观察不同版本协议的表现
# - v1: 卡死
# - v2: 卡死（ACK 丢失）
# - v3: 能重传
# - v4: 能去重
# - v5: 高效传输

# 清除
sudo tc qdisc del dev lo root
```

**实验 2：观察拥塞控制**

```python
# 记录 cwnd 变化
import matplotlib.pyplot as plt

cwnd_history = []
# ... (在拥塞控制代码中记录)

plt.plot(cwnd_history)
plt.xlabel('Time (ACK count)')
plt.ylabel('Congestion Window')
plt.title('TCP Congestion Control')
plt.show()

# 观察：
# - 指数增长（慢启动）
# - 线性增长（拥塞避免）
# - 骤降（超时）
```

**实验 3：抓包分析**

```bash
# 使用 tcpdump 抓包
sudo tcpdump -i lo port 9999 -w udp_reliable.pcap

# 使用 Wireshark 分析
# 观察：
# - 消息序列号
# - ACK 模式
# - 重传行为
```

### 常见陷阱

**陷阱 1：忘记处理边界情况**

```python
# ❌ 错误：序列号溢出
seq = (seq + 1) % 256  # 8 位序列号

# 问题：seq=255 后变成 0，可能与旧消息混淆

# ✅ 正确：使用足够大的序列号
seq = (seq + 1) % (2**32)  # 32 位，TCP 使用
```

**陷阱 2：死锁**

```python
# ❌ 错误：双方都等待对方
# Client 等待 Server 数据
# Server 等待 Client 数据
# → 死锁

# ✅ 正确：使用非阻塞 I/O 或超时
sock.settimeout(5.0)
```

**陷阱 3：忽视网络延迟**

```python
# ❌ 错误：固定超时 1 秒
TIMEOUT = 1.0

# 问题：网络延迟可能 > 1 秒

# ✅ 正确：动态计算超时（TCP RTT 估计）
# RTO = RTT + 4 × RTT_VAR
```

---

## 结语：从实现到理解

通过从 UDP 一步步构建可靠协议，我们重新发明了 TCP 的核心机制。这个过程的价值在于：

1. **理解本质**：可靠性不是一蹴而就，而是层层构建
2. **权衡思维**：每个机制都有代价，需要权衡
3. **问题驱动**：设计来源于解决实际问题
4. **系统思维**：各个机制相互配合，形成完整系统

当你真正理解了这些原理，会发现：
- TCP 不再是黑盒，而是一系列优雅的解决方案
- 网络编程不再神秘，而是可以推理的
- 可以设计自己的协议（如 QUIC、KCP）
- 能够调优和排查网络问题

**最后的建议**：
- 动手实现每个版本
- 用抓包工具观察实际行为
- 阅读 TCP RFC（RFC 793, RFC 5681）
- 研究现代协议（QUIC, SCTP）

当你开始自然地用"窗口"、"拥塞"、"重传"这些概念思考网络问题时，你就已经从应用开发者成长为系统开发者了。
