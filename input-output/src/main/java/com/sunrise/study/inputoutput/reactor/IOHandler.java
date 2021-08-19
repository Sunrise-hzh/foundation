package com.sunrise.study.inputoutput.reactor;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @author huangzihua
 * @date 2021-07-08
 */
public class IOHandler implements Runnable {
    final SocketChannel socketChannel;
    final SelectionKey selectionKey;

    public IOHandler(Selector selector, SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        this.selectionKey = socketChannel.register(selector, 0);
        this.selectionKey.attach(this);
        this.selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    @Override
    public void run() {

    }
}
