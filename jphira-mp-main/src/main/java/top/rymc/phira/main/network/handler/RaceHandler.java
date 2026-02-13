package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.Main;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.data.GameRecord;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.network.PhiraRecord;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.request.PhiraFetcher;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.Playing;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.data.state.WaitForReady;
import top.rymc.phira.protocol.handler.PacketHandler;
import top.rymc.phira.protocol.packet.clientbound.*;
import top.rymc.phira.protocol.packet.serverbound.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RaceHandler extends PacketHandler {

    private boolean inGame = false;

    private final Player player;
    private final PlayerConnection connection;

    private final ChartInfo chartInfo;

    private final List<TouchFrame> touchFrames = new ArrayList<>();
    private final List<JudgeEvent> judgeEvents = new ArrayList<>();

    private Thread worker;

    public RaceHandler(Player player, ChartInfo chartInfo) {
        this.player = player;
        this.chartInfo = chartInfo;
        this.connection = player.getConnection();
        initWorker();
    }

    private void initWorker() {
        worker = new Thread(() -> {
            try {
                connection.sendChat(String.format("已加入房间，谱面: %s (#%s)", chartInfo.getName(), chartInfo.getId()));
                connection.sendChat("将在" + player.getPlayerInfo().getFormattedJoinWait() + "秒后自动进入游戏，你可以在此之前离开房间");
                connection.sendChat("————————————————————————————————————————");

                Thread.sleep(player.getPlayerInfo().getJoinWaitInMilliseconds());

                inGame = true;
                connection.send(new ClientBoundChangeStatePacket(new WaitForReady()));
                connection.sendChat("游戏开始! 点击准备立即进入游戏");

                UserInfo info = player.getUserInfo();
                System.out.printf("Player [%s] %s has join a game on chart #%s%n",
                        info.getId(),
                        info.getName(),
                        chartInfo.getId()
                );


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        worker.start();
    }



    @Override
    public void handle(ServerBoundPingPacket packet) {
        connection.send(ClientBoundPongPacket.INSTANCE);
    }

    @Override
    public void handle(ServerBoundAuthenticatePacket packet) {
        connection.send(new ClientBoundAuthenticatePacket.Failed("你已经登录服务器了"));
    }

    @Override
    public void handle(ServerBoundChatPacket packet) {
        connection.send(new ClientBoundChatPacket.Failed("你不能在比赛服务器聊天"));
    }


    @Override
    public void handle(ServerBoundTouchesPacket packet) {
        touchFrames.addAll(packet.getFrames());
        /*
        List<TouchFrame> frames = packet.getFrames();
        System.out.printf("Total Frames: %d%n", frames.size());
        for (TouchFrame frame : frames) {
            System.out.printf("TouchFrame { time: %f, points: [", frame.getTime());
            List<TouchPoint> points = frame.getPoints();
            for (int i = 0; i < points.size(); i++) {
                TouchPoint point = points.get(i);
                CompactPos pos = point.getPos();
                System.out.printf("(%d, CompactPos { x: %f, y: %f })", point.getId(), pos.getX(), pos.getY());
                if (i != points.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("] }");
        }
         */
    }

    @Override
    public void handle(ServerBoundJudgesPacket packet) {
        judgeEvents.addAll(packet.getJudges());
        /*
        List<JudgeEvent> judges = packet.getJudges();
        System.out.printf("Total Judges: %d%n", judges.size());
        for (JudgeEvent judge : judges) {
            System.out.printf("JudgeEvent { time: %f, line_id: %d, note_id: %d, judgement: %s }%n",
                    judge.getTime(),
                    judge.getLineId(),
                    judge.getNoteId(),
                    judge.getJudgement()
            );
        }
         */
    }

    @Override
    public void handle(ServerBoundCreateRoomPacket serverBoundCreateRoomPacket) {
        connection.send(new ClientBoundCreateRoomPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundJoinRoomPacket serverBoundJoinRoomPacket) {
        connection.send(new ClientBoundJoinRoomPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundLeaveRoomPacket serverBoundLeaveRoomPacket) {
        if (inGame) {
            connection.send(new ClientBoundLeaveRoomPacket.Failed("你不能在游戏中离开房间"));
            return;
        }

        worker.interrupt();
        connection.setPacketHandler(new PlayHandler(player));
        connection.send(new ClientBoundLeaveRoomPacket.Success());
        UserInfo info = player.getUserInfo();
        System.out.printf("Player [%s] %s has left the race room for chart #%s%n",
                info.getId(),
                info.getName(),
                chartInfo.getId()
        );
    }

    @Override
    public void handle(ServerBoundLockRoomPacket serverBoundLockRoomPacket) {
        connection.send(new ClientBoundLeaveRoomPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundCycleRoomPacket serverBoundCycleRoomPacket) {
        connection.send(new ClientBoundLeaveRoomPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundSelectChartPacket serverBoundSelectChartPacket) {
        connection.send(new ClientBoundLeaveRoomPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundRequestStartPacket serverBoundRequestStartPacket) {
        connection.send(new ClientBoundLeaveRoomPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundReadyPacket serverBoundReadyPacket) {
        connection.send(new ClientBoundReadyPacket.Success());
        connection.send(new ClientBoundChangeStatePacket(new Playing()));
        UserInfo info = player.getUserInfo();
        System.out.printf("Player [%s] %s is start to play chart #%s%n",
                info.getId(),
                info.getName(),
                chartInfo.getId()
        );

    }

    @Override
    public void handle(ServerBoundCancelReadyPacket serverBoundCancelReadyPacket) {
        connection.send(new ClientBoundCancelReadyPacket.Failed("你不能取消准备"));
    }

    @Override
    public void handle(ServerBoundPlayedPacket packet) {

        CompletableFuture.runAsync(() -> {
            connection.sendChat("提交成绩中，请稍后...");
            UserInfo info = player.getUserInfo();
            try {
                GameRecord record = PhiraFetcher.GET_RECORD_INFO.apply(packet.getId());
                Main.getDao().save(record);
                connection.sendChat(String.format("你成功上传了成绩，你的成绩是 分数: %s, 准度: %s%%, 误差: ±%sms",
                        record.getScore(),
                        record.getAccuracy() * 100,
                        record.getStd() * 1000
                ));

                Player.addRecord(new PhiraRecord(record.getId(), record.getChart(), chartInfo.getName(), player.getUserInfo().getId(), player.getUserInfo().getName(), touchFrames, judgeEvents));
                connection.sendChat(String.format("已保存本次游玩的操作录像: %s.phirarec", record.getId()));

                System.out.printf("Player [%s] %s submitted a record for chart #%s, record id: %s%n",
                        info.getId(),
                        info.getName(),
                        chartInfo.getId(),
                        record.getId()
                );
                player.refreshScoreOperator();

            } catch (Exception e) {
                connection.sendChat("成绩提交失败!");
                connection.sendChat("请将错误信息提交给管理者以解决问题:");
                connection.sendChat(getStackTraceAsString(e));
                e.printStackTrace();

                System.err.printf("Failed to submit record for player [%s] %s on chart #%s%n",
                        info.getId(),
                        info.getName(),
                        chartInfo.getId()
                );
            }

            endGame();
        });

        connection.send(new ClientBoundPlayedPacket.Success());


    }

    public static String getStackTraceAsString(final Throwable throwable) {
        if (throwable == null) {
            return "null";
        }

        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return throwable.toString();
        }
    }


    @Override
    public void handle(ServerBoundAbortPacket serverBoundAbortPacket) {
        connection.send(new ClientBoundAbortPacket.Success());
        connection.sendChat("你放弃了游戏!");
        endGame();
    }

    private void endGame() {
        inGame = false;
        connection.send(new ClientBoundChangeStatePacket(new SelectChart(chartInfo.getId())));

        if (player.getPlayerInfo().isAutoContinue()) {
            connection.sendChat("游戏结束，正在自动重新进入房间...");
            connection.sendChat("————————————————————————————————————————");
            connection.setPacketHandler(new RaceHandler(player, chartInfo));
        } else {
            connection.sendChat("游戏结束，若需要重新开始请重新加入房间");
            connection.sendChat("————————————————————————————————————————");
        }
    }
}
