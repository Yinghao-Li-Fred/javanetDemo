package cn.ybzy.demo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class Demo6 {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket();
//            ServerSocket serverSocket1 = new ServerSocket(5000); // 绑定本机的端口号5000
//            ServerSocket serverSocket2 = new ServerSocket(5000, 60); // 队列大小： 60,可以同时接收的连接队列
//            ServerSocket serverSocket3 = new ServerSocket(5000, 60, InetAddress.getByName("127.0.0.1"));
            serverSocket.bind(new InetSocketAddress("127.0.0.1", 5000));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
