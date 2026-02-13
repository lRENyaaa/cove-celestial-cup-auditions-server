package top.rymc.phira.main.storage.player;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;

public class VerifyServer {
    private final HttpServer server;
    private final String host;
    private final int port;

    @FunctionalInterface
    public interface VerifyHandler {
        Optional<VerifyResult> verify(String qq, String code);
    }

    @FunctionalInterface
    public interface CheckHandler {
        Optional<VerifyResult> check(String qq);
    }

    private final VerifyHandler verifyHandler;
    private final CheckHandler checkHandler;

    public VerifyServer(String host, int port, VerifyHandler verifyHandler, CheckHandler checkHandler) throws IOException {
        this.host =  host;
        this.port = port;
        this.verifyHandler = verifyHandler;
        this.checkHandler = checkHandler;

        InetAddress bindAddr = "0.0.0.0".equals(this.host) ?
                null : InetAddress.getByName(this.host);

        this.server = HttpServer.create(new InetSocketAddress(bindAddr, port), 0);
        this.server.createContext("/verify", new VerifyEndpoint());
        this.server.createContext("/check", new CheckEndpoint());
        this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public void start() {
        server.start();
        System.out.println("Server started on http://" + host + ":" + port);
        System.out.println("  - POST/GET /verify?qq=xxx&code=xxx");
        System.out.println("  - POST/GET /check?qq=xxx");
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject json) throws IOException {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private JsonObject createResultJson(Optional<VerifyResult> result) {
        JsonObject json = new JsonObject();
        json.addProperty("result", result.isPresent());
        result.ifPresent(r -> {
            json.addProperty("user", r.user());
            if (r.name() != null) {
                json.addProperty("name", r.name());
            }
        });
        return json;
    }

    public String getAddress() {
        return host + ":" + port;
    }

    private java.util.Map<String, String> parseQueryString(String query) {
        var map = new java.util.HashMap<String, String>();
        if (query == null || query.isEmpty()) return map;

        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private class VerifyEndpoint implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                var params = parseQueryString(exchange.getRequestURI().getQuery());
                String qq = params.get("qq");
                String code = params.get("code");

                if (qq == null || code == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Missing required parameters: qq, code");
                    sendJsonResponse(exchange, 400, error);
                    return;
                }

                Optional<VerifyResult> result = verifyHandler.verify(qq, code);
                sendJsonResponse(exchange, 200, createResultJson(result));

            } catch (Exception e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Internal error: " + e.getMessage());
                sendJsonResponse(exchange, 500, error);
            }
        }
    }

    private class CheckEndpoint implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                var params = parseQueryString(exchange.getRequestURI().getQuery());
                String qq = params.get("qq");

                if (qq == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Missing required parameter: qq");
                    sendJsonResponse(exchange, 400, error);
                    return;
                }

                Optional<VerifyResult> result = checkHandler.check(qq);
                sendJsonResponse(exchange, 200, createResultJson(result));

            } catch (Exception e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Internal error: " + e.getMessage());
                sendJsonResponse(exchange, 500, error);
            }
        }
    }
}