package cn.ybzy.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int SERVER_PORT = 31000;
    private static final String CHARSET = "UTF-8";
    private Charset charset = Charset.forName(CHARSET);
    // 连接进来的客户端，保存到统一的集合里
    public static ChatRoomMap<String, AsynchronousSocketChannel> clients = new ChatRoomMap<>();

    // 对服务器进行初始化
    public void init() {
        try {
            // 创建线程池executorService
            ExecutorService executorService = Executors.newFixedThreadPool(20); // 创建一个大小固定的线程池
            // 创建channelGroup
            AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            // 拿到服务器的套接字通道
            AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup); // 绑定线程池的服务端的套接字的通道
            // 绑定IP地址
            serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", SERVER_PORT));
            // 循环接收客户端的连接
            serverSocketChannel.accept(null, new AcceptHandler(serverSocketChannel)); // accept本身并不执行IO，只是通知操作系统利用该线程池进行IO操作
        } catch (Exception e) {
            System.out.println("server startup error, port number may be occupied");
        }
    }

    // 内部类
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {
        private AsynchronousServerSocketChannel serverSocketChannel;
        // 接收客户信息和发送客户信息的缓冲区
        private ByteBuffer rBuffer = ByteBuffer.allocate(1024);
        private ByteBuffer wBuffer = ByteBuffer.allocate(1024);
        public AcceptHandler(AsynchronousServerSocketChannel serverSocketChannel) {
            this.serverSocketChannel = serverSocketChannel;
        }
        /**
         * 操作系统完成了指定的IO后，回调completed函数
         * @param clientSocketChannel
         * @param attachment
         */
        @Override
        public void completed(AsynchronousSocketChannel clientSocketChannel, Object attachment) { // 相当于一个监听器,应用程序对操作系统完成IO结果的一个监听
            // 这里表示服务器有客户端连进来了，对客户端进行处理
            // 递归，循环不停接收新的客户端连接
            serverSocketChannel.accept(null, this);
            // 读取客户端传来的信息，clientSocketChannel表示连接进来的客户端
            clientSocketChannel.read(rBuffer, null, new CompletionHandler<Integer, Object>() { // 异步读数据，操作系统底层处理IO读数据
                @Override
                public void completed(Integer hasRead, Object attachment) { // 数据读取成功，数据已经到rBuffer里了，result是读到缓冲器里的字节数
                    rBuffer.flip();
                    String content = charset.decode(rBuffer).toString();//StandardCharsets.UTF_8.decode(rBuffer).toString(); //Charset.forName("UTF-8").decode(rBuffer).toString(); //String.valueOf(Charset.forName("UTF-8").decode(rBuffer)); //new String(rBuffer.array(), 0, result);
                    // 服务端收到客户端的信息有两类：客户端注册来的用户名，聊天信息
                    if(content.startsWith(ChatRoomProtocol.USER_ROUND) && content.endsWith(ChatRoomProtocol.USER_ROUND)) {
                        // 信息是注册过来的用户名，要进行一系列的处理
                        login(clientSocketChannel, content);
                    } else if (content.startsWith(ChatRoomProtocol.PRIVATEMSG_ROUND) && content.endsWith(ChatRoomProtocol.PRIVATEMSG_ROUND)){ // 聊天信息，判断是公聊信息还是私聊信息
                        // 私聊信息
                        sendMsgToUser(clientSocketChannel, content);
                    } else if (content.startsWith(ChatRoomProtocol.PUBLICMSG_ROUND) && content.endsWith(ChatRoomProtocol.PUBLICMSG_ROUND)) {
                        // 公聊信息
                        dispatch(clientSocketChannel, content);
                    }
                    rBuffer.clear();
                    // 递归，循环读取数据
                    clientSocketChannel.read(rBuffer, null, this);
                }
                @Override
                public void failed(Throwable exc, Object attachment) {
                    System.out.println("data reading failed: " + exc); // exc: 失败原因
                    // 可能是客户端关闭了，把失效的客户端从集合里移除
                    Server.clients.removeByValue(clientSocketChannel);
                }
            });
        }

        /**
         * 操作系统完成指定IO的过程中出现异常，回调failed函数
         * @param exc
         * @param attachment
         */
        @Override
        public void failed(Throwable exc, Object attachment) { // 如果IO流操作失败，操作系统调用failed函数
            System.out.println("connecting failed: " + exc);
        }

        /**
         * 服务端实现客户端的登录功能
         * @param client
         * @param content
         */
        private void login(AsynchronousSocketChannel client, String content) {
            System.out.println("login coming!");
            try {
                // 首先，把真正的用户名拿到
                String username = getRealMsg(content);
                if (Server.clients.map.containsKey(username)) { // 用户名重复
                    System.out.println("User already exists!");
                    wBuffer.clear();
                    wBuffer.put(charset.encode(ChatRoomProtocol.USER_ROUND + ChatRoomProtocol.NAME_REP + ChatRoomProtocol.USER_ROUND));
                    wBuffer.flip();
                    client.write(wBuffer).get(); // 异步通信，应用程序通知操作系统写，不可以用while循环

                } else {
                    System.out.println("User login success!");
                    wBuffer.clear();
                    wBuffer.put(charset.encode(ChatRoomProtocol.USER_ROUND + ChatRoomProtocol.LOGIN_SUCCESS + ChatRoomProtocol.USER_ROUND));
                    wBuffer.flip();
                    client.write(wBuffer).get();
                    // 登录成功后，把该客户端保存到集合中
                    Server.clients.put(username, client);
                }
            } catch (Exception e) {
                // 客户端连接出了问题，移除该客户端
                e.printStackTrace();
            }
        }

        /**
         * 对私聊信息的转发
         * @param client
         * @param str
         */
        private void sendMsgToUser(AsynchronousSocketChannel client, String str) {
            try {
                // 客户端发送来的是私聊信息
                // 拿到真正的信息，信息里包含了目标用户和消息
                String userAndMsg = getRealMsg(str);
                // 上面的信息是用ChatRoomProtocol.SPLIT_SIGN来隔开的
                String targetUser = userAndMsg.split(ChatRoomProtocol.SPLIT_SIGN)[0]; // 拿到消息里的目标用户信息
                String privateMsg = userAndMsg.split(ChatRoomProtocol.SPLIT_SIGN)[1]; // 拿到消息里的发送信息
                // 服务器转发给指定用户
                wBuffer.clear();
                wBuffer.put(charset.encode(Server.clients.getKeyByValue(client) + "privately say: " + privateMsg));
                wBuffer.flip();
                // 拿到目标用户targetUser的socketChannel
                Server.clients.map.get(targetUser).write(wBuffer).get();
            } catch (Exception e) {
                Server.clients.removeByValue(client);
            }
        }

        /**
         * 对公聊信息的转播
         * @param client
         * @param str
         */
        public void dispatch(AsynchronousSocketChannel client, String str) {
            try {
                // 先拿到真正的信息
                String publicMsg =getRealMsg(str);
                Set<AsynchronousSocketChannel> valueSet = Server.clients.getValueSet();
                for (AsynchronousSocketChannel cli : valueSet) { // 循环遍历每一个客户的SocketChannel，发送信息
                    wBuffer.clear();
                    wBuffer.put( charset.encode(Server.clients.getKeyByValue(client) + " say: " + publicMsg));
                    wBuffer.flip();
                    cli.write(wBuffer).get();
                }
            } catch (Exception e1) {
                Server.clients.removeByValue(client);
            }
        }

        // 去除协议字符的方法
        private String getRealMsg(String lines) {
            return lines.substring(ChatRoomProtocol.PROTOCOL_LEN, lines.length()-ChatRoomProtocol.PROTOCOL_LEN);
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.init();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
