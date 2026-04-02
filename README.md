# TCPChatRoom

一个基于 Java Socket + Swing 的局域网聊天室示例项目，支持：
- 登录与在线用户列表
- 群聊
- 私聊
- 表情、图片、文件消息
- MySQL 聊天记录持久化

## 目录结构

```text
TCPChatRoom/
├─ src/chat/
│  ├─ ChatServer.java        # 服务端入口
│  ├─ ChatClientMain.java    # GUI 客户端入口
│  ├─ ChatClientGUI.java     # GUI 逻辑
│  ├─ ClientHandler.java     # 服务端单连接处理
│  ├─ ClientReceiver.java    # 客户端消息接收线程
│  ├─ DBUtil.java            # MySQL 读写
│  ├─ ChatClient.java        # 命令行测试客户端（保留）
│  └─ avatars/               # 静态资源（当前未启用）
├─ lib/mysql-connector-j-9.1.0.jar
├─ sql/init_chatroom.sql
└─ scripts/smoke_test.py
```

## 环境要求

- JDK 17
- MySQL 8.x
- IntelliJ IDEA（推荐，项目已内置 `.idea` 配置）
- Python 3.7+（仅用于冒烟脚本）

## 数据库初始化

1. 确保 MySQL 已启动。
2. 执行脚本：

```sql
SOURCE sql/init_chatroom.sql;
```

3. 如需修改数据库连接，请编辑 `src/chat/DBUtil.java` 中这三项：
- `URL`
- `USER`
- `PASSWORD`

默认值：
- 数据库：`chatroom`
- 用户：`root`
- 密码：`123456`

## 启动方式（IntelliJ）

1. 打开项目根目录 `TCPChatRoom`。
2. 确认 Project SDK 为 JDK 17。
3. 先运行 `chat.ChatServer`。
4. 再运行一个或多个 `chat.ChatClientMain` 实例。

## 启动方式（命令行，可选）

在项目根目录执行：

```powershell
javac -encoding UTF-8 -cp "lib/mysql-connector-j-9.1.0.jar" -d out/production/TCPChatRoom src/chat/*.java
java -cp "out/production/TCPChatRoom;lib/mysql-connector-j-9.1.0.jar" chat.ChatServer
```

另开终端启动客户端：

```powershell
java -cp "out/production/TCPChatRoom;lib/mysql-connector-j-9.1.0.jar" chat.ChatClientMain
```

## 协议格式（文本行）

- 登录：`LOGIN|username`
- 群聊：`CHAT|username|content`
- 私聊：`PRIVATE|sender|receiver|content`
- 表情：`EMOJI|username|emoji`
- 图片：`IMAGE|username|base64`
- 文件：`FILE|username|fileName|base64`
- 在线列表（服务端推送）：`USERLIST|u1,u2,...`

## 冒烟测试

在服务端启动后执行：

```powershell
python scripts/smoke_test.py --host 127.0.0.1 --port 8888
```

脚本会校验：
- 登录广播
- 群聊广播
- 私聊定向投递
- 文件消息广播
