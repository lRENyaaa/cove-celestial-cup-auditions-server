package top.rymc.phira.main.game;

import lombok.Getter;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PhiraRecord;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.storage.player.PlayerDataManager;
import top.rymc.phira.main.storage.player.PlayerInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Player {

    @Getter
    private final PlayerConnection connection;
    @Getter
    private final UserInfo userInfo;
    @Getter
    private PlayerInfo playerInfo;
    @Getter
    private RaceScoreOperator scoreOperator;

    public Player(PlayerConnection connection, UserInfo userInfo, PlayerInfo playerInfo) {
        this.connection = connection;
        this.userInfo = userInfo;
        this.playerInfo = playerInfo;
        this.scoreOperator = RaceScoreOperator.from(userInfo.getId());
    }

    public void refreshPlayerInfo() {
        this.playerInfo = PlayerDataManager.loadPlayerInfo(userInfo.getId());
    }

    public void refreshScoreOperator() {
        this.scoreOperator = RaceScoreOperator.from(userInfo.getId());
    }

    private static final Path recordDirectory = Path.of("records");

    @Getter
    private static final List<PhiraRecord> records = new ArrayList<>(PhiraRecord.readFromDirectory(recordDirectory));

    public static void addRecord(PhiraRecord record) {
        records.add(record);
        PhiraRecord.saveAsFile(record, recordDirectory);
    }

    public static Optional<PhiraRecord> getRecordByRecordId(int recordId) {
        return records.stream().filter(r -> r.getId() == recordId).findFirst();
    }
}
