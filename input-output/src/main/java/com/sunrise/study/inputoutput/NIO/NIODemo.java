package com.sunrise.study.inputoutput.NIO;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.security.SecureRandom;

/**
 * @author huangzihua
 * @date 2021-07-02
 */
public class NIODemo {

    public static void main(String[] args) throws IOException {
        channel();
    }

    public static void channel() throws IOException {
        // 打开文件输入流
        FileChannel inChannel = new FileInputStream("d:\\temp\\小菠萝.png").getChannel();
        // 打开文件输出流
        FileChannel outChannel = new FileOutputStream("d:\\temp\\小菠萝-拷贝.png").getChannel();
        // 分配 1024 个字节大小的缓冲区
        ByteBuffer dsts = ByteBuffer.allocate(1024);
        // 将数据从通道读入缓冲区
        while (inChannel.read(dsts) != -1) {
            // 切换缓冲区的读写模式
            dsts.flip();
            // 将缓冲区的数据通过通道写到目的地
            outChannel.write(dsts);
            // 清空缓冲区，准备下一次读
            dsts.clear();
        }
        inChannel.close();
        outChannel.close();
    }

    /**
     * 展示缓存区的读写
     */
    public static void bufferReadAndWrite() {
        // 分配内存大小为11的整型缓存区
        IntBuffer buffer = IntBuffer.allocate(11);
        // 往buffer里写入2个整型数据
        for (int i = 0; i < 2; ++i) {
            int randomNum = new SecureRandom().nextInt();
            buffer.put(randomNum);
        }
        // 将Buffer从写模式切换到读模式
        buffer.flip();
        System.out.println("position >> " + buffer.position()
                + " limit >> " + buffer.limit()
                + " capacity >> " + buffer.capacity());
        // 读取buffer里的数据
        // hasRemaining()判断 position 和 limit 之间是否存在元素。return position < limit;
        while (buffer.hasRemaining()) {
            System.out.println(buffer.get());
        }
        System.out.println("position >> " + buffer.position()
                + " limit >> " + buffer.limit()
                + " capacity >> " + buffer.capacity());
    }
}
