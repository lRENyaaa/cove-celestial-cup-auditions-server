package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.Main;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.RaceScoreOperator;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.storage.player.PlayerDataManager;
import top.rymc.phira.main.storage.player.PlayerInfo;
import top.rymc.phira.main.util.MathCalculator;
import top.rymc.phira.protocol.data.UserProfile;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.handler.PacketHandler;
import top.rymc.phira.protocol.packet.clientbound.*;
import top.rymc.phira.protocol.packet.serverbound.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayHandler extends PacketHandler {

    private final Player player;
    private final PlayerConnection connection;
    
    public PlayHandler(Player player) {
        this.player = player;
        this.connection = player.getConnection();
        init();
    }

    private void init() {
        UserInfo userInfo = player.getUserInfo();
        connection.sendChat("CoveCelestial Cup 澈宸杯 海选比赛服务器 正式服");
        connection.sendChat(String.format("[%s] %s 你好，你需要完成并提交海选图池中的谱面来参加海选",userInfo.getId(), userInfo.getName()));
        connection.sendChat(String.format("你已绑定到QQ: %s", player.getPlayerInfo().getQq()));
        connection.sendChat("每张谱对应一个房间ID，加入房间即游玩该谱面，房间均为虚拟房间，不受其他人影响");
        List<ChartInfo> chartInfoList = Main.getChartInfoList();
        for (int i = 0, chartInfoListSize = chartInfoList.size(); i < chartInfoListSize; i++) {
            ChartInfo info = chartInfoList.get(i);
            connection.sendChat(String.format("房间: %s (快捷加入房间: %s) - 谱面: %s (#%s)", "race" + info.getId(), i, info.getName(), info.getId()));
        }
        connection.sendChat("你的历史最好成绩:");

        BigDecimal totalAccScore = BigDecimal.ZERO;
        BigDecimal totalStdScore = BigDecimal.ZERO;
        for (ChartInfo info : Main.getChartInfoList()) {
            int id = info.getId();

            RaceScoreOperator scoreOperator = player.getScoreOperator();

            Optional<RaceScoreOperator.ScoreInfo> bestScoreInfo = scoreOperator.getBestScoreInfo(id);
            if (bestScoreInfo.isEmpty()) {
                connection.sendChat(String.format("房间: %s - 无成绩记录", "race" + id));
                continue;
            }

            RaceScoreOperator.ScoreInfo scoreInfo = bestScoreInfo.get();
            int score = scoreInfo.score();
            float accuracy = (float) scoreInfo.accuracy() * 100;
            float std = (float) scoreInfo.std() * 1000;
            connection.sendChat(String.format("房间: %s - 分数: %s, 准度: %s%%, 误差: ±%sms", "race" + id, score, accuracy, std));

            float difficulty = info.getDifficulty();

            BigDecimal accScore = MathCalculator.accScore(difficulty, accuracy / 100);
            BigDecimal stdScore = MathCalculator.stdScore(difficulty, std);

            connection.sendChat(String.format("房间: %s - 准度分: %s, 误差分: %s", "race" + id,
                    accScore.setScale(5, RoundingMode.DOWN).stripTrailingZeros().toPlainString(),
                    stdScore.setScale(5, RoundingMode.DOWN).stripTrailingZeros().toPlainString()));

            totalAccScore = totalAccScore.add(accScore);
            totalStdScore = totalStdScore.add(stdScore);
        }

        connection.sendChat(String.format("你的总积分: 准度分 %s 误差分 %s",
                totalAccScore.setScale(5, RoundingMode.DOWN).stripTrailingZeros().toPlainString(),
                totalStdScore.setScale(5, RoundingMode.DOWN).stripTrailingZeros().toPlainString()));


        connection.sendChat("自动循环加入房间: " + (player.getPlayerInfo().isAutoContinue() ? "已启用" : "未启用"));
        connection.sendChat("自动开始游戏等待时间: " + player.getPlayerInfo().getFormattedJoinWait() + "秒");
        connection.sendChat("通过创建名称为 status 的房间来查看排行榜");
        connection.sendChat("通过创建名称为 loop 的房间来切换自动循环加入房间的设置");
        connection.sendChat("通过创建名称为 wait-n 的房间来设置自动开始游戏等待时间为n秒");
        connection.sendChat("n必须大于1，使用_代替小数点");
        connection.sendChat("————————————————————————————————————————");
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
        
    }

    @Override
    public void handle(ServerBoundJudgesPacket packet) {

    }

    @Override
    public void handle(ServerBoundCreateRoomPacket packet) {

        String room = packet.getRoomId().toLowerCase(Locale.ROOT);
        if (room.equals("status")) {
            connection.sendChat("请稍后，正在为你收集数据");

            CompletableFuture.runAsync(() -> {
                List<ChartInfo> chartInfoList = Main.getChartInfoList();

                List<RaceScoreOperator> operators = Main.getDao().listAllPlayers()
                        .stream()
                        .map(RaceScoreOperator::from)
                        .toList();

                Map<UserInfo, Score> scoreMap = new HashMap<>();

                for (RaceScoreOperator operator : operators) {
                    BigDecimal totalAccScore = BigDecimal.ZERO;
                    BigDecimal totalStdScore = BigDecimal.ZERO;

                    for (ChartInfo info : chartInfoList) {
                        int id = info.getId();

                        Optional<RaceScoreOperator.ScoreInfo> bestScoreInfo = operator.getBestScoreInfo(id);
                        if (bestScoreInfo.isEmpty()) {
                            continue;
                        }

                        RaceScoreOperator.ScoreInfo scoreInfo = bestScoreInfo.get();

                        float accuracy = (float) scoreInfo.accuracy();
                        float std = (float) scoreInfo.std() * 1000;

                        float difficulty = info.getDifficulty();

                        BigDecimal accScore = MathCalculator.accScore(difficulty, accuracy);
                        BigDecimal stdScore = MathCalculator.stdScore(difficulty, std);

                        totalAccScore = totalAccScore.add(accScore);
                        totalStdScore = totalStdScore.add(stdScore);
                    }

                    scoreMap.put(operator.getUserInfo(), new Score(totalAccScore, totalStdScore));
                }

                AtomicInteger i = new AtomicInteger(1);
                scoreMap.entrySet().stream()
                        .sorted((Map.Entry.comparingByValue(Score.RANK_COMPARATOR)))
                        .forEach(entry -> {
                            UserInfo user = entry.getKey();
                            Score score = entry.getValue();

                            connection.sendChat(String.format("排名 %s, 玩家 [%s] %s - 准度分 %s 误差分 %s",
                                    i.getAndIncrement(),
                                    user.getId(),
                                    user.getName(),
                                    score.accScore.setScale(5, RoundingMode.DOWN).stripTrailingZeros().toPlainString(),
                                    score.stdScore.setScale(5, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
                            ));
                        });

                connection.sendChat("————————————————————————————————————————");

            });

            connection.send(new ClientBoundCreateRoomPacket.Failed("操作已完成"));
            return;
        }

        if (room.equals("loop")) {
            PlayerInfo oldPlayerInfo = player.getPlayerInfo();
            boolean newValue = !oldPlayerInfo.isAutoContinue();
            PlayerInfo newPlayerInfo = new PlayerInfo(oldPlayerInfo.getId(), oldPlayerInfo.getQq(), newValue, oldPlayerInfo.getJoinWait());
            PlayerDataManager.savePlayerInfo(newPlayerInfo);
            player.refreshPlayerInfo();
            connection.send(new ClientBoundCreateRoomPacket.Failed("自动循环加入房间已" + (newValue ? "启用" : "禁用")));
            return;
        }

        if (room.startsWith("wait-")) {
            String waitTimeStr = room.substring(5).replace('_', '.');
            try {
                float waitTime = Float.parseFloat(waitTimeStr);
                if (waitTime < 1) {
                    connection.send(new ClientBoundCreateRoomPacket.Failed("等待时间必须大于1秒"));
                    return;
                }

                PlayerInfo oldPlayerInfo = player.getPlayerInfo();
                PlayerInfo newPlayerInfo = new PlayerInfo(oldPlayerInfo.getId(), oldPlayerInfo.getQq(), oldPlayerInfo.isAutoContinue(), waitTime);
                PlayerDataManager.savePlayerInfo(newPlayerInfo);
                player.refreshPlayerInfo();
                connection.send(new ClientBoundCreateRoomPacket.Failed("自动开始游戏等待时间已设置为 " + newPlayerInfo.getFormattedJoinWait() + " 秒"));
            } catch (NumberFormatException e) {
                connection.send(new ClientBoundCreateRoomPacket.Failed("请输入正确的等待时间，格式: wait_n, n必须大于1"));
            }
            return;
        }

        connection.send(new ClientBoundCreateRoomPacket.Failed("你不能在比赛服务器创建房间"));

    }

    private record Score(BigDecimal accScore, BigDecimal stdScore) {
        private static final Comparator<Score> RANK_COMPARATOR =
                Comparator.comparing(Score::accScore)
                        .thenComparing(Score::stdScore)
                        .reversed();
    }

    @Override
    public void handle(ServerBoundJoinRoomPacket packet) {
        String room = packet.getRoomId().toLowerCase(Locale.ROOT);
        if (room.startsWith("race")) {
            String id = room.substring(4);
            for (ChartInfo info : Main.getChartInfoList()) {
                if (!String.valueOf(info.getId()).equals(id)) {
                    continue;
                }

                joinRaceFakeRoom(info);
                return;
            }

            connection.send(new ClientBoundJoinRoomPacket.Failed("请输入正确的房间名"));
            return;
        }

        findChartByIndex(room).ifPresentOrElse(
                this::joinRaceFakeRoom,
                () -> connection.send(new ClientBoundJoinRoomPacket.Failed("请输入正确的房间名"))
        );
    }

    private Optional<ChartInfo> findChartByIndex(String indexStr) {
        int index;
        try {
            index = Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        List<ChartInfo> chartInfoList = Main.getChartInfoList();
        if (index < 0 || index >= chartInfoList.size()) {
            return Optional.empty();
        }
        return Optional.of(chartInfoList.get(index));
    }


    private void joinRaceFakeRoom(ChartInfo info) {
        UserInfo userInfo = player.getUserInfo();
        UserProfile profile = new UserProfile(userInfo.getId(), userInfo.getName());
        connection.send(new ClientBoundOnJoinRoomPacket(profile, false));
        connection.send(new ClientBoundJoinRoomPacket.Success(new SelectChart(info.getId()), List.of(profile), new ArrayList<>(), true));
        connection.setPacketHandler(new RaceHandler(player, info));

        System.out.printf("Player [%s] %s joined race room for chart #%s%n",
                userInfo.getId(),
                userInfo.getName(),
                info.getId()
        );
    }

    @Override
    public void handle(ServerBoundLeaveRoomPacket packet) {
        connection.send(new ClientBoundLeaveRoomPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundLockRoomPacket packet) {
        connection.send(new ClientBoundLockRoomPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundCycleRoomPacket packet) {
        connection.send(new ClientBoundCycleRoomPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundSelectChartPacket packet) {
        connection.send(new ClientBoundSelectChartPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundRequestStartPacket packet) {
        connection.send(new ClientBoundRequestStartPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundReadyPacket packet) {
        connection.send(new ClientBoundReadyPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundCancelReadyPacket packet) {
        connection.send(new ClientBoundCancelReadyPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundPlayedPacket packet) {
        connection.send(new ClientBoundPlayedPacket.Failed("你不能在这执行这个操作"));
    }

    @Override
    public void handle(ServerBoundAbortPacket packet) {
        connection.send(new ClientBoundAbortPacket.Failed("你不能在这执行这个操作"));
    }
}
