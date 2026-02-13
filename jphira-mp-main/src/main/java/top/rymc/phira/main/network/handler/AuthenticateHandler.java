package top.rymc.phira.main.network.handler;

import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.Main;
import top.rymc.phira.main.data.UserInfo;

import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.request.PhiraFetcher;
import top.rymc.phira.main.storage.player.PlayerDataManager;
import top.rymc.phira.main.storage.player.PlayerInfo;
import top.rymc.phira.protocol.handler.PacketHandler;
import top.rymc.phira.protocol.data.UserProfile;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundAuthenticatePacket;
import top.rymc.phira.protocol.packet.serverbound.*;

import java.io.IOException;

@RequiredArgsConstructor
public class AuthenticateHandler extends PacketHandler {

    private final PlayerConnection connection;

    @Override
    public void handle(ServerBoundPingPacket packet) {

    }

    @Override
    public void handle(ServerBoundAuthenticatePacket packet) {

        try {

            System.out.printf("%s sent his token [%s]%n",connection.getRemoteAddressAsString(), packet.getToken());
            UserInfo userInfo = PhiraFetcher.GET_USER_INFO.apply(packet.getToken());

            PlayerInfo playerInfo = PlayerDataManager.loadPlayerInfo(userInfo.getId());
            if (playerInfo == null) {
                String code = Main.getVerificationCodeService().generateOrGetCode(userInfo);
                String message = String.format(
                        """

                                你好 [%s] %s
                                在进入服务器内你需要先绑定QQ
                                你的绑定码为: %s (5分钟内有效)
                                请在QQ群内发送 /bind %s 进行绑定
                                """,
                        userInfo.getId(),
                        userInfo.getName(),
                        code, code
                );

                connection.send(new ClientBoundAuthenticatePacket.Failed(message));
                connection.close();
                return;
            }

            connection.send(new ClientBoundAuthenticatePacket.Success(
                    new UserProfile(userInfo.getId(), userInfo.getName()),
                    false
            ));

            System.out.printf("%s has logged in as [%s] %s%n", connection.getRemoteAddressAsString(), userInfo.getId(), userInfo.getName());

            connection.sendChat("————————————————————————————————————————");

            connection.setPacketHandler(new PlayHandler(new Player(connection, userInfo, playerInfo)));
        } catch (IOException e) {
            connection.send(new ClientBoundAuthenticatePacket.Failed(e.getMessage()));
            connection.close();
        }
    }



    @Override
    public void handle(ServerBoundChatPacket packet) {

    }

    @Override
    public void handle(ServerBoundTouchesPacket packet) {

    }

    @Override
    public void handle(ServerBoundJudgesPacket packet) {

    }

    @Override
    public void handle(ServerBoundCreateRoomPacket packet) {

    }

    @Override
    public void handle(ServerBoundJoinRoomPacket packet) {

    }

    @Override
    public void handle(ServerBoundLeaveRoomPacket packet) {

    }

    @Override
    public void handle(ServerBoundLockRoomPacket packet) {

    }

    @Override
    public void handle(ServerBoundCycleRoomPacket packet) {

    }

    @Override
    public void handle(ServerBoundSelectChartPacket packet) {

    }

    @Override
    public void handle(ServerBoundRequestStartPacket packet) {

    }

    @Override
    public void handle(ServerBoundReadyPacket packet) {

    }

    @Override
    public void handle(ServerBoundCancelReadyPacket packet) {

    }

    @Override
    public void handle(ServerBoundPlayedPacket packet) {

    }

    @Override
    public void handle(ServerBoundAbortPacket packet) {

    }
}
