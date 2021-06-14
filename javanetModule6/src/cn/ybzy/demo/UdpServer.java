package cn.ybzy.demo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class UdpServer { // 服务端用来接收数据，要绑定端口号
    public static void main(String[] args) {
        // 首先拿到套接字
        try(DatagramSocket socket = new DatagramSocket(new InetSocketAddress("127.0.0.1", 30000));) {
            // 定义接收和发送数据的Packet
            byte[] inBuffer = new byte[4096];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            // 定义发数据的集装箱
            DatagramPacket outPacket;
            // 先收数据
            while   (socket.isClosed() == false) { // 不停地接收客户端的信息
                socket.receive(inPacket);
                System.out.println(new String(inBuffer, 0, inPacket.getLength()));
                // 从接收到的数据里拿到发送数据的地址
                SocketAddress clientAddr = inPacket.getSocketAddress();
                // 按原路把数据返回
                byte[] sendData = "The server received the message".getBytes();
                outPacket = new DatagramPacket(sendData, sendData.length, clientAddr);
                socket.send(outPacket);
            }
        } catch (Exception e) {

        }
    }
}
