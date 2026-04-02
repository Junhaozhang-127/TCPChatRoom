package chat;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 聊天客户端 GUI（最终版）
 * 功能：
 * 1. 群聊、私聊
 * 2. 图片发送/显示
 * 3. 文件发送/接收/保存
 * 4. 在线用户列表
 * 5. 历史聊天记录（避免重复显示）
 */
public class ChatClientGUI extends JFrame {

    private JTextPane chatPane;
    private StyledDocument doc;
    private JTextField inputField;
    private JList<String> userList;
    private DefaultListModel<String> userModel;

    private PrintWriter out;
    private String username;
    private final List<FileLinkRange> fileLinks = new ArrayList<>();

    // 防止历史记录重复显示
    private boolean historyLoaded = false;

    private static class FileLinkRange {
        private final int start;
        private final int endExclusive;
        private final String fileName;
        private final String base64;

        private FileLinkRange(int start, int endExclusive, String fileName, String base64) {
            this.start = start;
            this.endExclusive = endExclusive;
            this.fileName = fileName;
            this.base64 = base64;
        }

        private boolean contains(int pos) {
            return pos >= start && pos < endExclusive;
        }
    }

    public ChatClientGUI(Socket socket, String username) throws Exception {
        this.username = username;
        this.out = new PrintWriter(socket.getOutputStream(), true);

        setTitle("聊天室 - " + username);
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 聊天显示区
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        doc = chatPane.getStyledDocument();
        chatPane.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int pos = chatPane.viewToModel2D(e.getPoint());
                for (int i = fileLinks.size() - 1; i >= 0; i--) {
                    FileLinkRange link = fileLinks.get(i);
                    if (link.contains(pos)) {
                        saveFile(link.fileName, link.base64);
                        break;
                    }
                }
            }
        });
        add(new JScrollPane(chatPane), BorderLayout.CENTER);

        // 在线用户列表
        userModel = new DefaultListModel<>();
        userList = new JList<>(userModel);
        add(new JScrollPane(userList), BorderLayout.EAST);

        // 底部输入区
        JPanel bottom = new JPanel(new BorderLayout());
        inputField = new JTextField();
        bottom.add(inputField, BorderLayout.CENTER);

        JButton sendBtn = new JButton("发送");
        bottom.add(sendBtn, BorderLayout.EAST);

        // 左侧按钮区：表情、图片、文件
        JButton emojiBtn = new JButton("😊");
        JButton imgBtn = new JButton("图片");
        JButton fileBtn = new JButton("文件");

        JPanel left = new JPanel();
        left.add(emojiBtn);
        left.add(imgBtn);
        left.add(fileBtn);

        bottom.add(left, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);

        // 事件绑定
        sendBtn.addActionListener(e -> sendText());
        inputField.addActionListener(e -> sendText());

        // 表情菜单
        JPopupMenu emojiMenu = new JPopupMenu();
        String[] emojis = {"😀", "😂", "😍", "😭"};
        for (String emo : emojis) {
            JMenuItem item = new JMenuItem(emo);
            item.addActionListener(ev -> out.println("EMOJI|" + username + "|" + emo));
            emojiMenu.add(item);
        }
        emojiBtn.addActionListener(e ->
                emojiMenu.show(emojiBtn, 0, emojiBtn.getHeight()));

        // 图片和文件发送
        imgBtn.addActionListener(this::sendImage);
        fileBtn.addActionListener(this::sendFile);

        // 双击用户名私聊
        userList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String target = userList.getSelectedValue();
                    if (target != null && !target.equals(username)) {
                        openPrivateChat(target);
                    }
                }
            }
        });

        // 加载历史记录（只加载一次）
        loadHistory();

        setVisible(true);
    }

    // 发送文字（群聊或私聊）
    private void sendText() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            String target = userList.getSelectedValue();
            if (target != null && !target.equals(username)) {
                // 私聊：发送方本地显示
                appendText("[私聊→" + target + "] " + text + "\n");
                out.println("PRIVATE|" + username + "|" + target + "|" + text);
            } else {
                // 群聊
                out.println("CHAT|" + username + "|" + text);
            }
            inputField.setText("");
        }
    }

    // 发送图片
    private void sendImage(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                byte[] data = java.nio.file.Files.readAllBytes(
                        chooser.getSelectedFile().toPath());
                out.println("IMAGE|" + username + "|" + Base64.getEncoder().encodeToString(data));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // 发送文件
    private void sendFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
                out.println("FILE|" + username + "|" + file.getName() + "|" +
                        Base64.getEncoder().encodeToString(data));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // 显示消息
    public void showMessage(String msg) {
        try {
            if (msg == null || msg.isBlank()) {
                return;
            }
            // 如果消息是历史消息标记且已经加载过，忽略
            if (historyLoaded && msg.startsWith("HISTORY|")) return;

            String[] data = msg.split("\\|", 4);
            if (data.length == 0) {
                return;
            }
            switch (data[0]) {
                case "SYSTEM":
                    if (data.length < 2) break;
                    appendText("[系统] " + data[1] + "\n");
                    break;
                case "CHAT":
                    if (data.length < 3) break;
                    appendText(data[1] + "：" + data[2] + "\n");
                    break;
                case "EMOJI":
                    if (data.length < 3) break;
                    appendText(data[1] + "：" + data[2] + "\n");
                    break;
                case "IMAGE":
                    if (data.length < 3) break;
                    appendText(data[1] + "：");
                    appendImage(data[2]);
                    appendText("\n");
                    break;
                case "FILE":
                    if (data.length < 4) break;
                    appendText(data[1] + " 发送文件：");
                    insertFileLink(data[2], data[3]);
                    appendText("\n");
                    break;
                case "PRIVATE":
                    if (data.length < 4) break;
                    String sender = data[1];
                    String receiver = data[2];
                    String content = data[3];
                    // 接收方显示
                    if (receiver.equals(username)) {
                        appendText("[私聊←" + sender + "] " + content + "\n");
                    }
                    break;
                case "USERLIST":
                    if (data.length < 2) break;
                    userModel.clear();
                    for (String u : data[1].split(",")) {
                        if (!u.isEmpty()) userModel.addElement(u);
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 追加文字
    private void appendText(String text) {
        try {
            doc.insertString(doc.getLength(), text, null);
            chatPane.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 插入图片
    private void appendImage(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            ImageIcon icon = new ImageIcon(bytes);
            Image img = icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
            chatPane.insertIcon(new ImageIcon(img));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 插入可点击文件链接
    private void insertFileLink(String fileName, String base64) {
        try {
            SimpleAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setUnderline(attr, true);
            StyleConstants.setForeground(attr, Color.BLUE);

            int start = doc.getLength();
            doc.insertString(start, fileName, attr);
            fileLinks.add(new FileLinkRange(start, start + fileName.length(), fileName, base64));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 保存文件到本地
    private void saveFile(String fileName, String base64) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(fileName));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                byte[] data = Base64.getDecoder().decode(base64);
                java.nio.file.Files.write(chooser.getSelectedFile().toPath(), data);
                JOptionPane.showMessageDialog(this, "文件保存成功");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 打开私聊输入框
    private void openPrivateChat(String target) {
        String msg = JOptionPane.showInputDialog(
                this,
                "对 " + target + " 说：",
                "私聊",
                JOptionPane.PLAIN_MESSAGE
        );

        if (msg != null && !msg.trim().isEmpty()) {
            appendText("[私聊→" + target + "] " + msg + "\n"); // 本地显示
            out.println("PRIVATE|" + username + "|" + target + "|" + msg);
        }
    }

    // 加载历史聊天记录（只加载一次，避免重复）
    public void loadHistory() {
        if (historyLoaded) return;
        historyLoaded = true;

        try {
            for (String s : DBUtil.loadRecentMessages(username, 20)) {
                appendText(s + "\n");
            }
            appendText("----------- 以上是历史记录 -----------\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
