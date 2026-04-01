//ChatClient 为命令行测试客户端，后续功能已由 ChatClientGUI 取代
package chat;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("127.0.0.1", 8888);
            System.out.println("已连接服务器");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);

            Scanner scanner = new Scanner(System.in);

            // 接收服务器消息线程
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (Exception e) {}
            }).start();

            System.out.print("请输入用户名：");
            String username = scanner.nextLine();
            out.println("LOGIN|" + username);


            // 发送消息
            while (true) {
                String text = scanner.nextLine();
                out.println(text);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
