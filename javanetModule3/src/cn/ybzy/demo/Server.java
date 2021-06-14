package cn.ybzy.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

public class Server {
    // 服务端绑定的端口号
    public static final int SERVER_PORT = 8888;
    // 定义字符编码器
    private Charset charset = Charset.forName("UTF-8");
    // 定义两个缓冲区，用来收发数据
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
    // 定义集合，放连接进来的客户端信息
    private static ChatRoomMap<String, SocketChannel> clients = new ChatRoomMap<>();
    // 定义选择器
    private Selector selector;

    // 初始化服务器包装成一个方法
    private void init() {
        // 拿到ServerSocketChannel的实例
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);// 设置为非阻塞模式
            ServerSocket serverSocket = serverSocketChannel.socket(); // returns a server socket associated with this channel
            serverSocket.bind(new InetSocketAddress("127.0.0.1", SERVER_PORT));
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); // 将通道注册进选择器，感兴趣事件只能是accept
            System.out.println("The server has started up!");
        } catch (IOException e) {
            System.out.println("Server connecting error!");
        }
    }

    // 循环监听通道
    public void listen() {
        while (true) {
            try {
                int count = selector.select(5000); // 5秒钟内，准备好的客户端
                if (count==0) continue;
                Set<SelectionKey> keys = selector.selectedKeys(); // 拿到准备好的通道的集合
                for (SelectionKey key : keys) { // 对keys进行遍历
                    handler(key); // 处理准备就绪的通道
                    keys.remove(key); // 把处理过的通道手动地从集合中移除
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 处理准备就绪的通道的方法
    private void handler(SelectionKey key) {
        // 第一件要处理的事情：接入客户端
        if (key.isAcceptable()) { // 有新的客户端进来
            try {
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel(); // 把通道取出来
                SocketChannel clientSocketChannel = serverSocketChannel.accept();// 正式地接入客户端
                clientSocketChannel.configureBlocking(false); // 设置为非阻塞模式
                clientSocketChannel.register(selector, SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (key.isReadable()) {
            // 客户端有信息发送过来了
            SocketChannel clientSocketChannel = (SocketChannel) key.channel(); // 把通道拿到，是与客户端一一对应的socketChannel
            try {
                readBuffer.clear();
                int hasRead = clientSocketChannel.read(readBuffer);
                if (hasRead > 0) {
                    readBuffer.flip();
                    String str = String.valueOf(charset.decode(readBuffer));
                    if (str.startsWith(ChatRoomProtocol.USER_ROUND) && str.endsWith(ChatRoomProtocol.USER_ROUND)) { // 如果发送过来的是用户信息
                        login(clientSocketChannel, str); // 实现客户登录
                    } else if (str.startsWith(ChatRoomProtocol.PRIVATEMSG_ROUND) && str.endsWith(ChatRoomProtocol.PRIVATEMSG_ROUND)) { // 如果发送过来的是私聊信息
                        // 服务器收到信息后，向指定服务器转发信息
                        sendMsgToUser(clientSocketChannel, str);
                    } else if (str.startsWith(ChatRoomProtocol.PUBLICMSG_ROUND) && str.endsWith(ChatRoomProtocol.PUBLICMSG_ROUND)) { // 最后一种可能：发送的是公聊信息
                        // 广播
                        dispatch(clientSocketChannel, str);
                    }
                }
            } catch (IOException e) {
                Server.clients.removeByValue(clientSocketChannel);
            }
        }
    }

    /**
     * 实现客户端的登录，把客户端用一个用户名标记
     * @param client
     * @param str
     */
    private void login(SocketChannel client, String str) {
        try {
            // 首先，把真正的用户名拿到
            String username = getRealMsg(str);
            if (Server.clients.map.containsKey(username)) {
                System.out.println("User already exists!");
                writeBuffer.clear();
                writeBuffer.put(charset.encode(ChatRoomProtocol.USER_ROUND + ChatRoomProtocol.NAME_REP + ChatRoomProtocol.USER_ROUND)); // 把要响应回客户端的信息放在WriteBuffer里
                writeBuffer.flip();
                while (writeBuffer.hasRemaining()) {
                    client.write(writeBuffer);
                }
            } else {
                System.out.println("User login success!");
                writeBuffer.clear();
                writeBuffer.put(charset.encode(ChatRoomProtocol.USER_ROUND + ChatRoomProtocol.LOGIN_SUCCESS + ChatRoomProtocol.USER_ROUND));
                writeBuffer.flip();
                while (writeBuffer.hasRemaining()) {
                    client.write(writeBuffer);
                }
                Server.clients.put(username, client); // 把该用户放到clients数组里
            }
        } catch (IOException e) {
            // 客户端连接出了问题，移除该客户端
            e.printStackTrace();
        }
    }

    /**
     * 对私聊信息的转发
     * @param client
     * @param str
     */
    private void sendMsgToUser(SocketChannel client, String str) {
        try {
            // 客户端发送来的是私聊信息
            // 拿到真正的信息，信息里包含了目标用户和消息
            String userAndMsg = getRealMsg(str);
            // 上面的信息是用ChatRoomProtocol.SPLIT_SIGN来隔开的
            String targetUser = userAndMsg.split(ChatRoomProtocol.SPLIT_SIGN)[0]; // 拿到消息里的目标用户信息
            String privateMsg = userAndMsg.split(ChatRoomProtocol.SPLIT_SIGN)[1]; // 拿到消息里的发送信息
            // 服务器转发给指定用户
            writeBuffer.clear();
            writeBuffer.put(charset.encode(Server.clients.getKeyByValue(client) + "privately say: " + privateMsg));
            writeBuffer.flip();
            while (writeBuffer.hasRemaining()) {
                // 拿到目标用户targetUser的socketChannel
                Server.clients.map.get(targetUser).write(writeBuffer);
            }
        } catch (IOException e) {
            Server.clients.removeByValue(client);
        }
    }

    /**
     * 对公聊信息的转播
     * @param client
     * @param str
     */
    public void dispatch(SocketChannel client, String str) {
        try {
            // 先拿到真正的信息
            String publicMsg =getRealMsg(str);
             Set<SocketChannel> valueSet = Server.clients.getValueSet();
             for (SocketChannel cli : valueSet) { // 循环遍历每一个客户的SocketChannel，发送信息
                 writeBuffer.clear();
                 writeBuffer.put( charset.encode(Server.clients.getKeyByValue(client) + " say: " + publicMsg));
                 writeBuffer.flip();
                 while (writeBuffer.hasRemaining()) {
                     cli.write(writeBuffer);
                 }
             }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    // 去除协议字符的方法
    private String getRealMsg(String lines) {
        return lines.substring(ChatRoomProtocol.PROTOCOL_LEN, lines.length()-ChatRoomProtocol.PROTOCOL_LEN);
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.init(); // 初始化服务器
        server.listen(); // 开始监听
    }
}
