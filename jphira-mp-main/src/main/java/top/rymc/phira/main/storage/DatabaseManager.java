package top.rymc.phira.main.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database Manager: Responsible for initializing H2 database and creating tables
 * Manages database connection pool and schema initialization
 */
public class DatabaseManager {

    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);
    private final HikariDataSource dataSource;

    public DatabaseManager(String dbPath) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(formatH2JdbcUrl(dbPath));
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        this.dataSource = new HikariDataSource(config);
        initializeTables();
    }
    private static String formatH2JdbcUrl(String dbPath) {
        if (dbPath.startsWith("~/")) {
            String homeDir = System.getProperty("user.home");
            dbPath = Paths.get(homeDir, dbPath.substring(2)).toString();
        }

        return formatH2JdbcUrl(Paths.get(dbPath));
    }

    private static String formatH2JdbcUrl(Path dbPath) {
        try {
            Path path = dbPath.toAbsolutePath().normalize();
            Path parentDir = path.getParent();

            if (parentDir != null) {
                Files.createDirectories(parentDir);
                if (!Files.isWritable(parentDir)) {
                    throw new IllegalStateException("Database directory not writable: " + parentDir);
                }
            }

            String filePath = path.toString().replace("\\", "/");

            return "jdbc:h2:file:" + filePath;

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to format H2 JDBC URL for path: " + dbPath, e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void initializeTables() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS game_record (
                id INT PRIMARY KEY,
                player INT NOT NULL,
                chart INT NOT NULL,
                score INT NOT NULL,
                accuracy FLOAT NOT NULL,
                perfect INT NOT NULL,
                good INT NOT NULL,
                bad INT NOT NULL,
                miss INT NOT NULL,
                speed FLOAT NOT NULL,
                max_combo INT NOT NULL,
                best BOOLEAN NOT NULL,
                best_std BOOLEAN NOT NULL,
                mods INT NOT NULL,
                full_combo BOOLEAN NOT NULL,
                time TIMESTAMP NOT NULL,
                std FLOAT NOT NULL,
                std_score FLOAT NOT NULL
            )
            """;

        String[] indexSQLs = {
                "CREATE INDEX IF NOT EXISTS idx_player ON game_record(player)",
                "CREATE INDEX IF NOT EXISTS idx_chart ON game_record(chart)",
                "CREATE INDEX IF NOT EXISTS idx_player_chart ON game_record(player, chart)",
                "CREATE INDEX IF NOT EXISTS idx_accuracy ON game_record(accuracy DESC)",
                "CREATE INDEX IF NOT EXISTS idx_std ON game_record(std DESC)"
        };

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);
            logger.info("Database table created successfully");

            for (String sql : indexSQLs) {
                stmt.execute(sql);
            }
            logger.info("All indexes created successfully");

        } catch (SQLException e) {
            logger.error("Database initialization failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}