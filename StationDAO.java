import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationDAO {

    public static List<Map<String, Object>> searchStations(String query, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String q = query.trim().toUpperCase();
        int maxResults = limit > 0 ? limit : 15;

        try (Connection conn = DatabaseConfig.getConnection()) {
            // 1. Check if query matches any metropolitan city cluster
            String clusterSql = """
                SELECT DISTINCT city_name 
                FROM city_station_clusters 
                WHERE LOWER(city_name) LIKE ? 
                ORDER BY city_name 
                LIMIT 2;
            """;
            try (PreparedStatement cps = conn.prepareStatement(clusterSql)) {
                cps.setString(1, "%" + query.trim().toLowerCase() + "%");
                try (ResultSet crs = cps.executeQuery()) {
                    while (crs.next()) {
                        String cityName = crs.getString("city_name");
                        Map<String, Object> cMap = new HashMap<>();
                        cMap.put("stationCode", cityName);
                        cMap.put("stationName", cityName + " - ALL STATIONS");
                        cMap.put("state", cityName);
                        cMap.put("zone", "ALL");
                        cMap.put("lat", 0.0);
                        cMap.put("lon", 0.0);
                        results.add(cMap);
                    }
                }
            } catch (SQLException ignore) {}

            // 2. Station code exact or prefix match first, then name contains match
            String sql = """
                SELECT station_code, station_name, state, zone, latitude, longitude,
                       CASE 
                           WHEN station_code = ? THEN 1
                           WHEN station_code LIKE ? THEN 2
                           WHEN LOWER(station_name) LIKE ? THEN 3
                           ELSE 4
                       END AS match_priority
                FROM stations
                WHERE station_code LIKE ? OR LOWER(station_name) LIKE ?
                ORDER BY match_priority, station_name
                LIMIT ?;
            """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String codePrefix = q + "%";
                String namePattern = "%" + query.trim().toLowerCase() + "%";

                ps.setString(1, q);
                ps.setString(2, codePrefix);
                ps.setString(3, namePattern);
                ps.setString(4, codePrefix);
                ps.setString(5, namePattern);
                ps.setInt(6, maxResults);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next() && results.size() < maxResults) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("stationCode", rs.getString("station_code"));
                        map.put("stationName", rs.getString("station_name"));
                        map.put("state", rs.getString("state"));
                        map.put("zone", rs.getString("zone"));
                        map.put("lat", rs.getDouble("latitude"));
                        map.put("lon", rs.getDouble("longitude"));
                        results.add(map);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in searchStations: " + e.getMessage());
        }

        return results;
    }

    public static Map<String, Object> getStation(String code) {
        if (code == null) return null;
        String sql = "SELECT station_code, station_name, state, zone FROM stations WHERE station_code = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("stationCode", rs.getString("station_code"));
                    map.put("stationName", rs.getString("station_name"));
                    map.put("state", rs.getString("state"));
                    map.put("zone", rs.getString("zone"));
                    return map;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getStation: " + e.getMessage());
        }
        return null;
    }
}
