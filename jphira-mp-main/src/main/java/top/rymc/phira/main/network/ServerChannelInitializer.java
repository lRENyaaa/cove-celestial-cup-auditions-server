package top.rymc.phira.main.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.timeout.ReadTimeoutHandler;
import top.rymc.phira.main.network.handler.AuthenticateHandler;
import top.rymc.phira.protocol.codec.decoder.FrameDecoder;
import top.rymc.phira.protocol.codec.decoder.PacketDecoder;
import top.rymc.phira.protocol.codec.encoder.FrameEncoder;
import top.rymc.phira.protocol.codec.encoder.PacketEncoder;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class ServerChannelInitializer extends ChannelInitializer<Channel> {

    private final ChannelGroup allChannels;

    public ServerChannelInitializer(ChannelGroup allChannels) {
        this.allChannels = allChannels;
    }


    @Override
    protected void initChannel(Channel channel) {
        allChannels.add(channel);

        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        String ipPort = remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();

        System.out.printf("Establishing a connection from %s%n",ipPort);

        FrameDecoder decoder = new FrameDecoder();
        channel.pipeline().addLast(decoder);

        decoder.getClientProtocolVersion().whenComplete((version,throwable) -> {
            if (throwable != null) {
                System.out.printf("Disconnecting %s: %s%n",ipPort,throwable.getMessage());
                if (channel.isActive()) {
                    channel.close();
                }
                return;
            }

            System.out.printf("Receive client version %s from %s%n",version,ipPort);

            channel.pipeline()
                    .addLast(new FrameEncoder())
                    .addLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                    .addLast(new PacketDecoder())
                    .addLast(new PacketEncoder());

            PlayerConnection connection = new PlayerConnection(channel, remoteAddress);
            connection.setPacketHandler(new AuthenticateHandler(connection));
            channel.pipeline().addLast(connection);
        });

    }
}
