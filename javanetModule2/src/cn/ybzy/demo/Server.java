package cn.ybzy.demo;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {
    // 服务器监听的端口号
    public static final int SERVER_PORT = 40000;
    // 把原来的list换成我们自己定义的新的数据结构，来保存连接进来的所有客户端，前面保存的是socket对象，、
    // 现在我们保存的是代表客户端的用户名称和对应的Socket关联的输出流
    public static ChatRoomMap<String, PrintStream> clients = new ChatRoomMap<>();
    // 绑定IP地址和端口的启动服务器的代码，封装起来init方法
    public void init() {
        try {
            // 第一步：创建ServerSocket
            ServerSocket serverSocket = new ServerSocket(); // 无参数，表示没有连接的socket
            serverSocket.bind(new InetSocketAddress("127.0.0.1", SERVER_PORT));
            // 用一个循环来不断的接收客户端的连接
            while (true) {
                // 接收客户端的连接请求，获取连接进来的客户端的套接字Socket
                Socket clientSocket = serverSocket.accept(); // 此方法会阻塞：直到有客户端连进来后才会往下执行，会返回一个与连进来的客户端一一对应的Socket
                // 不能像前面一样，单线程去连接进来的客户端，效率太低，我们要用多线程来处理，进来一个客户端就分配一条线程
                new Thread(new ServerThread(clientSocket)).start();
            }
        } catch (Exception e) {
            System.out.println("server start up failed, port number may be occupied: " + SERVER_PORT);
        }
    }
    public static void main(String[] args) {
        Server server = new Server();
        server.init();
    }
}
