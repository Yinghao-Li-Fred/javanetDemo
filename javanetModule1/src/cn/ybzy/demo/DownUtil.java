package cn.ybzy.demo;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownUtil {  // 工具类，多线程的方式实现文件下载
    // 首先，需要知道要下载的目标文件的URL
    private String urlPath;
    // 文件下载后，确定保存文件的位置
    private String targetFile;
    // 确定下载用几条线程
    private int threadNum;
    // 定义一个数组，放的是下载的线程类
    private DownThread[] threads;
    // 把用于下载的线程类定义为工具里的内部类

    private class DownThread extends Thread {
        // 线程类里的具体的东西：连接上远程文件，然后把文件划分为几块，让线程分别去实现下载
        private int startPos;
        private int currentPartSize;
        private RandomAccessFile currentPart;
        private int length; // 记录每条线程已经下载下来的文件的字节数
        public DownThread(int startPos, int currentPartSize, RandomAccessFile currentPart) {
            this.startPos = startPos;
            this.currentPartSize = currentPartSize;
            this.currentPart = currentPart;
        }
        @Override
        public void run() {
            try {
                // 真正实现下载
                // 第一步：连接上目标文件
                URL url = new URL(urlPath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // 是从web服务器上获取文件
                // 真正去连接之前，要设置连接头部信息
                conn.setConnectTimeout(5000); // 设置连接超时时间：5s
                conn.setRequestMethod("GET"); // 设置连接目标的请求方法
                conn.setRequestProperty("Accept", "*/*");// 设置头部信息：允许客户端处理所有文件
                conn.setRequestProperty("Accept-Language", "zh-CN"); // 设置语言环境
                conn.setRequestProperty("Charset", "UTF-8"); // 字符集
                conn.setRequestProperty("Connection", "Keep-Alive"); // 设置连接模式
                // 前面设置这么多东西，一个目的：取得目标文件大小
                conn.connect(); //连接上目标，可写可不写
                // 第二步：把目标文件数据拿下来，放到输入流里
                InputStream in = conn.getInputStream();
                // 从输入流里拿数据
                // 把in的指针，跳到该线程负责下载的位置
                in.skip(this.startPos);
                // 跳到这个位置后，从输入流里获取数据
                byte[] buffer = new byte[1024];
                int hasRead = 0;
                while (length<currentPartSize && (hasRead=(in.read(buffer))) != -1) {
                    // 写到当前文件块
                    currentPart.write(buffer, 0, hasRead);
                    length += hasRead;
                }
                currentPart.close();
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 保存一下目标文件的大小
    private int fileSize;
    public DownUtil() {}
    public DownUtil(String urlPath, String targetFile, int threadNum) {
        this.urlPath = urlPath;
        this.targetFile = targetFile;
        this.threadNum = threadNum;
        this.threads = new DownThread[threadNum];
    }
    // 定义一个实现文件下载的方法
    public void download() throws Exception {
        // 第一件事：连接到目标文件，获取目标文件大小
        URL url = new URL(urlPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // 是从web服务器上获取文件
        // 真正去连接之前，要设置连接头部信息
        conn.setConnectTimeout(5000); // 设置连接超时时间：5s
        conn.setRequestMethod("GET"); // 设置连接目标的请求方法
        conn.setRequestProperty("Accept", "*/*");// 设置头部信息：允许客户端处理所有文件
        conn.setRequestProperty("Accept-Language", "zh-CN"); // 设置语言环境
        conn.setRequestProperty("Charset", "UTF-8"); // 字符集
        conn.setRequestProperty("Connection", "Keep-Alive"); // 设置连接模式
        // 前面设置这么多东西，一个目的：取得目标文件大小
        conn.connect(); //连接上目标，可写可不写
        fileSize = conn.getContentLength(); // 获取目标大小
        conn.disconnect(); // 用了以后，及时关闭资源通道
        // 根据文件的大小和下载线程数量，来进行目标文件切块
        // 先在本地创建一个和目标文件同样大小的文件
        RandomAccessFile file = new RandomAccessFile(targetFile, "rw");
        file.setLength(fileSize);
        file.close(); // 打开磁盘上的文件资源要及时关掉
        // 每个线程负责下载的文件块的大小
         int currentPartSize = fileSize/threadNum + 1;
        // 实现切块
        for (int i=0; i<threadNum; i++) {
            // 设置每个线程下载的文件块的开始位置
            int startPos = i*currentPartSize;
            // 让每个线程使用一个RandomAccessFile的对象来进行下载
            RandomAccessFile currentPart = new RandomAccessFile(targetFile, "rw");// 每个线程负责下载的文件块
            // 每个文件块的指针，移动到每个线程起始的下载位置
            currentPart.seek(startPos);
            // 创建线程，真正负责下载
            threads[i] = new DownThread(startPos, currentPartSize, currentPart);
            // 让线程启动，开始下载
            threads[i].start();
        }
    }
    // 实现下载百分比进度条
    public double getCompleteRate() {
        // 定义一个变量
        int sumSize = 0;
        for (int i=0; i<threadNum; i++) {
            sumSize += threads[i].length;
        }
        return sumSize*1.0/fileSize; // 已下载下来的文件的百分比
    }
}
