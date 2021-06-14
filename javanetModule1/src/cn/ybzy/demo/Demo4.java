package cn.ybzy.demo;

public class Demo4 {
    public static void main(String[] args) throws Exception {
        DownUtil downUtil = new DownUtil("https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=2496571732,442429806&fm=26&gp=0.jpg", "D:\\aaa.jpg", 5);
        downUtil.download();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (downUtil.getCompleteRate() <= 1) {
                    System.out.println("download completed: " + downUtil.getCompleteRate());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
