package cn.ybzy.demo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.util.Scanner;

public class UdbClient {
    public static void main(String[] args) {
        // 创建套接字
        try(DatagramSocket socket = new DatagramSocket();) { // 客户端发送数据，不传参就是随机使用本地端口
            // 定义两个用来收发数据的集装箱
            byte[] inbuffer = new byte[4096];
            DatagramPacket inPacket = new DatagramPacket(inbuffer, inbuffer.length); // 带地址的是用来发的，不带地址的是用来收的，在收数据的集装箱上面指定的长度，是每次收数据的报包大小上限
            // 预定义一个发数据的集装箱
            DatagramPacket outPacket = new DatagramPacket(new byte[0], 0, new InetSocketAddress("127.0.0.1", 30000)); // 发数据的集装箱，后面要有目标地址
            // 从键盘上获取数据，然后填进发送数组
            Scanner sc = new Scanner(System.in);
            while (sc.hasNextLine()) {
                byte[] dates = sc.nextLine().getBytes();
                outPacket.setData(dates); // 把datas数组塞进outPacket发送集装箱
                socket.send(outPacket);
                // 服务器端收到我发送的数据，服务器应该可以沿着去的路，给我回应一个信息回来
                socket.receive(inPacket);
                System.out.println("The server sent this message: " + new String(inbuffer, 0, inPacket.getLength()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
