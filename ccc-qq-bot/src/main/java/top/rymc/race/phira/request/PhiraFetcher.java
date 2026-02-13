package top.rymc.race.phira.request;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import lombok.Setter;
import top.rymc.race.phira.data.ChartInfo;
import top.rymc.race.phira.data.GameRecord;
import top.rymc.race.phira.data.UserInfo;
import top.rymc.race.function.throwable.ThrowableFunction;
import top.rymc.race.function.throwable.ThrowableIntFunction;
import top.rymc.race.function.throwable.ThrowableSupplier;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;

public class PhiraFetcher {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class,
                    (JsonDeserializer<OffsetDateTime>) (json, type, context) ->
                            OffsetDateTime.parse(json.getAsString()))
            .create();

    @Setter
    private static String host = "https://phira.5wyxi.com/";

    private static String fetch(HttpRequest request) throws IOException {
        return retry(() -> {
            try {
                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("HTTP request failed with status code: " + response.statusCode());
                }

                return response.body();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }, 5);
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> T retry(ThrowableSupplier<T,IOException> supplier, int maxAttempts) throws IOException {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                return supplier.get();
            } catch (IOException e) {
                if (i == maxAttempts - 1) throw e;
            }
        }
        throw new AssertionError();
    }

    public static ThrowableFunction<String, UserInfo,IOException> GET_USER_INFO = (token) -> {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "me"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        return GSON.fromJson(fetch(request), UserInfo.class);
    };

    public static ThrowableIntFunction<ChartInfo, IOException> GET_CHART_INFO = (chartId) -> {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "chart/" + chartId))
                .GET()
                .build();

        return GSON.fromJson(fetch(request), ChartInfo.class);
    };

    ThrowableIntFunction<GameRecord, IOException> GET_RECORD_INFO = (recordId) -> {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + "record/" + recordId))
                .GET()
                .build();

        return GSON.fromJson(fetch(request), GameRecord.class);
    };

}
