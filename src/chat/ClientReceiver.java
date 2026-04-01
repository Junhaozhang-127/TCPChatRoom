package chat;

import java.io.BufferedReader;

/**
 * 客户端接收线程
 * 作用：
 *  1. 一直接收服务器发来的消息
 *  2. 转交给 ChatClientGUI.showMessage() 显示
 *
 * 这是一个“纯接收线程”，不负责任何业务逻辑
 */
public class ClientReceiver implements Runnable {

    /** 服务器输入流 */
    private BufferedReader in;

    /** 客户端 GUI，用来显示消息 */
    private ChatClientGUI gui;

    /**
     * 构造方法
     *
     * @param in  从服务器读取消息的 BufferedReader
     * @param gui 聊天窗口对象
     */
    public ClientReceiver(BufferedReader in, ChatClientGUI gui) {
        this.in = in;
        this.gui = gui;
    }

    /**
     * 线程入口
     * 不断读取服务器消息并显示
     */
    @Override
    public void run() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                // 所有显示逻辑都交给 GUI
                gui.showMessage(msg);
            }
        } catch (Exception e) {
            System.out.println("与服务器断开连接");
        }
    }
}
