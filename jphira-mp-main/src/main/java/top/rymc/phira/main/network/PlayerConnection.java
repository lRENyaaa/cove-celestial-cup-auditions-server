package top.rymc.phira.main.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.Main;
import top.rymc.phira.protocol.data.message.ChatMessage;
import top.rymc.phira.protocol.handler.PacketHandler;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.ClientBoundPacket;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Optional;

@Getter
public class PlayerConnection extends ChannelInboundHandlerAdapter {

    private final Channel channel;
    private final InetSocketAddress remoteAddress;

    @Setter
    private PacketHandler packetHandler;

    public PlayerConnection(Channel channel, InetSocketAddress remoteAddress) {
        this.channel = channel;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (packetHandler == null) {
            return;
        }

        if (this.isClosed()) {
            return;
        }

        ServerBoundPacket packet = (ServerBoundPacket) msg;
        packet.handle(packetHandler);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!ctx.channel().isActive()) {
            return;
        }

        ctx.close();

        Logger logger = Main.getLogger();

        if (cause instanceof ReadTimeoutException) {
            logger.error("{}: read timed out", getRemoteAddressAsString());
            return;
        }

        if (cause instanceof SocketException) {
            logger.error("{}: {}", getRemoteAddressAsString(), cause.getMessage());
            return;
        }

        logger.atError().withThrowable(cause).log("{}: exception encountered", getRemoteAddressAsString());


    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.printf("Client disconnected: %s%n", getRemoteAddressAsString());
        super.channelInactive(ctx);
    }

    public Optional<ChannelFuture> send(ClientBoundPacket packet) {
        if (this.isClosed()) {
            return Optional.empty();
        }

        return Optional.ofNullable(channel.writeAndFlush(packet));
    }

    public void sendChat(String message) {
        this.send(new ClientBoundMessagePacket(new ChatMessage(-1,message)));
    }

    public boolean isClosed() {
        return !channel.isActive();
    }

    public void close() {
        if (!this.isClosed()) {
            channel.close();
        }
    }

    public String getRemoteAddressAsString() {
        return remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();
    }
}
