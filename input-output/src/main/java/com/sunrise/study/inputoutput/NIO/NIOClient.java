package com.sunrise.study.inputoutput.NIO;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

/**
 * @author huangzihua
 * @date 2021-07-02
 */
public class NIOClient {
    public static final int CAPACITY = 1024;

    public static void main(String[] args) throws Exception {
        ByteBuffer dsts = ByteBuffer.allocate(CAPACITY);
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 3333));
        socketChannel.configureBlocking(false);
        Scanner sc = new Scanner(System.in);
        while (true) {
            String msg = sc.next();
            dsts.put(msg.getBytes());
            dsts.flip();
            socketChannel.write(dsts);
            dsts.clear();
        }
    }
}
