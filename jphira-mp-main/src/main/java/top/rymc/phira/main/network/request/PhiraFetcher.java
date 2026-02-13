package top.rymc.phira.main.network.request;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.data.GameRecord;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.function.throwable.ThrowableFunction;
import top.rymc.phira.main.function.throwable.ThrowableIntFunction;
import top.rymc.phira.main.function.throwable.ThrowableSupplier;
import top.rymc.phira.main.util.GsonUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class PhiraFetcher {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = GsonUtil.getGson();

    @Setter
    private static String host = "https://phira.5wyxi.com/";

    @Getter
    private static final GenericCache<String, UserInfo> tokenCache =
            GenericCache.create(10, TimeUnit.MINUTES, 10000);
    @Getter
    private static final GenericCache<Integer, GameRecord> recordCache =
            GenericCache.create(30, TimeUnit.MINUTES, 50000);
    @Getter
    private static final GenericCache<Integer, ChartInfo> chartCache =
            GenericCache.create(30, TimeUnit.MINUTES, 10000);
    @Getter
    private static final GenericCache<Integer, UserInfo> userCache =
            GenericCache.create(10, TimeUnit.MINUTES, 5000);

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
        }, 10);
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> T retry(ThrowableSupplier<T, IOException> supplier, int maxAttempts) throws IOException {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                return supplier.get();
            } catch (IOException e) {
                if (i == maxAttempts - 1) throw e;
            }
        }
        throw new AssertionError();
    }

    public static ThrowableFunction<String, UserInfo, IOException> GET_USER_INFO =
            (token) -> tokenCache.get(token, t -> {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(host + "me"))
                        .header("Authorization", "Bearer " + t)
                        .GET()
                        .build();
                return GSON.fromJson(fetch(request), UserInfo.class);
            });

    public static ThrowableIntFunction<UserInfo, IOException> GET_USER_INFO_BY_ID =
            (userId) -> userCache.get(userId, id -> {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(host + "user/" + id))
                        .GET()
                        .build();
                return GSON.fromJson(fetch(request), UserInfo.class);
            });

    public static ThrowableIntFunction<ChartInfo, IOException> GET_CHART_INFO =
            (chartId) -> chartCache.get(chartId, id -> {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(host + "chart/" + id))
                        .GET()
                        .build();
                return GSON.fromJson(fetch(request), ChartInfo.class);
            });

    public static ThrowableIntFunction<GameRecord, IOException> GET_RECORD_INFO =
            (recordId) -> recordCache.get(recordId, id -> {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(host + "record/" + id))
                        .GET()
                        .build();
                return GSON.fromJson(fetch(request), GameRecord.class);
            });
}
