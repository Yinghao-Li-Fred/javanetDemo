package cn.ybzy.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.net.InetSocketAddress;

public class ChatFrame extends JDialog {
    // 聊天信息区
    JTextArea msgArea = new JTextArea(12, 45);
    // 聊天输入区
    JTextField chatField = new JTextField(30);
    // 发送聊天信息的按钮
    JButton sendBn = new JButton("发送");
    // 该交谈窗口对应的用户
    UserInfo user;
    // 构造器，用于初始化交谈对话框的界面
    public ChatFrame(LanTalk parent, final UserInfo user) {
        super(parent, "和" + user.getName() + "聊天中", false);
        this.user = user;
        msgArea.setEditable(false);
        add(new JScrollPane(msgArea));
        JPanel buttom = new JPanel();
        buttom.add(new JLabel("输入信息："));
        buttom.add(chatField);
        buttom.add(sendBn);
        add(buttom, BorderLayout.SOUTH);
        Action sendAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 点击发送按钮，执行这个方法，发送的信息有两类：公聊私聊
                InetSocketAddress dest = (InetSocketAddress) user.getAddress();
                if (dest == null) { // 公聊信息，广播 MulticastSocket.send
                    LoginFrame.comUtil.broadCast(chatField.getText());
                    msgArea.setText("您对大家说：" + chatField.getText() + "/n" + msgArea.getText());
                } else { // 私聊信息，对UDP协议的支持DatagramSocket.send
                    // 获取发送消息的目的
                    dest = new InetSocketAddress(dest.getHostName(), dest.getPort()+1);
                    LoginFrame.comUtil.sendSingle(chatField.getText(), dest);
                    addString("您对" + user.getName() + "说：" + chatField.getText());

                }
            }
        };
        // 将按钮和上面写的行为关联起来
        sendBn.addActionListener(sendAction);
        // Ctrl+Enter快捷键
        chatField.getInputMap().put(KeyStroke.getKeyStroke('\n', InputEvent.CTRL_DOWN_MASK), "send");
        // 将send与sendAction关联
        chatField.getActionMap().put("send", sendAction);
        pack();
    }

    public void addString(String msg) {
        msgArea.setText(msg + "/n" + msgArea.getText());
    }
}
