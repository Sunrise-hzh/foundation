package com.sunrise.study.inputoutput.NIO.selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * @author huangzihua
 * @date 2021-07-06
 */
public class NioDiscardServer {
    public static void main(String[] args) throws IOException {
        server();
    }

    public static void server() throws IOException {
        // 获取选择器
        Selector selector = Selector.open();
        // 获取通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 设置非阻塞
        serverSocketChannel.configureBlocking(false);
        // 绑定连接
        serverSocketChannel.bind(new InetSocketAddress(18899));
        System.out.println("服务器启动成功");
        // 将通道注册的“接收”事件注册到选择器上
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        // 轮询感兴趣的IO事件
        while (selector.select() > 0) {
            // 获取选择键集合
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                // 获取单个选择键
                SelectionKey selectionKey = selectedKeys.next();
                // 判断key是什么事件
                if (selectionKey.isAcceptable()) {
                    // 若是“接收事件”，就获取客户端连接
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    // 设置非阻塞
                    socketChannel.configureBlocking(false);
                    // 将新连接的通道的可读事件注册到选择器上
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (selectionKey.isReadable()) {
                    // 若是“可读事件”，则读取数据
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    // 读取数据，然后丢弃
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    byte[] text = new byte[1024];
                    int length = 0;
                    while ((length = socketChannel.read(byteBuffer)) > 0) {
                        byteBuffer.flip();
                        byteBuffer.get(text, 0, byteBuffer.limit());
                        System.out.println(new String(text, StandardCharsets.UTF_8));
                        byteBuffer.clear();
                    }
                    socketChannel.close();
                }
                // 移除选择键
                selectedKeys.remove();
            }
        }
        serverSocketChannel.close();
    }
}
