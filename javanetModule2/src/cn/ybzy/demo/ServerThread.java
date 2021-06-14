package cn.ybzy.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 * 线程类，是用来处理与客户端直接的数据交互的
 * 肯定就需要，与客户端对应的那个套接字
 */
public class ServerThread implements Runnable {
    private Socket socket = null;
    private BufferedReader br = null;
    private PrintStream ps = null;
    public ServerThread(Socket socket) throws IOException {
        this.socket = socket;

    }
    @Override
    public void run() {
        try {
            // 获取客户端对应的输入流
            br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "GBK"));
            // 再拿到客户端对应的输出流
            ps = new PrintStream(socket.getOutputStream(), true, "GBK");
            // 先通过输入流读数据
            String lines = null;
            // 用循环不断地读取客户端发送来的信息
            while ((lines=br.readLine()) != null) {
                // 先读取客户端发送来的用户名称，协议规定，客户端发送来的用户信息，必须是USER_ROUND作为信息的前后缀
                if (lines.startsWith(ChatRoomProtocol.USER_ROUND) && lines.endsWith(ChatRoomProtocol.USER_ROUND)) {
                    // 接收到的是用户名称，写下来拿到真正的用户名称
                    // 定义一个方法去掉前后缀
                    String userName = getRealMsg(lines);
                    // 判断用户不能重复
                    if (Server.clients.map.containsKey(userName)) { // 用户已存在于map集合中
                        System.out.println("This user already exists!");
                        ps.println(ChatRoomProtocol.NAME_REP);
                    } else {
                        System.out.println("User login success!");
                        ps.println(ChatRoomProtocol.LOGIN_SUCCESS);
                        Server.clients.put(userName, ps);
                    }
                } else if(lines.startsWith(ChatRoomProtocol.PRIVATEMSG_ROUND) && lines.endsWith(ChatRoomProtocol.PRIVATEMSG_ROUND)) {
                    // 客户端发送来的信息是私聊
                    // 先拿到真正的信息，信息里包含了目标用户和信息
                    String userAndMsg = getRealMsg(lines);
                    // 上面的信息是用ChatroomProtocol.SPLIT_SIGN来隔开的。 拿到目标用户名
                    String targetUser = userAndMsg.split(ChatRoomProtocol.SPLIT_SIGN)[0];
                    String privatemsg = userAndMsg.split(ChatRoomProtocol.SPLIT_SIGN)[1];
                    // 拿到前面两个信息，服务器就可以转发给指定的用户
                    Server.clients.map.get(targetUser).println(Server.clients.getKeyByValue(ps) + " this user privately say: " + privatemsg);
                } else { // 最后一种可能：公聊
                    // 先拿到真正的信息
                    String publicmsg = getRealMsg(lines);
                    // 广播
                    for (PrintStream clientPs : Server.clients.getValueSet()) {
                        clientPs.println(Server.clients.getKeyByValue(ps) + " say: " + publicmsg);
                    }
                }
            }
        } catch (Exception e) {
            // 如果客户端与服务端Socket交互发生异常
            // 这个客户端可能已经关闭了，这个客户端应该从clients集合里删除
            Server.clients.removeByValue(ps);
            System.out.println(Server.clients.map.size());
        } finally {
            try {
                // 关闭IO和网络
                br.close();
                ps.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 去除协议字符的方法
    private String getRealMsg(String lines) {
        return lines.substring(ChatRoomProtocol.PROTOCOL_LEN, lines.length()-ChatRoomProtocol.PROTOCOL_LEN);
    }
}
