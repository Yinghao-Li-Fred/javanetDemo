package cn.ybzy.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class UdpNIOServer {
    // 定义两个缓冲区，发数据和收数据
    private ByteBuffer inBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer outBuffer = ByteBuffer.allocate(1024);

    // 定义一个初始化方法
    public void init() {
        // TCP: ServerSocketChannel
        // UDP: DataProgramChannel
        try {
            DatagramChannel datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false); // 设置为非阻塞模式
            // 服务器端接收数据，要绑定一个端口号
            datagramChannel.bind(new InetSocketAddress("127.0.0.1", 40000));
            // 选择器
            Selector selector = Selector.open();
            // 将通道注册进选择器
            datagramChannel.register(selector, SelectionKey.OP_READ);
            // 通过循环不断地接收和收发数据
            while (true) {
                int count = selector.select(5000); // 连接超时时间：5秒
                if (count == 0) continue;
                Iterator<SelectionKey> it = selector.selectedKeys().iterator(); // 拿到选择就绪的通道的集合进行遍历
                while (it.hasNext()) {// 遍历集合里的每一个数据项
                    SelectionKey key = it.next();
                    if (key.isReadable()) {
                        handleread(key);
                    }
                    selector.selectedKeys().remove(key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleread(SelectionKey key) {
        try {
            DatagramChannel datagramChannel = (DatagramChannel) key.channel(); // 拿到key这个通道
            inBuffer.clear(); // 读缓冲区清空
            InetSocketAddress sendAddr = (InetSocketAddress) datagramChannel.receive(inBuffer);
            inBuffer.flip();
            System.out.println("客户端发来的信息：" + StandardCharsets.UTF_8.decode(inBuffer).toString());
            // 给客户端回应信息
            String content = "服务器收到";
            outBuffer.clear(); // 写缓冲区
            outBuffer.put(content.getBytes("UTF-8"));
            outBuffer.flip();
            datagramChannel.send(outBuffer, new InetSocketAddress(sendAddr.getHostName(), sendAddr.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        UdpNIOServer server = new UdpNIOServer();
        server.init();
    }
}
