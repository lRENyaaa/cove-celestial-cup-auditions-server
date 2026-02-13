package top.rymc.phira.main.storage.dao;

import top.rymc.phira.main.data.GameRecord;
import java.util.List;
import java.util.Optional;

public interface GameRecordDao {

    boolean save(GameRecord record);

    Optional<GameRecord> findById(int id);

    List<GameRecord> findAll();

    default boolean delete(GameRecord record) {
        return delete(record.getId());
    }

    boolean delete(int id);

    List<GameRecord> findByPlayer(int player);



    List<GameRecord> findByPlayerAndChart(int player, int chart);

    Optional<GameRecord> findBestByPlayerAndChart(int player, int chart);

    List<Integer> listAllPlayers();
}