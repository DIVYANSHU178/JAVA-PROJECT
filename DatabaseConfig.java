import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    private static String dbUrl = null;
    private static String dbUser = "postgres";
    private static String dbPassword = "";
    private static boolean driverLoaded = false;

    static {
        try {
            Class.forName("org.postgresql.Driver");
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            System.err.println("❌ PostgreSQL JDBC Driver not found: " + e.getMessage());
        }

        // 1. Check config.properties if present
        File propFile = new File("config.properties");
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                Properties props = new Properties();
                props.load(fis);
                if (props.containsKey("db.url")) dbUrl = props.getProperty("db.url").trim();
                if (props.containsKey("db.user")) dbUser = props.getProperty("db.user").trim();
                if (props.containsKey("db.password")) dbPassword = props.getProperty("db.password").trim();
            } catch (IOException ignored) {}
        }

        // 2. Override from System properties or Environment variables if provided
        String sysUrl = System.getProperty("db.url", System.getenv("DB_URL"));
        if (sysUrl != null && !sysUrl.isBlank()) dbUrl = sysUrl.trim();

        String sysUser = System.getProperty("db.user", System.getenv("DB_USER"));
        if (sysUser != null && !sysUser.isBlank()) dbUser = sysUser.trim();

        String sysPass = System.getProperty("db.password", System.getenv("DB_PASSWORD"));
        if (sysPass != null) dbPassword = sysPass.trim();
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            throw new SQLException("PostgreSQL JDBC driver not loaded. Ensure lib/postgresql-42.7.4.jar is in classpath.");
        }

        // If URL was determined, connect directly
        if (dbUrl != null) {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        }

        // Auto-detect: Try port 7777 first (custom), then default port 5432
        String[] candidateUrls = {
            "jdbc:postgresql://localhost:7777/railstay_db",
            "jdbc:postgresql://localhost:5432/railstay_db"
        };

        for (String url : candidateUrls) {
            try {
                Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                if (conn != null && !conn.isClosed()) {
                    dbUrl = url;
                    System.out.println("✅ Database connection established: " + url);
                    return conn;
                }
            } catch (SQLException ignored) {
                // Try next port
            }
        }

        // If both failed, try with standard default password 'postgres' on 5432 or 7777
        for (String url : candidateUrls) {
            try {
                Connection conn = DriverManager.getConnection(url, dbUser, "postgres");
                if (conn != null && !conn.isClosed()) {
                    dbUrl = url;
                    dbPassword = "postgres";
                    System.out.println("✅ Database connection established with password 'postgres': " + url);
                    return conn;
                }
            } catch (SQLException ignored) {}
        }

        // Fallback default for error reporting
        dbUrl = candidateUrls[0];
        throw new SQLException("Could not connect to PostgreSQL on localhost:7777 or localhost:5432 for database 'railstay_db'.\n" +
                "👉 Solution: Run setup_database.bat to create railstay_db and import data/railstay_full_backup.sql, or specify credentials in config.properties.");
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            return false;
        }
    }

    public static String getActiveUrl() {
        return dbUrl != null ? dbUrl : "Not Connected";
    }

    public static void main(String[] args) {
        boolean ok = testConnection();
        System.out.println("PostgreSQL Connection status: " + (ok ? "CONNECTED SUCCESS (" + getActiveUrl() + ")" : "FAILED"));
    }
}
