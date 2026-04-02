package chat;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天室服务器
 * 功能：
 * 1. 接受客户端连接
 * 2. 管理在线用户列表
 * 3. 广播消息（群聊、私聊、图片、文件、表情）
 */
public class ChatServer {

    // 保存所有客户端处理器
    private static final List<ClientHandler> clients = new ArrayList<>();

    public static synchronized void addClient(ClientHandler c) {
        clients.add(c);
    }

    public static synchronized void removeClient(ClientHandler c) {
        clients.remove(c);
        String username = c.getUsername();
        if (username != null && !username.isBlank()) {
            broadcast("SYSTEM|" + username + " 下线了");
        }
        sendUserList();
    }

    // 群发消息
    public static synchronized void broadcast(String msg) {
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }

    // 私聊
    public static synchronized void sendPrivate(String target, String msg) {
        for (ClientHandler c : clients) {
            if (target.equals(c.getUsername())) {
                c.send(msg);
            }
        }
    }

    // 更新在线用户列表
    public static synchronized void sendUserList() {
        StringBuilder sb = new StringBuilder("USERLIST|");
        for (ClientHandler c : clients) {
            String username = c.getUsername();
            if (username != null && !username.isBlank()) {
                sb.append(username).append(",");
            }
        }
        broadcast(sb.toString());
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            System.out.println("聊天室服务器启动，端口 8888");
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
