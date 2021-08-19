package com.sunrise.study.inputoutput.NIO.selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author huangzihua
 * @date 2021-07-06
 */
public class NioDiscardClient {
    public static void main(String[] args) throws IOException {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 18899);
        // 获取通道
        SocketChannel socketChannel = SocketChannel.open(address);
        // 切换非阻塞
        socketChannel.configureBlocking(false);
        // 不断自旋，等待连接
        while (!socketChannel.finishConnect()) {
        }
        System.out.println("客户端连接成功");
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put("hello world".getBytes());
        byteBuffer.flip();
        // 发送服务器
        socketChannel.write(byteBuffer);
        socketChannel.shutdownInput();
        socketChannel.close();
    }
}
