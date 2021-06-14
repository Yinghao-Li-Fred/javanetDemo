package cn.ybzy.demo;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class Demo2 {
    public static void main(String[] args) {
        try {
            String urlEncode = URLEncoder.encode("李英豪", "UTF-8");
            System.out.println(urlEncode);

            String urlDecode = "%E6%9D%8E%E8%8B%B1%E8%B1%AA";
            System.out.println(URLDecoder.decode(urlDecode, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
