package com.sunrise.study.inputoutput.NIO.selector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author huangzihua
 * @date 2021-07-06
 */
public class NioReceiveServer {
    // 接收文件路径
    private static final String RECEIVE_PATH = "D:/temp/nio_test/";
    // 缓冲区
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    // 使用 Map 保存每个客户端传输，当 OP_READ 通道可读时，根据 Channel 找到对应的对象
    Map<SelectableChannel, Client> clientMap = new HashMap<>();

    public void startServer() throws IOException {
        // 获取选择器
        Selector selector = Selector.open();
        // 获取通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverSocketChannel.socket();
        // 设置非阻塞
        serverSocketChannel.configureBlocking(false);
        // 绑定连接
        InetSocketAddress address = new InetSocketAddress(18899);
        serverSocket.bind(address);
        // 将通道注册到选择器上
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务器监听中...");
        // 轮询选择键集合
        while (selector.select() > 0) {
            // 获取选择键集合
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                // 获取单个选择键
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {   // 连接接收
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = server.accept();
//                    if (socketChannel == null) {continue;}
                    socketChannel.configureBlocking(false);
                    SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
                    Client client = new Client();
                    client.remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
                    clientMap.put(socketChannel, client);
                    System.out.println(socketChannel.getRemoteAddress() + "连接成功...");
                } else if (key.isReadable()) {  // socket可读
                    processData(key);
                }
                // NIO 的特点是只会累加，已选择键的集合不会删除
                // 如果不删除，下一次又会被select()函数选中
                iterator.remove();
            }
        }
    }

    // 处理客户端传输过来的数据
    private void processData(SelectionKey key) throws IOException {
        Client client = clientMap.get(key.channel());
        SocketChannel socketChannel = (SocketChannel) key.channel();
        int num = 0;
        try {
            // 首先清空buffer
            buffer.clear();
            // 读取通道内容
            while ((num = socketChannel.read(buffer)) > 0) {
                // 切换buffer模式
                buffer.flip();
                // 客户端发送过的，首先处理文件名
                if (null == client.fileName) {
                    // 判断buffer大小（这里如果小于4，是否说明数据传输不完整？）
                    if (buffer.capacity() < 4) {
                        continue;
                    }
                    // 获取文件长度
                    int fileNameLen = buffer.getInt();
                    System.out.println("文件长度：" + fileNameLen);

                    // 获取文件名。（这里可能报错）
                    byte[] fileNameBytes = new byte[fileNameLen];
                    buffer.get(fileNameBytes);
                    // 文件名，byte 转换为 string
                    String fileName = new String(fileNameBytes, StandardCharsets.UTF_8);

                    // 创建目录
                    File directory = new File(RECEIVE_PATH);
                    if (!directory.exists()) {
                        directory.mkdir();
                    }
                    System.out.println("NIO 传输目标dir：" + directory);
                    client.fileName = fileName;
                    String fullName = directory.getAbsolutePath() + "\\" + fileName;
                    System.out.println("NIO 传输目标文件：" + fullName);
                    // 创建文件
                    File file = new File(fullName);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    // 创建文件通道
                    FileChannel fileChannel = new FileOutputStream(file).getChannel();
                    client.outChannel = fileChannel;

                    if (buffer.capacity() < 8) {
                        continue;
                    }
                    // 读取文件长度
                    long fileLength = buffer.getLong();
                    client.fileLength = fileLength;
                    client.startTime = System.currentTimeMillis();
                    System.out.println("NIO 传输开始：");

                    // 读取文件内容，并写入到本地磁盘
                    client.receiveLength += buffer.capacity();
                    if (buffer.capacity() > 0) {
                        // 写入文件
                        client.outChannel.write(buffer);
                    }
                    if (client.isFinished()) {
                        finished(key, client);
                    }
                    buffer.clear();
                } else {
                    // 客户端发送过来的，最后是文件内容
                    client.receiveLength += buffer.capacity();
                    // 写入文件
                    client.outChannel.write(buffer);
                    if (client.isFinished()) {
                        finished(key, client);
                    }
                    buffer.clear();
                }
            }
            key.cancel();
        } catch (IOException e) {
            key.cancel();
            e.printStackTrace();
            return;
        }
        if (num == -1) {
            finished(key, client);
            buffer.clear();
        }
    }

    private void finished(SelectionKey key, Client client) throws IOException {
        client.outChannel.close();
        System.out.println("上传完毕");
        key.cancel();
        System.out.println("文件接收成功，File Name：" + client.fileName);
        System.out.println(" Size：" + client.fileLength);
        long endTime = System.currentTimeMillis();
        System.out.println(" NIO IO 传输毫秒数：" + (endTime - client.startTime));
    }

    public static void main(String[] args) throws IOException {
        NioReceiveServer server = new NioReceiveServer();
        server.startServer();
    }

    static class Client {
        String fileName;    // 文件名
        long fileLength;    // 长度
        long startTime;     // 开始传输的时间
        InetSocketAddress remoteAddress;    // 客户端地址
        FileChannel outChannel;     // 输出的文件通道
        long receiveLength;         // 接收长度

        public boolean isFinished() {
            return receiveLength >= fileLength;
        }
    }
}
