package top.rymc.phira.main.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.OffsetDateTime;

public class GsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class,
                    (JsonSerializer<OffsetDateTime>) (src, type, context) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(OffsetDateTime.class,
                    (JsonDeserializer<OffsetDateTime>) (json, type, context) -> OffsetDateTime.parse(json.getAsString()))
            .setPrettyPrinting()
            .create();

    public static Gson getGson() {
        return GSON;
    }
}