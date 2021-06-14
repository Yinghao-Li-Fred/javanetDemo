package cn.ybzy.demo;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

public class SimpleAIOServer {
    public static void main(String[] args) {
        // 拿到异步服务器套接字通道，放在try括号里，通道自动关闭
        try (AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open();) {
             serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 6500));
             // 用循环不断地接收客户端的连接
            while (true) {
                Future<AsynchronousSocketChannel> future = serverSocketChannel.accept(); // 注意：在BIO和NIO中，此方法是阻塞程序，直到有客户端连进来后才会往下走，AIO中是异步程序
                // while (!future.isDone()); // 用while循环人为阻塞
                AsynchronousSocketChannel socketChannel = future.get(); // get方法会完成阻塞，直到操作系统完成网络IO，返回代表客户端的套接字通道
                // 这时可以和客户端通信了
                ByteBuffer wBuffer = ByteBuffer.allocate(1024);
                wBuffer.clear();
                wBuffer.put("This is the server".getBytes("UTF-8"));
                wBuffer.flip();
                socketChannel.write(wBuffer); // 在异步模式下不加循环
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
