package top.rymc.race.phira;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public class VerifyClient {
    private final HttpClient client;
    private final String baseUrl;

    public VerifyClient(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Optional<VerifyResult> verify(String qq, String code) throws IOException, InterruptedException {
        String uri = baseUrl + "/verify?qq=" +
                     encode(qq) + "&code=" + encode(code);
        return requestAndParse(uri);
    }

    public Optional<VerifyResult> check(String qq) throws IOException, InterruptedException {
        String uri = baseUrl + "/check?qq=" + encode(qq);
        return requestAndParse(uri);
    }

    private Optional<VerifyResult> requestAndParse(String uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new RuntimeException("Request failed with status: " + response.statusCode());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        boolean result = json.get("result").getAsBoolean();

        if (result && json.has("user")) {
            int user = json.get("user").getAsInt();
            String name = json.has("name") && !json.get("name").isJsonNull() ?
                    json.get("name").getAsString() : null;
            return Optional.of(new VerifyResult(user, name));
        }
        return Optional.empty();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }


}
