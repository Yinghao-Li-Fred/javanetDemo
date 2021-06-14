package cn.ybzy.demo;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Demo3 {
    public static void main(String[] args) {
        try {
            URL url = new URL("http://14.215.177.39/");
            System.out.println(url.getProtocol());
            System.out.println(url.getPort());
            System.out.println(url.getHost());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 接下来，正式和http://14.215.177.39/对应的服务器建立TCP连接
            conn.connect();
            // 发起GET HTTP请求
            InputStream in = conn.getInputStream(); // 这个方法会向对应服务器发起HTTP请求，服务器收到请求后会给我们一个HTTP响应，响应包含：响应头，响应正文
                                                    // 正文部分就包含在getInputStream方法返回的in中
            byte[] buffer = new byte[1024];
            int hasRead = 0;
            while ((hasRead=in.read(buffer))!=-1) {
                System.out.println(new String(buffer, 0, hasRead));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
