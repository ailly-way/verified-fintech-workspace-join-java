package learning.fintech;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class WorkspaceJoin {
    private final InfraiClient client;

    WorkspaceJoin(InfraiClient client) { this.client = client; }

    static String decision(String email, String domain, BigDecimal amount) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        String company = domain.trim().toLowerCase(Locale.ROOT);
        if (!company.matches("[a-z0-9-]+(\\.[a-z0-9-]+)+") ||
                !normalized.matches("[^@\\s]+@[^@\\s]+") ||
                !normalized.substring(normalized.indexOf('@') + 1).equals(company)) {
            throw new IllegalArgumentException("Employee email must match the company domain");
        }
        if (amount.signum() < 0) throw new IllegalArgumentException("Amount must not be negative");
        return amount.compareTo(new BigDecimal("10000")) >= 0 ? "review" : "allow";
    }

    Map<String, Object> join(String email, String domain, BigDecimal amount, String eventId)
            throws IOException, InterruptedException {
        String action = decision(email, domain, amount);
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("event_id is required");
        Map<String, Object> proof = client.verifyDomain(domain.toLowerCase(Locale.ROOT));
        if (!Boolean.TRUE.equals(proof.get("verified"))) {
            throw new InfraiClient.ApiException(403, "Company domain is not verified");
        }
        Map<String, Object> user = client.createUser(email.toLowerCase(Locale.ROOT),
                domain.toLowerCase(Locale.ROOT), eventId);
        Map<String, Object> notification = Map.of("event_id", eventId,
                "kind", "workspace_join", "recipient", email.toLowerCase(Locale.ROOT),
                "decision", action);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspace", domain.toLowerCase(Locale.ROOT));
        result.put("user", user);
        result.put("payment_action", action);
        result.put("audit_notification", notification);
        return result;
    }
}
