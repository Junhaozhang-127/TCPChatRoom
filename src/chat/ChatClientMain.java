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
            Socket socket = new Socket("localhost", 8888);

            ChatClientGUI gui = new ChatClientGUI(socket, username);
            gui.loadHistory(); // 加载历史聊天记录

            // 接收服务器消息线程
            new Thread(() -> {
                try {
                    var in = new java.io.BufferedReader(
                            new java.io.InputStreamReader(socket.getInputStream()));
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        gui.showMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            var out = new java.io.PrintWriter(socket.getOutputStream(), true);
            out.println("LOGIN|" + username);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
