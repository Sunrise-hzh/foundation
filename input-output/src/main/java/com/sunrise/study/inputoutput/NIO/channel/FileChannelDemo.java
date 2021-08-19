package com.sunrise.study.inputoutput.NIO.channel;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * NIO 的 FileChannel 测试：
 * 1、测试读取文件内容
 * 2、测试写入文件内容
 * 3、测试复制文件
 * @author huangzihua
 * @date 2021-07-06
 */
public class FileChannelDemo {


    public static void main(String[] args) throws IOException {
//        read();
//        write();
//        copyByNIO();
        copyByStream();
    }

    /**
     * 读取文件
     * @throws IOException
     */
    public static void read() throws IOException {
        File file = new File("D:/temp/nio_test/", "file.txt");
        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            FileChannel inputChannel = fis.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int read = inputChannel.read(buffer);
            System.out.println(read);
            byte[] text = new byte[100];
            buffer.flip();
            buffer.get(text, 0, buffer.limit());
            System.out.println(new String(text, StandardCharsets.UTF_8));
            inputChannel.close();
            fis.close();
        }
    }

    public static void write() throws IOException {
        File file = new File("D:/temp/nio_test/", "file.txt");
        if (file.exists()) {
            FileOutputStream fos = new FileOutputStream(file);
            FileChannel outputChannel = fos.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            String text = "hello nio!";
            buffer.put(text.getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            int write = outputChannel.write(buffer);
            System.out.println(write);
            outputChannel.force(true);
            outputChannel.close();
            fos.close();
        }
    }

    public static void copyByNIO() throws IOException {
        long startTime = System.currentTimeMillis();
        // 源文件路径
        File srcFile = new File("D:/temp/nio_test/test.pdf");
        // copy文件路径
        File tarFile = new File("D:/temp/nio_test/test2.pdf");
        tarFile.createNewFile();    // 新建文件

        // 获取输入输出流
        FileInputStream fis = new FileInputStream(srcFile);
        FileOutputStream fos = new FileOutputStream(tarFile);

        // 获取通道
        FileChannel fisChannel = fis.getChannel();
        FileChannel fosChannel = fos.getChannel();

        // 分配buffer
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        // 首先读取
        while (fisChannel.read(buffer) != -1) {
            // 从fisChannel通道写入数据到buffer后，切换buffer为读取模式
            buffer.flip();
            // 从buffer中读取数据，写入到fosChannel通道中
            fosChannel.write(buffer);
            // 写入完成，清空buffer
            buffer.clear();
        }

        fosChannel.force(true);
        fis.close();
        fos.close();
        fis.close();
        fos.close();
        long endTime = System.currentTimeMillis();
        System.out.println("文件大小：" + (srcFile.length() / 1024) + "字节");
        System.out.println("总耗时：" + (endTime - startTime));
        System.out.println("传输速度：" + ((srcFile.length() / 1024) / (endTime - startTime) / 1000) + " kb/s");
        System.out.println("传输速度：" + ((srcFile.length() / 1024) / (endTime - startTime) / 1000 / 1024) + " M/s");
    }

    public static void copyByStream() throws IOException {
        long startTime = System.currentTimeMillis();
        // 源文件路径
        File srcFile = new File("D:/temp/nio_test/ubuntu.iso");
        // copy文件路径
        File tarFile = new File("D:/temp/nio_test/ubuntu3.iso");
        tarFile.createNewFile();    // 新建文件

        // 获取输入输出流
        FileInputStream fis = new FileInputStream(srcFile);
        FileOutputStream fos = new FileOutputStream(tarFile);

        byte[] buf = new byte[1024];

        while (fis.read(buf) != -1) {
            fos.write(buf);
        }

        fis.close();
        fos.close();
        long endTime = System.currentTimeMillis();
        System.out.println("文件大小：" + (srcFile.length() / 1024) + "字节");
        System.out.println("总耗时：" + (endTime - startTime));
        System.out.println("传输速度：" + ((srcFile.length() / 1024) / ((endTime - startTime) / 1000)) + " kb/s");
        System.out.println("传输速度：" + ((srcFile.length() / 1024) / ((endTime - startTime) / 1000) / 1024) + " M/s");
    }
}
