package top.rymc.phira.main.storage.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private static final String DATA_FOLDER = "playerdata";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private static final ConcurrentHashMap<Integer, PlayerInfo> CACHE = new ConcurrentHashMap<>();

    static {
        File folder = new File(DATA_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        loadAllToCache();
    }

    private static void loadAllToCache() {
        File folder = new File(DATA_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null) {
            int count = 0;
            for (File file : files) {
                try {
                    String filename = file.getName();
                    int id = Integer.parseInt(filename.substring(0, filename.length() - 5));

                    try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                        PlayerInfo info = GSON.fromJson(reader, PlayerInfo.class);
                        if (info != null) {
                            CACHE.put(id, info);
                            count++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to preload: " + file.getName());
                }
            }
            System.out.println("Preloaded " + count + " player records into cache");
        }
    }

    public static boolean savePlayerInfo(PlayerInfo playerInfo) {
        if (playerInfo == null) {
            System.err.println("PlayerInfo cannot be null");
            return false;
        }

        int id = playerInfo.getId();
        CACHE.put(id, playerInfo);

        File file = new File(DATA_FOLDER, id + ".json");
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(playerInfo, writer);
            writer.flush();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save player data for ID " + id + ", rolling back cache");
            CACHE.remove(id);
            e.printStackTrace();
            return false;
        }
    }

    public static PlayerInfo loadPlayerInfo(int id) {
        PlayerInfo cached = CACHE.get(id);
        if (cached != null) {
            return cached;
        }

        File file = new File(DATA_FOLDER, id + ".json");
        if (!file.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            PlayerInfo info = GSON.fromJson(reader, PlayerInfo.class);
            if (info != null) {
                CACHE.put(id, info);
            }
            return info;
        } catch (IOException e) {
            System.err.println("Failed to load player data for ID " + id);
            e.printStackTrace();
            return null;
        }
    }

    public static boolean deletePlayerInfo(int id) {
        CACHE.remove(id);

        File file = new File(DATA_FOLDER, id + ".json");
        if (file.exists()) {
            if (!file.delete()) {
                System.err.println("Warning: Failed to delete file for ID " + id + ", but cache is already cleared");
                return false;
            }
        }
        return true;
    }

    public static boolean exists(int id) {
        return CACHE.containsKey(id) || new File(DATA_FOLDER, id + ".json").exists();
    }

    public static PlayerInfo reload(int id) {
        CACHE.remove(id);
        return loadPlayerInfo(id);
    }

    public static int getCachedCount() {
        return CACHE.size();
    }

    public static Collection<PlayerInfo> getAllCached() {
        return CACHE.values();
    }

    public static String getDataFolderPath() {
        return new File(DATA_FOLDER).getAbsolutePath();
    }
}