package com.sunrise.study.inputoutput.NIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Set;

/**
 * @author huangzihua
 * @date 2021-07-02
 */
public class NIOServer {
    Selector serverSelector = Selector.open();
    Selector clientSelector = Selector.open();

    public NIOServer() throws IOException {
    }

    public static void main(String[] args) throws IOException {
        NIOServer server = new NIOServer();
        new Thread(() -> {
            try {
                // 对应IO编程中服务端启动
                ServerSocketChannel listenerChannel = ServerSocketChannel.open();
                listenerChannel.socket().bind(new InetSocketAddress(3333));
                listenerChannel.configureBlocking(false);
                listenerChannel.register(server.serverSelector, SelectionKey.OP_ACCEPT);
                server.acceptListener();
            } catch (IOException ignored) {
            }
        }).start();
        new Thread(() -> {
            try {
                server.clientListener();
            } catch (IOException ignored) {
            }
        }).start();
    }

    // 监听客户端连接
    public void acceptListener() throws IOException {
        while (true) {
            if (serverSelector.select(1) > 0) {
                Set<SelectionKey> set = serverSelector.selectedKeys();
                Iterator<SelectionKey> keyIterator = set.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        try {
                            // (1) 每来一个新连接，注册到clientSelector
                            SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
                            clientChannel.configureBlocking(false);
                            clientChannel.register(clientSelector, SelectionKey.OP_READ);
                        } finally {
                            // 从就绪的列表中移除这个key
                            keyIterator.remove();
                        }
                    }
                }
            }
        }
    }
    // 监听客户端的 IO 状态就绪
    public void clientListener() throws IOException {
        while (true) {
            // 批量轮询是否有哪些连接有数据可读
            if (clientSelector.select(1) > 0) {
                Set<SelectionKey> set = clientSelector.selectedKeys();
                Iterator<SelectionKey> keyIterator = set.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    // 判断该通道是否读就绪状态
                    if (key.isReadable()) {
                        try {
                            // 获取客户端通道读入数据
                            SocketChannel clientChannel = (SocketChannel) key.channel();
                            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                            clientChannel.read(byteBuffer);
                            byteBuffer.flip();
                            System.out.println(
                                    LocalDateTime.now().toString() + " Server 端接收到来自 Client 端的消息: " +
                                            Charset.defaultCharset().decode(byteBuffer).toString());
                        } finally {
                            // 从就绪的列表中移除这个key
                            keyIterator.remove();
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }
            }
        }
    }
}
