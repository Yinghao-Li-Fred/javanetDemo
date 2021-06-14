package cn.ybzy.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private static final int SERVER_PORT = 31000;
    private static final String CHARSET = "UTF-8";
    private Charset charset = Charset.forName(CHARSET);
    // 与服务器连接的通道
    AsynchronousSocketChannel clientChannel;
    // 用GUI定义一个主窗体
    JFrame mainWin = new JFrame("Group Chat Room");
    // 定义显示聊天内容的文本域
    JTextArea jta = new JTextArea(16, 48);
    // 定义输入聊天内容的文本框
    JTextField jtf = new JTextField(40);
    // 定义发送聊天内容的按钮
    JButton sendBtn = new JButton("Send");
    // login的tip
    String tip = "";
    // 写缓冲区
    ByteBuffer wBuffer = ByteBuffer.allocate(1024);
    // 读缓冲区
    ByteBuffer rBuffer = ByteBuffer.allocate(1024);

    // 上面皮肤，整个客户端程序的初始化
    public void init() {
        mainWin.setLayout(new BorderLayout());
        jta.setEditable(false);
        mainWin.add(new JScrollPane(jta), BorderLayout.CENTER);
        JPanel jp = new JPanel();
        jp.add(jtf);
        jp.add(sendBtn);
        // 按钮要定义点击之后的事件响应
        Action sendAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = jtf.getText();
                content = content.trim(); // 把content里边的空格去掉
                if(content.length() > 0) {
                    try {
                        if (content.indexOf(":") > 0 && content.startsWith("//")) { // 判断是公聊信息还是私聊信息
                            // 私聊信息
                            content = content.substring(2); // 去掉开头的两个双斜杠
                            wBuffer.clear();
                            wBuffer.put(charset.encode(ChatRoomProtocol.PRIVATEMSG_ROUND + content.split(":")[0] + ChatRoomProtocol.SPLIT_SIGN + content.split(":")[1] + ChatRoomProtocol.PRIVATEMSG_ROUND));
                            wBuffer.flip();
                            clientChannel.write(wBuffer).get(); // 异步不能使用while循环
                        } else {
                            // 公聊信息
                            wBuffer.clear();
                            wBuffer.put(charset.encode(ChatRoomProtocol.PUBLICMSG_ROUND + content + ChatRoomProtocol.PUBLICMSG_ROUND));
                            wBuffer.flip();
                            clientChannel.write(wBuffer).get();
                        }
                    } catch (Exception ex) {
                        System.out.println("data send error!");
                    }
                }
                // 把发送出去的信息，从文本框中清楚
                jtf.setText("");
            }
        };
        // 把事件和按钮关联起来
        sendBtn.addActionListener(sendAction);
        // 定义一个快捷键组合Ctrl+Enter发送信息
        jtf.getInputMap().put(KeyStroke.getKeyStroke('\n', InputEvent.CTRL_MASK), "send"); // '\n'表示回车建，CTRL_MASK表示Control 键
        // 上面定义了一个快捷键，把快捷键和发送信息的事件响应关联起来
        jtf.getActionMap().put("send", sendAction);
        mainWin.add(jp, BorderLayout.SOUTH);
        // 给主窗体设置关闭按钮 X
        mainWin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainWin.pack(); // 窗体自动调整大小，根据包裹在里面的组件自动调整到合适的大小
        mainWin.setVisible(true); // 主窗体在界面上显示出来
    }

    // 连接服务器
    public void connect() {
        try {
            // 定义线程池
            ExecutorService executorService = Executors.newFixedThreadPool(80);
            // 定义channelGroup
            AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            // 获取客户端的套接字通道，因为我们用了GUI，客户端套接字应该保存到上面的属性上去
            clientChannel = AsynchronousSocketChannel.open(channelGroup);
            // 连接服务器
            clientChannel.connect(new InetSocketAddress("127.0.0.1", SERVER_PORT)).get(); // 返回future，调用future的get方法阻塞连接
            System.out.println("clientSocket successfully connected to the server!");
            // 连接上服务器后，登录服务器
            login(clientChannel, tip);
            // 登录成功以后，接收服务器传来的信息
            rBuffer.clear();
            clientChannel.read(rBuffer, null, new CompletionHandler<Integer, Object>() { // 异步模式，通知操作系统来读
                @Override
                public void completed(Integer result, Object attachment) {
                    // 进到这里就表示读出来了
                    // 从缓冲区里把数据读出来
                    rBuffer.flip();
                    String content = charset.decode(rBuffer).toString();
                    // content类型：登陆后响应信息，聊天信息
                    if (content.startsWith(ChatRoomProtocol.USER_ROUND) && content.endsWith(ChatRoomProtocol.USER_ROUND)) { // 如果取到的信息是登录回复
                        // 拿到真正的登录回复信息
                        String loginRes = getRealMsg(content);
                        if (loginRes.equals(ChatRoomProtocol.NAME_REP)) { // 如果登录失败
                            tip = "Username already exists, please login again!";
                            login(clientChannel, tip); // 登录失败，重新登录
                        } else if (loginRes.equals(ChatRoomProtocol.LOGIN_SUCCESS)) {
                            System.out.println( "login success!");
                        }
                    } else { // 公聊信息或私聊信息，回来的是聊天信息，打印到GUI窗口里
                        jta.append(content + "\n");
                    }
                    // 客户端要不停地读取服务端发过来的信息
                    rBuffer.clear();
                    clientChannel.read(rBuffer, null, this); // 递归回调自己
                }
                @Override
                public void failed(Throwable exc, Object attachment) {
                    System.out.println("data reading failed!" + exc); // exc 异常信息
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 去除协议字符的方法
    private String getRealMsg(String lines) {
        return lines.substring(ChatRoomProtocol.PROTOCOL_LEN, lines.length()-ChatRoomProtocol.PROTOCOL_LEN);
    }

    // login方法
    private void login(AsynchronousSocketChannel client, String tip) {
        try {
            // 登录弹框
            String username = JOptionPane.showInputDialog(tip + "输入用户名: ");
            // 把username发送到服务器上去
            wBuffer.clear();
            wBuffer.put(charset.encode(ChatRoomProtocol.USER_ROUND + username + ChatRoomProtocol.USER_ROUND));
            wBuffer.flip();
            client.write(wBuffer).get(); // 异步模式不可以重复通知操作系统写
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.init();
        client.connect();
    }
}
