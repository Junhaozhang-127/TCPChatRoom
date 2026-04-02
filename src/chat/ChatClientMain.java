package chat;

import java.net.Socket;
import javax.swing.JOptionPane;

/**
 * 聊天客户端启动类
 * 功能：
 * 1. 输入用户名
 * 2. 连接服务器
 * 3. 创建 GUI
 * 4. 启动接收消息线程
 */
public class ChatClientMain {

    public static void main(String[] args) {
        try {
            String username = JOptionPane.showInputDialog("请输入用户名");
            if (username == null || username.isBlank()) {
                JOptionPane.showMessageDialog(null, "用户名不能为空");
                return;
            }

            Socket socket = new Socket("localhost", 8888);

            ChatClientGUI gui = new ChatClientGUI(socket, username);

            // 接收服务器消息线程
            var in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(socket.getInputStream()));
            new Thread(new ClientReceiver(in, gui)).start();

            var out = new java.io.PrintWriter(socket.getOutputStream(), true);
            out.println("LOGIN|" + username);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
