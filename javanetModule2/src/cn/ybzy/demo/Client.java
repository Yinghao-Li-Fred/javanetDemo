package cn.ybzy.demo;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    private static final int SERVER_PORT = 40000;
    private Socket socket = null;
    private PrintStream ps = null;
    private BufferedReader inServer = null;
    private BufferedReader inKey = null;

    /**
     * 客户端连接服务器的功能，并且实现用户登录
     */
    public void init() {
        try {
            // 首先初始化键盘输入流
            inKey = new BufferedReader(new InputStreamReader(System.in, "GBK"));
            // 连接到服务器
            socket = new Socket("127.0.0.1", SERVER_PORT);
            // 获取socket对应的输入输出流
            ps = new PrintStream(socket.getOutputStream(), true, "GBK");
            inServer = new BufferedReader(new InputStreamReader(socket.getInputStream(), "GBK"));
            // 用一个循环来进行服务器的登录
            String tip = "";
            while (true) {
                // 使用GUI，一个弹出的对话框
                String userName = JOptionPane.showInputDialog(tip + "Username: ");
                // 把用户输入的用户名发送给服务器
                ps.println(ChatRoomProtocol.USER_ROUND + userName + ChatRoomProtocol.USER_ROUND);
                // 发送给服务器以后，紧接着获取服务器的响应，如果用户名重复，返回-1
                String res = inServer.readLine();
                if (res.equals(ChatRoomProtocol.NAME_REP)) {
                    tip = "Repeated Username, please login again,";
                    continue;
                }
                if (res.equals(ChatRoomProtocol.LOGIN_SUCCESS)) {
                    break;
                }
            }
        } catch(UnknownHostException e1) {
            System.out.println("Unable to find the server, please confirm the server is up!");
            closeRes();
            System.exit(1);// 系统退出
        } catch (IOException e2) {
            System.out.println("network error!");
            closeRes();
            System.exit(1);
        }
        // 登录上服务器后，获取服务器的响应信息，在控制台上显示
        new Thread(new ClientThread(inServer)).start();
    }

    /**
     * 客户端获取键盘上的信息并且发送给服务器的功能
     */
    private void readAndSend() {
       try {
           // 通过循环不断地获取键盘上的信息，包装发送
           String line = null;
           while ((line=inKey.readLine()) != null) {
               // 对line的内容进行判断，发送的是私聊信息，还是公聊信息
               // 规定：发送的信息如果有冒号,并且是以//开头，表示私聊信息
               if (line.indexOf(":")>0 && line.startsWith("//")) { // 私聊信息
                   // 信息中带有目标客户和信息
                   line = line.substring(2); // 首先，去掉前面两个//
                   ps.println(ChatRoomProtocol.PRIVATEMSG_ROUND + line.split(":")[0] + ChatRoomProtocol.SPLIT_SIGN + line.split(":")[1] + ChatRoomProtocol.PRIVATEMSG_ROUND);// //用户名:........
               } else { // 公聊信息
                   ps.println(ChatRoomProtocol.PUBLICMSG_ROUND + line + ChatRoomProtocol.PUBLICMSG_ROUND);
               }
           }
       } catch (IOException e) {
           System.out.println("network communication error!");
           closeRes();
           System.exit(1);
       }
    }

    /**
     * 对资源进行关闭的功能
     */
    private void closeRes() {
        try {
            inKey.close();
            inServer.close();
            ps.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.init();
        client.readAndSend();
        client.closeRes();
    }
}
