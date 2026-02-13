package top.rymc.phira.main.storage.dao;

import top.rymc.phira.main.data.GameRecord;
import top.rymc.phira.main.storage.DatabaseManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.*;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameRecordDaoImpl implements GameRecordDao {

    private static final Logger logger = LogManager.getLogger(GameRecordDaoImpl.class);
    private final DatabaseManager dbManager;

    public GameRecordDaoImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private GameRecord mapRow(ResultSet rs) throws SQLException {
        GameRecord record = new GameRecord();
        record.setId(rs.getInt("id"));
        record.setPlayer(rs.getInt("player"));
        record.setChart(rs.getInt("chart"));
        record.setScore(rs.getInt("score"));
        record.setAccuracy(rs.getFloat("accuracy"));
        record.setPerfect(rs.getInt("perfect"));
        record.setGood(rs.getInt("good"));
        record.setBad(rs.getInt("bad"));
        record.setMiss(rs.getInt("miss"));
        record.setSpeed(rs.getFloat("speed"));
        record.setMaxCombo(rs.getInt("max_combo"));
        record.setBest(rs.getBoolean("best"));
        record.setBestStd(rs.getBoolean("best_std"));
        record.setMods(rs.getInt("mods"));
        record.setFullCombo(rs.getBoolean("full_combo"));

        Timestamp timestamp = rs.getTimestamp("time");
        record.setTime(timestamp.toInstant().atOffset(ZoneOffset.UTC));

        record.setStd(rs.getFloat("std"));
        record.setStdScore(rs.getFloat("std_score"));
        return record;
    }

    @Override
    public boolean save(GameRecord record) {
        String sql = """
        MERGE INTO game_record
        (id, player, chart, score, accuracy, perfect, good, bad, miss,
         speed, max_combo, best, best_std, mods, full_combo, time, std, std_score)
        KEY (id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, record.getId());
            pstmt.setInt(2, record.getPlayer());
            pstmt.setInt(3, record.getChart());
            pstmt.setInt(4, record.getScore());
            pstmt.setFloat(5, record.getAccuracy());
            pstmt.setInt(6, record.getPerfect());
            pstmt.setInt(7, record.getGood());
            pstmt.setInt(8, record.getBad());
            pstmt.setInt(9, record.getMiss());
            pstmt.setFloat(10, record.getSpeed());
            pstmt.setInt(11, record.getMaxCombo());
            pstmt.setBoolean(12, record.isBest());
            pstmt.setBoolean(13, record.isBestStd());
            pstmt.setInt(14, record.getMods());
            pstmt.setBoolean(15, record.isFullCombo());
            pstmt.setTimestamp(16, Timestamp.from(record.getTime().toInstant()));
            pstmt.setFloat(17, record.getStd());
            pstmt.setFloat(18, record.getStdScore());

            int affectedRows = pstmt.executeUpdate();
            boolean success = affectedRows > 0;
            logger.debug("Insert successful, record ID: {}", record.getId());
            return success;

        } catch (SQLException e) {
            logger.error("Save record failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<GameRecord> findById(int id) {
        String sql = "SELECT * FROM game_record WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Query by ID {} failed: {}", id, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<GameRecord> findAll() {
        String sql = "SELECT * FROM game_record ORDER BY id DESC LIMIT 1000";
        List<GameRecord> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                results.add(mapRow(rs));
            }
            logger.debug("Query all completed, total records: {}", results.size());
            return results;

        } catch (SQLException e) {
            logger.error("Query all records failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM game_record WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("Delete record ID {} failed: {}", id, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<GameRecord> findByPlayer(int player) {
        String sql = "SELECT * FROM game_record WHERE player = ? ORDER BY time DESC";

        List<GameRecord> results = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, player);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(mapRow(rs));
            }
            logger.debug("Query player {} completed, total records: {}", player, results.size());
            return results;

        } catch (SQLException e) {
            logger.error("Query player {} failed: {}", player, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<GameRecord> findByPlayerAndChart(int player, int chart) {
        String sql = "SELECT * FROM game_record WHERE player = ? AND chart = ? ORDER BY time DESC";

        List<GameRecord> results = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, player);
            pstmt.setInt(2, chart);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(mapRow(rs));
            }
            logger.debug("Query player {} chart {} completed, total records: {}", player, chart, results.size());
            return results;

        } catch (SQLException e) {
            logger.error("Query player {} chart {} failed: {}", player, chart, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<GameRecord> findBestByPlayerAndChart(int player, int chart) {
        String sql = """
            SELECT * FROM game_record
            WHERE player = ? AND chart = ?
            ORDER BY accuracy DESC, std DESC
            LIMIT 1
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, player);
            pstmt.setInt(2, chart);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Find best record for player {} chart {} failed: {}", player, chart, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public List<Integer> listAllPlayers() {
        String sql = "SELECT DISTINCT player FROM game_record ORDER BY player ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            List<Integer> playerList = new ArrayList<>();
            while (rs.next()) {
                playerList.add(rs.getInt("player"));
            }

            return playerList;

        } catch (SQLException e) {
            logger.error("Query all player failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}