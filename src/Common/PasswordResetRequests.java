package Common;

import Database.Db;

import java.util.List;
import java.util.Map;

public final class PasswordResetRequests {
    public record Request(int id, String username, String message, String createdAt, String status) {}

    private PasswordResetRequests() {}

    public static void submit(String username) throws Exception {
        ensureTable();
        Db.update("""
                INSERT INTO password_reset_requests (username, message)
                VALUES (?, ?)
                """, username, "Lost password - username: " + username);
    }

    public static List<Request> list() throws Exception {
        ensureTable();
        return Db.query("""
                SELECT request_id, username, message, created_at, status
                FROM password_reset_requests
                ORDER BY created_at DESC, request_id DESC
                """).stream().map(PasswordResetRequests::toRequest).toList();
    }

    public static void markResolved(int requestId) throws Exception {
        ensureTable();
        Db.update("UPDATE password_reset_requests SET status = 'Resolved' WHERE request_id = ?", requestId);
    }

    private static void ensureTable() throws Exception {
        Db.update("""
                CREATE TABLE IF NOT EXISTS password_reset_requests (
                    request_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    message VARCHAR(500) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    status VARCHAR(20) NOT NULL DEFAULT 'Pending'
                )
                """);
    }

    private static Request toRequest(Map<String, Object> row) {
        return new Request(
                ((Number) row.get("request_id")).intValue(),
                text(row.get("username")),
                text(row.get("message")),
                text(row.get("created_at")),
                text(row.get("status")));
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
