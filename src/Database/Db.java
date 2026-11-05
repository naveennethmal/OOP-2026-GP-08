package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Db {

    private static boolean noticeSchemaReady;

    private Db() {
    }

    public static List<Map<String, Object>> query(String sql, Object... parameters) throws Exception {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metadata = resultSet.getMetaData();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int column = 1; column <= metadata.getColumnCount(); column++) {
                        row.put(metadata.getColumnLabel(column), resultSet.getObject(column));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    public static int update(String sql, Object... parameters) throws Exception {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            return statement.executeUpdate();
        }
    }

    public static long insertAndGetId(String sql, Object... parameters) throws Exception {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, parameters);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return the generated ID.");
                }
                return keys.getLong(1);
            }
        }
    }

    public static synchronized void ensureNoticeSchema() throws Exception {
        if (noticeSchemaReady) return;

        ensureNoticeColumn("target_role", "VARCHAR(50) NOT NULL DEFAULT 'All'");
        ensureNoticeColumn("start_date", "DATE NULL");
        ensureNoticeColumn("end_date", "DATE NULL");
        noticeSchemaReady = true;
    }

    public static List<Map<String, Object>> noticesForRole(String role) throws Exception {
        ensureNoticeSchema();
        return query("""
                SELECT notice_id, title, published_date, description,
                       DATEDIFF(CURDATE(), published_date) <= 7 AS is_new
                FROM notices
                WHERE (target_role = 'All' OR target_role = ?)
                  AND (start_date IS NULL OR start_date <= CURDATE())
                  AND (end_date IS NULL OR end_date >= CURDATE())
                ORDER BY published_date DESC, notice_id DESC
                """, role);
    }

    public static List<Map<String, Object>> latestNoticesForRole(String role, int limit) throws Exception {
        ensureNoticeSchema();
        return query("""
                SELECT notice_id, title, published_date, description,
                       DATEDIFF(CURDATE(), published_date) <= 7 AS is_new
                FROM notices
                WHERE (target_role = 'All' OR target_role = ?)
                  AND (start_date IS NULL OR start_date <= CURDATE())
                  AND (end_date IS NULL OR end_date >= CURDATE())
                ORDER BY published_date DESC, notice_id DESC
                LIMIT ?
                """, role, limit);
    }

    public static int countNoticesForRole(String role) throws Exception {
        ensureNoticeSchema();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(*) AS total
                FROM notices
                WHERE (target_role = 'All' OR target_role = ?)
                  AND (start_date IS NULL OR start_date <= CURDATE())
                  AND (end_date IS NULL OR end_date >= CURDATE())
                """, role);
        return ((Number) rows.get(0).get("total")).intValue();
    }

    private static void ensureNoticeColumn(String column, String definition) throws Exception {
        List<Map<String, Object>> rows = query(
                "SELECT COUNT(*) AS total FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'notices' AND column_name = ?",
                column);
        if (((Number) rows.get(0).get("total")).intValue() == 0) {
            update("ALTER TABLE notices ADD COLUMN " + column + " " + definition);
        }
    }

    private static void bind(PreparedStatement statement, Object... parameters) throws Exception {
        for (int index = 0; index < parameters.length; index++) {
            statement.setObject(index + 1, parameters[index]);
        }
    }
}
