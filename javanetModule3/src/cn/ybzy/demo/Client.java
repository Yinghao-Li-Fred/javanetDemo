package cn.ybzy.demo;

import javax.swing.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.Set;

public class Client {
    // 端口
    public static final int SERVER_PORT = 8888;
    // 定义一个信息编码器
    private Charset charset = Charset.forName("UTF-8");
    // 定义两个缓冲区
    private ByteBuffer rBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer wBuffer = ByteBuffer.allocate(1024);
    // 定义Selector
    private Selector selector;
    // 第一次登录失败后用到的小tip
    private String tip = "";

    // 初始化客户端
    private void init() {
        try {
            SocketChannel clientChannel = SocketChannel.open(); // 获取SocketChannel套接字通道
            clientChannel.configureBlocking(false); // 设置非阻塞模式
            selector = Selector.open();
            clientChannel.register(selector, SelectionKey.OP_CONNECT); // 注册clientChannel
            clientChannel.connect(new InetSocketAddress("127.0.0.1", SERVER_PORT)); // 非阻塞连接，会马上返回，程序继续往下执行
            // 循环监听
            while (true) {
                int count = selector.select(5000); // select a set of keys whose corresponding channels are ready for IO operation
                if (count == 0) continue;
                Set<SelectionKey> keys = selector.selectedKeys(); // 拿到准备好通道的集合
                for (SelectionKey key : keys) {  // 遍历该集合
                    // 对key进行处理
                    handler(key);
                    keys.remove(key);
                }
            }
        } catch (IOException e) {
            System.out.println("Server connection error!");
        }
    }

    /**
     * 处理事件的方法
     * @param key
     */
    private void handler (SelectionKey key) {
        // 首先第一个事件就是连接事件 OP_CONNECT
        if (key.isConnectable()) {
            try {
                SocketChannel client = (SocketChannel) key.channel(); // 连接上服务器的客户端
                if (client.isConnectionPending()) { // 是否正在连接服务器
                    client.finishConnect(); // 如果客户端已经连接上服务器。会返回true，如果没连接上会报错。此句话如果正常没有报错的情况下，执行过了，可以确定，客户端已经正常连接上了服务器
                    System.out.println("The client connect to the server successfully!");
                    // 实现客户端登录服务器
                    login(client, tip); // 因为是非阻塞模式，所以要封装成一个方法, readline()不会堵在那，即使读不到东西也会往下走
                    // 开启一个子线程，负责获取键盘信息（公聊信息，私聊信息）发送给服务器
                    new Thread(new ClientThread(client)).start();
                    // 改变选择器对通道的兴趣事件
                    client.register(selector, SelectionKey.OP_READ); // key.interestOps(SelectionKey.OP_READ);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (key.isReadable()) {
            try {
                // 服务器端有信息发过来，服务器会发送两类信息，一类是登录回复，一类是聊天内容
                SocketChannel client = (SocketChannel) key.channel();
                rBuffer.clear();
                int count = client.read(rBuffer);
                if (count > 0) {
                    // String str = String.valueOf(charset.decode(rBuffer)); // 取到信息
                    String str = new String(rBuffer.array(), 0, count, "UTF-8");
                    if (str.startsWith(ChatRoomProtocol.USER_ROUND) && str.endsWith(ChatRoomProtocol.USER_ROUND)) { // 如果取到的信息是登录回复
                        // 拿到真正的登录回复信息
                        String loginRes = getRealMsg(str);
                        if (loginRes.equals(ChatRoomProtocol.NAME_REP)) { // 如果登录失败
                            tip = "Username already exists, please login again!";
                            login(client, tip);
                        } else if (loginRes.equals(ChatRoomProtocol.LOGIN_SUCCESS)) {
                            System.out.println( "login success!");
                        }
                    } else { // 公聊信息或私聊信息，回来的是聊天信息
                        System.out.println(str);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 实现客户端登录服务器的方法
    private void login(SocketChannel client, String tip) {
        try {
            // 用GUI里的弹出对话框
            String username = JOptionPane.showInputDialog(tip + "Username: ");
            // 把username发送到服务器上去
            wBuffer.clear();
            wBuffer.put(charset.encode(ChatRoomProtocol.USER_ROUND + username + ChatRoomProtocol.USER_ROUND));
            wBuffer.flip();
            while (wBuffer.hasRemaining()) {
                client.write(wBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 开启一个子线程，负责获取键盘信息（公聊信息，私聊信息）发送给服务器
    private class ClientThread implements Runnable {
        private SocketChannel client;
        public ClientThread (SocketChannel client) {
            this.client = client;
        }
        @Override
        public void run() {
            while (true) {
                try {
                    Scanner scanner = new Scanner(System.in);
                    String line = scanner.nextLine();
                    if (line.indexOf(":") > 0 && line.startsWith("//")) { // 判断是公聊信息还是私聊信息
                        // 私聊信息
                        line = line.substring(2); // 去掉开头的两个双斜杠
                        wBuffer.clear();
                        wBuffer.put(charset.encode(ChatRoomProtocol.PRIVATEMSG_ROUND + line.split(":")[0] + ChatRoomProtocol.SPLIT_SIGN + line.split(":")[1] + ChatRoomProtocol.PRIVATEMSG_ROUND));
                        wBuffer.flip();
                        while (wBuffer.hasRemaining()) {
                            client.write(wBuffer);
                        }
                    } else {
                        // 公聊信息
                        wBuffer.clear();
                        wBuffer.put(charset.encode(ChatRoomProtocol.PUBLICMSG_ROUND + line + ChatRoomProtocol.PUBLICMSG_ROUND));
                        wBuffer.flip();
                        while (wBuffer.hasRemaining()) {
                            client.write(wBuffer);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 去除协议字符的方法
    private String getRealMsg(String lines) {
        return lines.substring(ChatRoomProtocol.PROTOCOL_LEN, lines.length()-ChatRoomProtocol.PROTOCOL_LEN);
    }

    // 添加执行入口
    public static void main(String[] args) {
        Client client = new Client();
        client.init();
    }
}
