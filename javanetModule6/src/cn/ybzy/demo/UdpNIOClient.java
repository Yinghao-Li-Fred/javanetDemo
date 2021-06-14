package cn.ybzy.demo;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class UdpNIOClient {
    public static void main(String[] args) {
        // 定义两个缓冲区
        ByteBuffer inBuffer = ByteBuffer.allocate(1024);
        ByteBuffer outBuffer = ByteBuffer.allocate(1024);
        try {
            Scanner scanner = new Scanner(System.in);
            DatagramChannel datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            while (scanner.hasNextLine()) {
                String content = scanner.nextLine();
                // 发送数据
                outBuffer.clear();
                outBuffer.put(content.getBytes("UTF-8"));
                outBuffer.flip();
                datagramChannel.send(outBuffer, new InetSocketAddress("127.0.0.1", 40000));

                inBuffer.clear();
                datagramChannel.receive(inBuffer);
                inBuffer.flip();
                System.out.println("服务器发送过来的信息：" + StandardCharsets.UTF_8.decode(inBuffer).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
