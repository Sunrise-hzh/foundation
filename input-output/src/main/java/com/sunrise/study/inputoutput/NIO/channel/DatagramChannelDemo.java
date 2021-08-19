package com.sunrise.study.inputoutput.NIO.channel;

import java.io.IOException;
import java.nio.channels.DatagramChannel;

/**
 * @author huangzihua
 * @date 2021-07-06
 */
public class DatagramChannelDemo {
    public static void main(String[] args) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
    }
}
