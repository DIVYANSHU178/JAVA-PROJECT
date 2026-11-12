import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final String DB_URL = System.getProperty("db.url", "jdbc:postgresql://localhost:7777/railstay_db");
    private static final String DB_USER = System.getProperty("db.user", "postgres");
    private static final String DB_PASSWORD = System.getProperty("db.password", "");

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        boolean ok = testConnection();
        System.out.println("PostgreSQL Connection status: " + (ok ? "CONNECTED SUCCESS" : "FAILED"));
    }
}
