package ftms;

import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/** All JDBC access lives here (database handling). Change URL/USER/PASS for your MySQL. */
public final class Db {
    private static final String URL = "jdbc:mysql://localhost:3306/ftms?serverTimezone=UTC";
    private static final String USER = "root", PASS = "root";

    private Db() {}

    public static List<Map<String, Object>> query(String sql, Object... p) throws SQLException {
        try (Connection c = DriverManager.getConnection(URL, USER, PASS); PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, p);
            try (ResultSet r = s.executeQuery()) {
                ResultSetMetaData m = r.getMetaData();
                List<Map<String, Object>> out = new ArrayList<>();
                while (r.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= m.getColumnCount(); i++) {
                        Object v = r.getObject(i);
                        row.put(m.getColumnLabel(i), v == null ? "" : v.toString());
                    }
                    out.add(row);
                }
                return out;
            }
        }
    }

    public static int update(String sql, Object... p) throws SQLException {
        try (Connection c = DriverManager.getConnection(URL, USER, PASS); PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, p);
            return s.executeUpdate();
        }
    }

    private static void bind(PreparedStatement s, Object[] p) throws SQLException {
        for (int i = 0; i < p.length; i++) s.setObject(i + 1, p[i]);
    }

    public static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8")));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
