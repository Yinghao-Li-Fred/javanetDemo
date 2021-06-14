package cn.ybzy.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 * 负责获取和显示从服务器那里发送来的信息
 */
public class ClientThread implements Runnable{
    private BufferedReader in = null;
    public ClientThread(BufferedReader in) {
        this.in = in;
    }
    @Override
    public void run() {
       try {
           // 实现获取和显示从服务器那里发送来的信息
           String content = null;
           // 用循环，不断地获取
           while((content=in.readLine())!=null) {
               System.out.println(content);
           }
       } catch (Exception e) {
           e.printStackTrace();
       } finally {
           try {
               in.close();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
    }
}
