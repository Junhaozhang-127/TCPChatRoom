package chat;

import java.io.*;
import java.net.Socket;

/**
 * 客户端处理器
 * 负责：
 * 1. 接收客户端发送的消息
 * 2. 根据消息类型广播或私聊
 * 3. 保存历史消息到数据库（可选）
 */
public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public void send(String msg) {
        out.println(msg);
    }

    @Override
    public void run() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {

                String[] data = msg.split("\\|", 4);
                String type = data[0];

                switch (type) {
                    case "LOGIN":
                        username = data[1];
                        ChatServer.addClient(this);
                        ChatServer.broadcast("SYSTEM|" + username + " 上线了");
                        ChatServer.sendUserList();
                        break;

                    case "CHAT":
                        // 可选：保存聊天记录到数据库
                        DBUtil.saveMessage(username, "ALL", data[2]);
                        ChatServer.broadcast(msg);
                        break;

                    case "EMOJI":
                    case "IMAGE":
                        // 可选：保存占位符到数据库
                        DBUtil.saveMessage(username, "ALL", "[" + type + "]");
                        ChatServer.broadcast(msg);
                        break;

                    case "FILE":
                        // 可选：保存占位符到数据库
                        DBUtil.saveMessage(username, "ALL", "[文件] " + data[2]);
                        ChatServer.broadcast(msg);
                        break;

                    case "PRIVATE":
                        ChatServer.sendPrivate(data[2], msg);
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println(username + " 下线");
        } finally {
            ChatServer.removeClient(this);
        }
    }
}
