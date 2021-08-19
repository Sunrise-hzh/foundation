package com.sunrise.study.inputoutput.NIO.selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author huangzihua
 * @date 2021-07-06
 */
public class SelectorDemo {
    public static void main(String[] args) throws IOException {
        Selector open = Selector.open();
        Selector open2 = Selector.open();

        System.out.println(open == open2);

        // serverSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        // 绑定连接
        serverSocketChannel.bind(new InetSocketAddress(18899));
        System.out.println(serverSocketChannel.validOps());
        // 注册
        serverSocketChannel.register(open, SelectionKey.OP_ACCEPT);

        System.out.println(open2.keys());
        System.out.println(open.keys());
    }

    public static void test() throws IOException {
        // 选择器
        Selector selector = Selector.open();
        // serverSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        // 绑定连接
        serverSocketChannel.bind(new InetSocketAddress(18899));
        System.out.println(serverSocketChannel.validOps());
        // 注册
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // select() 是阻塞方法
        while (selector.select() > 0) {
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                // 根据具体的I/O事件，执行对应业务
                if (key.isAcceptable()) {
                    System.out.println("accept I/O event");
                } else if (key.isConnectable()) {
                    System.out.println("connect I/O event");
                } else if (key.isReadable()) {
                    System.out.println("read I/O event");
                } else if (key.isWritable()) {
                    System.out.println("write I/O event");
                }
                // 处理完成需要移除选择键，否则将再次轮询到
                keyIterator.remove();
            }
        }
    }
}
