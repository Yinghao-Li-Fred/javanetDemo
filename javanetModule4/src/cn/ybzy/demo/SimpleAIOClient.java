package cn.ybzy.demo;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

public class SimpleAIOClient {
    public static void main(String[] args) {
        try (AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open();) {
            // 定义读数据的缓冲区
            ByteBuffer rBuffer = ByteBuffer.allocate(1024);
            Charset charset = Charset.forName("UTF-8"); // 定义解码方式
            // 连接服务器
            Future<Void> future = socketChannel.connect(new InetSocketAddress("127.0.0.1", 6500)); // 此connect方法不阻塞
            // 客户端成功连接服务器以后才能往下操作，人为阻塞
            // while (!future.isDone());
            future.get(); // 人为阻塞
            // 连接上了，就可以从通道里边拿数据了
            rBuffer.clear();
            Future fu = socketChannel.read(rBuffer); // 此方法不阻塞，需要人为阻塞
            fu.get(); // 调用get方法阻塞程序
            // 阻塞完了，表示数据从通道读取到缓冲区完成
            rBuffer.flip();
            //String content = String.valueOf(charset.decode(rBuffer));
            String content = charset.decode(rBuffer).toString();
            System.out.println("The server send this message: " + content);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
