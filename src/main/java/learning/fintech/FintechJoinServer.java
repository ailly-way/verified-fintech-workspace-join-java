package learning.fintech;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class FintechJoinServer {
    private FintechJoinServer() {}

    public static void main(String[] args) throws IOException {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
        InfraiClient client = new InfraiClient(key);
        WorkspaceJoin service = new WorkspaceJoin(client);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/domains", exchange -> {
            try {
                Map<String, Object> input = input(exchange);
                send(exchange, 200, client.addDomain(required(input, "domain")));
            } catch (Exception error) { reject(exchange, error); }
        });
        server.createContext("/joins", exchange -> {
            try {
                Map<String, Object> input = input(exchange);
                send(exchange, 200, service.join(required(input, "email"), required(input, "domain"),
                        new BigDecimal(required(input, "amount")), required(input, "event_id")));
            } catch (Exception error) { reject(exchange, error); }
        });
        server.start();
        System.out.println("Listening on http://127.0.0.1:" + port);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> input(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) throw new IllegalArgumentException("Use POST");
        Object body = Json.parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        if (!(body instanceof Map<?, ?>)) throw new IllegalArgumentException("Expected JSON object");
        return (Map<String, Object>) body;
    }

    private static String required(Map<String, Object> input, String field) {
        Object value = input.get(field);
        if (!(value instanceof String) || ((String) value).isBlank())
            throw new IllegalArgumentException(field + " is required as a string");
        return (String) value;
    }

    private static void reject(HttpExchange exchange, Exception error) throws IOException {
        int status = error instanceof InfraiClient.ApiException api ? api.status
                : error instanceof IllegalArgumentException ? 400 : 502;
        send(exchange, status, Map.of("error", error.getMessage() == null ? "Request failed" : error.getMessage()));
    }

    private static void send(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
    }
}
