package com.sunrise.study.inputoutput.NIO.selector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

/**
 * @author huangzihua
 * @date 2021-07-06
 */
public class NioSendClient {
    public static void main(String[] args) throws IOException {
        NioSendClient client = new NioSendClient();
        client.sendFile();
    }

    public void sendFile() throws IOException {
        String sourcePath = "D:/temp/nio_test/file.txt";
        File file = new File(sourcePath);
        FileChannel fileChannel = new FileInputStream(file).getChannel();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.socket().connect(new InetSocketAddress("127.0.0.1", 18899));
        socketChannel.configureBlocking(false);
        // 可以通过finishConnect()判断通道是否连上，但是是否可读，需要通过selector选择器来检查
        while (!socketChannel.finishConnect()) {
            // 不断地自旋、等待，或者做一些其他的事情
        }
        // 发送文件名称和长度
        ByteBuffer buffer = sendFileNameAndLength("file.txt", file, socketChannel);
        // 发送文件内容
        int length = sendContent(file, fileChannel, socketChannel, buffer);
        if (length == -1) {
            fileChannel.close();
            socketChannel.shutdownOutput();
            socketChannel.close();
        }
    }

    public int sendContent(File file, FileChannel fileChannel, SocketChannel socketChannel, ByteBuffer buffer) throws IOException {
        // 发送文件内容
        System.out.println("开始传输文件");
        int length = 0;
        long progress = 0;
        while ((length = fileChannel.read(buffer)) > 0) {
            buffer.flip();
            socketChannel.write(buffer);
            buffer.clear();
            progress += length;
            System.out.println("| " + (100 * progress / file.length()) + "% |");
        }
        return length;
    }

    // 步骤：文件名称长度-->>文件名-->>文件大小
    public ByteBuffer sendFileNameAndLength(String destFile, File file, SocketChannel socketChannel) throws IOException {
        // 1、定义文件名称buffer
        ByteBuffer fileNameByteBuffer = ByteBuffer.allocate(1024);
        // 2、往buffer写数据
        fileNameByteBuffer.put(destFile.getBytes());


        // 发送文件名称长度
        // 1、分配buffer
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        // 2、get文件名长度
        int fileNameLen = destFile.getBytes().length;
        // 3、往buffer写数据
        buffer.putInt(fileNameLen);
        // 4、切换buffer为read模式
        buffer.flip();
        // 5、往channel写数据
        socketChannel.write(buffer);
        // 6、清空buffer
        buffer.clear();
        System.out.println("Client 文件名称长度发送完成：" + fileNameLen);

        // 发送文件名称
        // 3、切换buffer模式
        fileNameByteBuffer.flip();
        // 4、往通道写数据
        socketChannel.write(fileNameByteBuffer);
        System.out.println("Client 文件名称发送完成：" + destFile);

        // 发送文件长度
        // 1、往buffer写数据
        buffer.putLong(file.length());
        // 2、切换buffer模式
        buffer.flip();
        // 3、往通道写数据
        socketChannel.write(buffer);
        // 4、清空buffer
        buffer.clear();
        System.out.println("Client 文件长度发送完成：" + file.length());
        return buffer;
    }
}
