package cn.ybzy.demo;

import javax.swing.*;
import java.io.IOException;
import java.net.*;
import java.nio.channels.MulticastChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ComUtil {
    public static final String CHARSET = "UTF-8";
    private static final String BROADCAST_IP = "230.0.0.1";
    public static final int BROADCAST_PORT =30000;
    private static final int DATA_LEN = 4096;
    private MulticastSocket socket = null;
    private DatagramSocket singleSocket = null;
    private InetAddress broadcastAddress = null;
    byte[] inBuff = new byte[DATA_LEN];
    private DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length);
    private DatagramPacket outPacket = null;
    private LanTalk lanTalk;

    public ComUtil(LanTalk lanTalk) throws Exception{
        this.lanTalk = lanTalk;
        socket = new MulticastSocket(BROADCAST_PORT); // 创建用于发送，接收数据的MulticastSocket对象，因为该MulticastSocket对象需要接收，所以有指定端口
        singleSocket = new DatagramSocket(BROADCAST_PORT+1); // 创建私聊用的DatagramSocket对象
        broadcastAddress = InetAddress.getByName(BROADCAST_IP);
        socket.joinGroup(broadcastAddress); // 将该socket加入指定的多点广播地址
        socket.setLoopbackMode(false); // 设置本MulticastSocket发送的数据报被回送到自身
        outPacket = new DatagramPacket(new byte[0], 0, broadcastAddress, BROADCAST_PORT); // 初始化发送用的DatagramSocket，它包含一个长度为0的字节数组
        new ReadBroad().start();
        Thread.sleep(1);
        new ReadSingle().start();  // 启动两个读取网络数据的线程
    }

    // 实现广播信息，广播消息的工具方法
    public void broadCast(String msg) {
        try {
            byte[] buff = msg.getBytes(CHARSET); // 将msg字符串转换成字节数组
            outPacket.setData(buff); // 设置发送用的DatagramPacket里的字节数据
            socket.send(outPacket); // 发送数据报
        } catch (Exception e) {
            e.printStackTrace();
            if (socket!=null) {
                socket.close();
            }
            JOptionPane.showMessageDialog(null, "发送信息异常，请确认30000端口空闲，且网络连接正常！", "网络异常", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // 发送私聊信息
    public void sendSingle(String msg, SocketAddress dest) {
        try {
            // 将msg字符串转换字节数组
            byte[] buff = msg.getBytes(CHARSET);
            DatagramPacket packet = new DatagramPacket(buff, buff.length, dest);
            singleSocket.send(packet);
        } catch (IOException e) {
            singleSocket.close();
            JOptionPane.showMessageDialog(null, "发送信息异常，请确认30001端口空闲，且网络连接正常！", "网络异常", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // 接收广播信息的线程类
    class ReadBroad extends Thread {
        @Override
        public void run() {
            // 用一个循环，不停地接收广播信息
            while (true) {
                try {
                    // 读取socket中的数据
                    socket.receive(inPacket);
                    // 从socket中读取的内容
                    String msg = new String(inBuff, 0, inPacket.getLength(), CHARSET);
                    // msg有类型的，用户信息，公聊聊天信息
                    if (msg.startsWith(ChatProtocol.PRESENCE) && msg.endsWith(ChatProtocol.PRESENCE)) { // 用户信息
                        String userMsg = msg.substring(2, msg.length()-2);
                        String[] userInfo = userMsg.split(ChatProtocol.SPLITER);
                        UserInfo user = new UserInfo(userInfo[1], userInfo[0], inPacket.getSocketAddress(), 0);
                        // 判断是否需要添加该用户的标记，如果该用户已经存在于用户列表中，不添加
                        boolean addFlag = true;
                        // 下面的集合里要放的数据是，后面马上要从列表中删除的用户数据
                        ArrayList<Integer> delList =new ArrayList<>();
                        // 循环遍历所有在列表中的用户
                        for (int i=1; i<lanTalk.getUserNum(); i++) {
                            UserInfo current = lanTalk.getUser(i);
                            // 将所有用户失去联系的次数加1
                            current.setLost(current.getLost() + 1);
                            if (current.equals(user)) { // 收到的广播信息对应的用户已经在列表中
                                current.setLost(0);
                                addFlag = false;
                            }
                            if (current.getLost() > 2) {
                                delList.add(i); // 此客户可能已经离线
                            }
                        }
                        // 删除delList中的所有索引对应的用户
                        for (int i=0; i<delList.size(); i++) {
                            lanTalk.removeUser(delList.get(i));
                        }
                        if (addFlag) { // 添加新用户
                            lanTalk.addUser(user);
                        }
                    } else { // 公聊
                        lanTalk.processMsg(inPacket, false);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 接收私聊信息的线程类
    class ReadSingle extends Thread {
        byte[] singleBuff = new byte[DATA_LEN];
        private DatagramPacket singlePacket = new DatagramPacket(singleBuff, singleBuff.length);
        @Override
        public void run() {
            while (true) {
                try {
                    singleSocket.receive(singlePacket);
                    lanTalk.processMsg(singlePacket, true);
                } catch (IOException e) {
                    singleSocket.close();
                    JOptionPane.showMessageDialog(null, "接收信息异常，请确认30001端口空闲，且网络连接正常!", "网络异常", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        }
    }





}
