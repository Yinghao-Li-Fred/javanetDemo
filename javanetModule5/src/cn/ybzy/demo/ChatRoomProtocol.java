package cn.ybzy.demo;

public interface ChatRoomProtocol {
    // 定义一个协议字符串的长度
    int PROTOCOL_LEN = 4;
    String PUBLICMSG_ROUND = "[pm]"; // 公聊消息的前后缀
    String USER_ROUND = "[us]"; // 用户名称的前后缀
    String LOGIN_SUCCESS = "1"; // 登录成功的前后缀
    String NAME_REP = "-1"; // 后面在客户端发送信息前，要求输入用户名，重复了返回这个标记
    String PRIVATEMSG_ROUND = "sm"; // 私聊信息前后缀
    String SPLIT_SIGN = "----"; // 信息的分割标记

}
