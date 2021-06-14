package cn.ybzy.demo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Demo5 {
    public static void main(String[] args) throws Exception {
        String urlPath = "http://localhost/demo/postIndex.html";
        // 要提交给服务器的数据
        String param = "name=" + URLEncoder.encode("Fred", "UTF-8");
        // 建立连接
        URL url = new URL(urlPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // 给连接对象设置参数
        conn.setDoOutput(true); // 使用post请求，必须有该设置项
        conn.setUseCaches(false); // 设置浏览器不使用缓存
        conn.setRequestMethod("POST"); // 设置缓存方式
        conn.setRequestProperty("Charset", "UTF-8"); // 设置请求头信息
        conn.setRequestProperty("Connection","Keep-Alive"); // 设置TCP的连接模式
        conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded"); // 提交数据的编码方式
        // 获取到输出流，才能把param放到请求体
        conn.connect();
        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
        dos.writeBytes(param); // 这里写到输出流，只是把数据放到内存中的缓冲区里，并没有发出去
        dos.flush(); // 真正写到报文里
        dos.close();
        // 获取服务器响应回来的信息
        int resultCode = conn.getResponseCode();
        if (resultCode == HttpURLConnection.HTTP_OK) { // 200
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line = null;
            while ((line = reader.readLine())!=null) {
                System.out.println(line);
            }
        }
    }
}
