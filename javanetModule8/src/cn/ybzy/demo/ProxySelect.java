package cn.ybzy.demo;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ProxySelect {
    private final String PROXY_ADDR = "119.57.108.53";
    private final int PROXY_PORT = 53281;
    private void init() throws Exception{
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                List<Proxy> list = new ArrayList<>();
                list.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("119.57.108.53", 53281)));
                list.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("27.191.234.69", 9999)));
                list.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("119.57.108.53", 53281)));
                return list;
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                System.out.println("error!");
            }
        });
        // 后面的上网行为都会使用上面的代理服务器
        URL url = new URL("http://www.baidu.com");
        URLConnection conn = url.openConnection();
        Scanner scanner = new Scanner(conn.getInputStream());
        while (scanner.hasNextLine()) {
            System.out.println(scanner.nextLine());
        }
    }
}
