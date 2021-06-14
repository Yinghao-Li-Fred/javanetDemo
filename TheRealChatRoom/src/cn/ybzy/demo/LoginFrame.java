package cn.ybzy.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class LoginFrame extends JDialog { // 登录弹框
    public JLabel tip;
    public JTextField userField = new JTextField(20);
    public JComboBox<Integer> iconList = new JComboBox<>(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
    private JButton loginBn = new JButton("登录");
    // 聊天的主界面
    private LanTalk lanTalkFrame;
    // 聊天通信的工具实例
    public static ComUtil comUtil;

    public LoginFrame(LanTalk parent, String msg) {
        super(parent, "输入名字后登录", true);
        this.lanTalkFrame = parent;
        setLayout(new GridLayout(5, 1)); // 设置Dialog的布局，表格布局，5行1列
        JPanel jp = new JPanel(); // 面板
        tip = new JLabel(msg);
        tip.setFont(new Font("Serif", Font.BOLD, 16)); // 设置tip字体
        jp.add(tip); // 把tip放到面板里
        add(jp); // 把面板放到dialog里
        add(getPanel("用户名", userField));
        iconList.setPreferredSize(new Dimension(224, 20));
        add(getPanel("图 标", iconList));
        JPanel bp = new JPanel();
        bp.add(loginBn);
        loginBn.addActionListener(new MyActionListener(this)); // 给按钮添加鼠标事件
        add(bp);
        pack();
        Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize(); // 拿到屏幕尺寸
        int x = (int) screensize.getWidth()/2 - this.getWidth()/2;
        int y = (int) screensize.getHeight()/2 - this.getHeight()/2;
        this.setLocation(x, y); // 使弹框显示在屏幕中央
        this.setVisible(true);
    }

    private JPanel getPanel(String name, JComponent jf) {
        JPanel jp = new JPanel();
        jp.add(new JLabel(name + ":"));
        jp.add(jf);
        return jp;
    }

    public void setTipMsg(String msg) {
        this.tip.setText(msg);
    }

    /**
     * 点击按钮以后，程序要做的功能，实现登录
     */
    private class MyActionListener implements ActionListener {
        private LoginFrame loginFrame;
        public MyActionListener(LoginFrame loginFrame) {
            this.loginFrame = loginFrame;
        }
        @Override
        public void actionPerformed(ActionEvent e) { // 按按钮以后会触发这个方法
            try {
                // 此例子中的登录：广播新用户上线了
                // 初始化聊天通信类
                comUtil = new ComUtil(lanTalkFrame);
                final String loginMsg = ChatProtocol.PRESENCE + userField.getText() + ChatProtocol.SPLITER + iconList.getSelectedObjects()[0] + ChatProtocol.PRESENCE;
                comUtil.broadCast(loginMsg);
                // 做一个定时器，每隔10秒钟，重新广播一次
                javax.swing.Timer timer = new javax.swing.Timer(1000 * 10, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        comUtil.broadCast(loginMsg);
                    } // UDP协议不可靠，多广播几次
                });
                timer.start();
                loginFrame.setVisible(false);
                lanTalkFrame.setVisible(true);
            } catch (Exception ex) {
                loginFrame.setTipMsg("确认30001端口空闲，且网络正常！");
            }
        }
    }

}
