package cn.ybzy.demo;

import java.net.*;
import java.util.Scanner;

public class ProxyDemo {
    private final String PROXY_ADDR = "119.57.108.53";
    private final int PROXY_PORT = 53281;
    private void init() throws Exception {
        URL url = new URL("http://www.google.com");
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_ADDR, PROXY_PORT));
        URLConnection conn = url.openConnection(proxy);
        Socket socket = new Socket(proxy);
        Scanner scan = new Scanner(conn.getInputStream()); // 获取连接的数据
        while (scan.hasNextLine()) {
            System.out.println(scan.nextLine());
        }
    }

    public static void main(String[] args) throws Exception {
        new ProxyDemo().init();
    }

}
