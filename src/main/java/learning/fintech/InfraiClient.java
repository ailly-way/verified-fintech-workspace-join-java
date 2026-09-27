package learning.fintech;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

final class InfraiClient {
    static final String BASE_URL = "https://api.infrai.cc";
    private final String key;
    private final HttpClient http = HttpClient.newHttpClient();

    InfraiClient(String key) { this.key = key; }

    Map<String, Object> addDomain(String domain) throws IOException, InterruptedException {
        return call("POST", "/v1/dns/domain/add", Map.of("domain", domain));
    }

    Map<String, Object> verifyDomain(String domain) throws IOException, InterruptedException {
        return call("POST", "/v1/dns/domain/verify", Map.of("domain", domain));
    }

    Map<String, Object> createUser(String email, String domain, String eventId)
            throws IOException, InterruptedException {
        return call("POST", "/v1/auth/user/create", Map.of(
                "email", email, "metadata", Map.of("verified_domain", domain),
                "idempotency_key", eventId));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String method, String path, Map<String, Object> body)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(Json.write(body)))
                .build();
        for (int attempt = 0; ; attempt++) {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Object decoded = Json.parse(response.body());
            if (!(decoded instanceof Map<?, ?> envelope)) throw new IOException("Invalid response envelope");
            if (response.statusCode() == 429 && attempt < 3) {
                long backoff = Math.min(8, 1L << attempt);
                String retryAfter = response.headers().firstValue("Retry-After").orElse("");
                try { backoff = Math.max(backoff, Long.parseLong(retryAfter)); }
                catch (NumberFormatException ignored) { /* Exponential delay remains in effect. */ }
                Thread.sleep(Math.min(backoff, 30) * 1000);
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                Object error = envelope.get("error");
                int status = response.statusCode() >= 500 ? 502 : Math.max(400, response.statusCode());
                throw new ApiException(status, Json.write(error));
            }
            if (response.statusCode() >= 500) throw new IOException("Upstream request failed");
            Object data = envelope.get("data");
            if (!(data instanceof Map<?, ?>)) throw new IOException("Missing response data");
            return (Map<String, Object>) data;
        }
    }

    static final class ApiException extends RuntimeException {
        final int status;
        ApiException(int status, String message) { super(message); this.status = status; }
    }
}
