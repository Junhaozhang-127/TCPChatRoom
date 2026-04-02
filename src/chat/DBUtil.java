package chat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBUtil {

    private static final String URL =
            "jdbc:mysql://localhost:3306/chatroom?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 保存消息
    public static void saveMessage(String sender, String receiver, String content) {
        String sql = "INSERT INTO chat_log(sender, receiver, content, time) VALUES(?,?,?,NOW())";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, content);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 返回最近 N 条和当前用户相关的消息
    public static List<String> loadRecentMessages(String username, int limit) {
        List<String> list = new ArrayList<>();
        String sql = """
                SELECT sender, receiver, content, time
                FROM chat_log
                WHERE receiver = 'ALL' OR sender = ? OR receiver = ?
                ORDER BY id DESC
                LIMIT ?
                """;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, username);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String sender = rs.getString("sender");
                String receiver = rs.getString("receiver");
                String content = rs.getString("content");
                String time = String.valueOf(rs.getTimestamp("time"));

                if ("ALL".equals(receiver)) {
                    list.add(0, "[" + time + "] " + sender + "：" + content);
                } else if (sender.equals(username)) {
                    list.add(0, "[" + time + "] [私聊→" + receiver + "] " + content);
                } else {
                    list.add(0, "[" + time + "] [私聊←" + sender + "] " + content);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
