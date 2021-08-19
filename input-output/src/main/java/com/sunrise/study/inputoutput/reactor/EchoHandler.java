package com.sunrise.study.inputoutput.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @author huangzihua
 * @date 2021-07-08
 */
public class EchoHandler implements Runnable {
    final SocketChannel socketChannel;
    final SelectionKey selectionKey;
    final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    static final int RECIEVING = 0;
    static final int SENDING = 0;
    int state = RECIEVING;

    public EchoHandler(Selector selector, SocketChannel channel) throws IOException {
        socketChannel = channel;
        socketChannel.configureBlocking(false);
        // 这里只是获取选择键，后面再设置感兴趣的IO事件
        selectionKey = socketChannel.register(selector, 0);

        // 将 Handler 作为选择键的附件
        selectionKey.attach(this);

        // 注册read事件
        selectionKey.interestOps(SelectionKey.OP_READ);
        selector.wakeup();
    }

    @Override
    public void run() {
        try {
            if (state == SENDING) {
                // 写入通道
                socketChannel.write(byteBuffer);
                byteBuffer.clear();
                selectionKey.interestOps(SelectionKey.OP_READ);
                state = RECIEVING;
            } else if (state == RECIEVING) {
                int length = 0;
                while ((length = socketChannel.read(byteBuffer)) > 0) {
                    System.out.println(new String(byteBuffer.array(), 0, length));
                }
                byteBuffer.flip();
                selectionKey.interestOps(SelectionKey.OP_WRITE);
                state = SENDING;
            }
        } catch (IOException e) {
            e.printStackTrace();
            selectionKey.cancel();
            try {
                socketChannel.finishConnect();
            } catch (IOException ex) {
                e.printStackTrace();
            }
        }
    }
}
