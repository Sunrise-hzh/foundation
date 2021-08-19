package com.sunrise.study.inputoutput.reactor;

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
 * @date 2021-07-08
 */
public class EchoServerReactor implements Runnable {
    Selector selector;
    ServerSocketChannel serverSocketChannel;

    public EchoServerReactor() throws IOException {
        this.selector = Selector.open();
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(18899));
        this.serverSocketChannel.configureBlocking(false);
        SelectionKey selectionKey = serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        selectionKey.attach(new AcceptorHandler());
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    dispatch(key);
                }
                selectedKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void dispatch(SelectionKey key) {
        Runnable handler = (Runnable) key.attachment();
        if (handler != null) {
            handler.run();
        }
    }

    class AcceptorHandler implements Runnable {

        @Override
        public void run() {
            SocketChannel socketChannel = null;
            try {
                socketChannel = serverSocketChannel.accept();
                if (socketChannel != null) {
                    new EchoHandler(selector, socketChannel);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new EchoServerReactor()).start();
    }
}
