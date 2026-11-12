import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TrainDAO {

    public static final Map<String, Integer> CLASS_CAPACITIES = Map.of(
            "1A", 24,
            "2A", 48,
            "3A", 64,
            "SL", 72,
            "CC", 72
    );

    public static final Map<String, Integer> RAC_CAPACITIES = Map.of(
            "1A", 0,
            "2A", 4,
            "3A", 8,
            "SL", 12,
            "CC", 0
    );

    public static final Map<String, Integer> WL_CAPACITIES = Map.of(
            "1A", 10,
            "2A", 20,
            "3A", 30,
            "SL", 50,
            "CC", 20
    );

    public static List<Map<String, Object>> searchTrains(String fromCode, String toCode, String date, String quota) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (fromCode == null || toCode == null) return list;

        String fCode = fromCode.trim().toUpperCase();
        String tCode = toCode.trim().toUpperCase();
        String q = (quota == null || quota.isBlank()) ? "GENERAL" : quota.trim().toUpperCase();

        Set<String> seen = new HashSet<>();

        try (Connection conn = DatabaseConfig.getConnection()) {
            List<String> fromCodes = resolveStationCodes(conn, fCode);
            List<String> toCodes = resolveStationCodes(conn, tCode);

            // 1. Route-stops query handles both direct origin-dest and intermediate stops accurately with cluster support
            String sql = """
                SELECT 
                    t.train_number, t.train_name, t.train_type, t.zone,
                    s1.station_code AS board_code, s1.station_name AS board_name, s1.departure_time AS board_dep,
                    s2.station_code AS dest_code, s2.station_name AS dest_name, s2.arrival_time AS dest_arr,
                    (s2.stop_sequence - s1.stop_sequence) AS stop_diff,
                    t.distance_km, t.duration_h, t.duration_m,
                    t.has_1a, t.has_2a, t.has_3a, t.has_sl, t.has_cc,
                    t.fare_1a, t.fare_2a, t.fare_3a, t.fare_sl, t.fare_cc
                FROM train_route_stops s1
                JOIN train_route_stops s2 
                    ON s1.train_number = s2.train_number 
                    AND s1.stop_sequence < s2.stop_sequence
                JOIN trains t 
                    ON t.train_number = s1.train_number
                WHERE s1.station_code = ANY (?) AND s2.station_code = ANY (?)
                ORDER BY s1.departure_time ASC
                LIMIT 60;
            """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setArray(1, conn.createArrayOf("varchar", fromCodes.toArray(new String[0])));
                ps.setArray(2, conn.createArrayOf("varchar", toCodes.toArray(new String[0])));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String tnum = rs.getString("train_number");
                        if (!seen.contains(tnum)) {
                            seen.add(tnum);
                            list.add(mapTrainResult(conn, rs, date, q));
                        }
                    }
                }
            }

            // 2. Direct trains fallback query
            if (list.size() < 10) {
                String directSql = """
                    SELECT 
                        train_number, train_name, train_type, zone,
                        from_station_code AS board_code, from_station_name AS board_name, departure_time AS board_dep,
                        to_station_code AS dest_code, to_station_name AS dest_name, arrival_time AS dest_arr,
                        1 AS stop_diff,
                        distance_km, duration_h, duration_m,
                        has_1a, has_2a, has_3a, has_sl, has_cc,
                        fare_1a, fare_2a, fare_3a, fare_sl, fare_cc
                    FROM trains
                    WHERE from_station_code = ANY (?) AND to_station_code = ANY (?)
                    LIMIT 30;
                """;
                try (PreparedStatement directPs = conn.prepareStatement(directSql)) {
                    directPs.setArray(1, conn.createArrayOf("varchar", fromCodes.toArray(new String[0])));
                    directPs.setArray(2, conn.createArrayOf("varchar", toCodes.toArray(new String[0])));
                    try (ResultSet drs = directPs.executeQuery()) {
                        while (drs.next()) {
                            String tnum = drs.getString("train_number");
                            if (!seen.contains(tnum)) {
                                seen.add(tnum);
                                list.add(mapTrainResult(conn, drs, date, q));
                            }
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error in searchTrains: " + e.getMessage());
        }

        return list;
    }

    public static List<String> resolveStationCodes(Connection conn, String codeOrCity) {
        List<String> codes = new ArrayList<>();
        if (codeOrCity == null || codeOrCity.isBlank()) return codes;
        String term = codeOrCity.trim().toUpperCase();
        codes.add(term);

        String clusterSql = """
            SELECT DISTINCT c2.station_code 
            FROM city_station_clusters c1
            JOIN city_station_clusters c2 ON c1.city_name = c2.city_name
            WHERE c1.station_code = ? OR UPPER(c1.city_name) = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(clusterSql)) {
            ps.setString(1, term);
            ps.setString(2, term);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String sc = rs.getString("station_code");
                    if (!codes.contains(sc)) {
                        codes.add(sc);
                    }
                }
            }
        } catch (SQLException e) {
            // table might not exist or error, fallback to term
        }
        return codes;
    }

    private static Map<String, Object> mapTrainResult(Connection conn, ResultSet rs, String date, String quota) throws SQLException {
        Map<String, Object> item = new HashMap<>();
        String trainNumber = rs.getString("train_number");
        item.put("trainNumber", trainNumber);
        item.put("trainName", rs.getString("train_name"));
        item.put("trainType", rs.getString("train_type"));
        item.put("zone", rs.getString("zone"));
        item.put("fromStationCode", rs.getString("board_code"));
        item.put("fromStationName", rs.getString("board_name"));
        item.put("toStationCode", rs.getString("dest_code"));
        item.put("toStationName", rs.getString("dest_name"));
        item.put("departureTime", rs.getString("board_dep"));
        item.put("arrivalTime", rs.getString("dest_arr"));

        int durH = rs.getInt("duration_h");
        int durM = rs.getInt("duration_m");
        if (durH == 0 && durM == 0) {
            durH = 8;
            durM = 30;
        }
        item.put("duration", durH + "h " + (durM > 0 ? durM + "m" : "00m"));
        item.put("distanceKm", rs.getInt("distance_km"));
        item.put("quota", quota);

        // Class details & live availability
        List<Map<String, Object>> classes = new ArrayList<>();
        String[] classCodes = {"1A", "2A", "3A", "SL", "CC"};
        for (String c : classCodes) {
            boolean hasClass = switch (c) {
                case "1A" -> rs.getBoolean("has_1a");
                case "2A" -> rs.getBoolean("has_2a");
                case "3A" -> rs.getBoolean("has_3a");
                case "SL" -> rs.getBoolean("has_sl");
                case "CC" -> rs.getBoolean("has_cc");
                default -> false;
            };

            if (hasClass) {
                double baseFare = switch (c) {
                    case "1A" -> rs.getDouble("fare_1a");
                    case "2A" -> rs.getDouble("fare_2a");
                    case "3A" -> rs.getDouble("fare_3a");
                    case "SL" -> rs.getDouble("fare_sl");
                    case "CC" -> rs.getDouble("fare_cc");
                    default -> 500.0;
                };

                // Adjust for quota
                double finalFare = calculateFareForQuota(baseFare, quota, c);

                Map<String, Object> cMap = new HashMap<>();
                cMap.put("classCode", c);
                cMap.put("className", getClassName(c));
                cMap.put("fare", Math.round(finalFare));

                // Availability calculation
                Map<String, Object> avail = getLiveAvailability(conn, trainNumber, c, date);
                cMap.put("availabilityStatus", avail.get("status"));
                cMap.put("availabilityText", avail.get("text"));
                cMap.put("statusCode", avail.get("code")); // CNF, RAC, WL, REGRET

                classes.add(cMap);
            }
        }
        item.put("availableClasses", classes);
        return item;
    }

    public static double calculateFareForQuota(double baseFare, String quota, String classCode) {
        if ("TATKAL".equalsIgnoreCase(quota)) {
            double premium = classCode.contains("A") || classCode.equals("CC") 
                    ? Math.max(300, baseFare * 0.30) 
                    : Math.max(100, baseFare * 0.20);
            return baseFare + premium;
        } else if ("SENIOR_CITIZEN".equalsIgnoreCase(quota)) {
            return baseFare * 0.60; // 40% concession
        }
        return baseFare;
    }

    public static String getClassName(String code) {
        return switch (code) {
            case "1A" -> "AC First Class (1A)";
            case "2A" -> "AC 2 Tier (2A)";
            case "3A" -> "AC 3 Tier (3A)";
            case "SL" -> "Sleeper (SL)";
            case "CC" -> "AC Chair Car (CC)";
            default -> code;
        };
    }

    public static Map<String, Object> getLiveAvailability(Connection conn, String trainNumber, String classType, String date) {
        Map<String, Object> res = new HashMap<>();
        int cnfCap = CLASS_CAPACITIES.getOrDefault(classType, 64);
        int racCap = RAC_CAPACITIES.getOrDefault(classType, 8);
        int wlCap = WL_CAPACITIES.getOrDefault(classType, 30);

        String sql = """
            SELECT count(*) 
            FROM booking_passengers bp
            JOIN bookings b ON bp.pnr = b.pnr
            WHERE b.train_number = ? AND b.journey_date = ?::date AND b.class_type = ? AND b.booking_status != 'CANCELLED';
        """;

        int bookedCount = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trainNumber);
            ps.setString(2, (date != null && !date.isBlank()) ? date : LocalDate.now().toString());
            ps.setString(3, classType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bookedCount = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching booked count: " + e.getMessage());
        }

        if (bookedCount < cnfCap) {
            int left = cnfCap - bookedCount;
            res.put("status", "AVAILABLE-" + left);
            res.put("text", "AVAILABLE " + left);
            res.put("code", "AVAILABLE");
        } else if (bookedCount < (cnfCap + racCap)) {
            int racNum = bookedCount - cnfCap + 1;
            res.put("status", "RAC " + racNum);
            res.put("text", "RAC " + racNum);
            res.put("code", "RAC");
        } else if (bookedCount < (cnfCap + racCap + wlCap)) {
            int wlNum = bookedCount - cnfCap - racCap + 1;
            res.put("status", "WL " + wlNum);
            res.put("text", "GNWL " + wlNum);
            res.put("code", "WL");
        } else {
            res.put("status", "REGRET");
            res.put("text", "REGRET / FULL");
            res.put("code", "REGRET");
        }
        res.put("bookedCount", bookedCount);
        return res;
    }

    // 7-day availability matrix
    public static List<Map<String, Object>> getAvailabilityMatrix(String trainNumber, String classType, String baseDate) {
        List<Map<String, Object>> matrix = new ArrayList<>();
        LocalDate start = (baseDate != null && !baseDate.isBlank()) 
                ? LocalDate.parse(baseDate) 
                : LocalDate.now();

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE, dd MMM");

        try (Connection conn = DatabaseConfig.getConnection()) {
            for (int i = 0; i < 7; i++) {
                LocalDate d = start.plusDays(i);
                String dStr = d.format(dateFmt);
                Map<String, Object> avail = getLiveAvailability(conn, trainNumber, classType, dStr);
                
                Map<String, Object> dayInfo = new HashMap<>();
                dayInfo.put("date", dStr);
                dayInfo.put("displayDate", d.format(dayFmt));
                dayInfo.put("status", avail.get("status"));
                dayInfo.put("code", avail.get("code"));
                matrix.add(dayInfo);
            }
        } catch (SQLException e) {
            System.err.println("Error in getAvailabilityMatrix: " + e.getMessage());
        }
        return matrix;
    }

    // Interactive Visual Coach layout
    public static Map<String, Object> getCoachLayout(String trainNumber, String classType, String coachName, String journeyDate) {
        Map<String, Object> result = new HashMap<>();
        String coach = (coachName != null && !coachName.isBlank()) 
                ? coachName.trim().toUpperCase() 
                : switch (classType) {
                    case "1A" -> "H1";
                    case "2A" -> "A1";
                    case "3A" -> "B1";
                    case "SL" -> "S1";
                    case "CC" -> "C1";
                    default -> "B1";
                };

        int totalSeats = CLASS_CAPACITIES.getOrDefault(classType, 64);
        result.put("coach", coach);
        result.put("classType", classType);
        result.put("totalSeats", totalSeats);

        // Fetch already booked seats for this coach and date
        Map<Integer, Map<String, Object>> occupiedSeats = new HashMap<>();
        String sql = """
            SELECT bp.seat_number, bp.berth_type, bp.passenger_name, bp.gender, bp.age
            FROM booking_passengers bp
            JOIN bookings b ON bp.pnr = b.pnr
            WHERE b.train_number = ? AND b.journey_date = ?::date AND bp.coach = ? AND b.booking_status != 'CANCELLED';
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trainNumber);
            ps.setString(2, journeyDate);
            ps.setString(3, coach);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int seat = rs.getInt("seat_number");
                    Map<String, Object> pInfo = new HashMap<>();
                    pInfo.put("seatNumber", seat);
                    pInfo.put("berthType", rs.getString("berth_type"));
                    pInfo.put("passengerName", rs.getString("passenger_name"));
                    pInfo.put("gender", rs.getString("gender"));
                    pInfo.put("age", rs.getInt("age"));
                    occupiedSeats.put(seat, pInfo);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getCoachLayout: " + e.getMessage());
        }

        List<Map<String, Object>> seats = new ArrayList<>();
        for (int i = 1; i <= totalSeats; i++) {
            Map<String, Object> seat = new HashMap<>();
            seat.put("seatNumber", i);
            String bType = determineBerthType(classType, i);
            seat.put("berthType", bType);

            if (occupiedSeats.containsKey(i)) {
                seat.put("occupied", true);
                seat.put("passenger", occupiedSeats.get(i));
            } else {
                seat.put("occupied", false);
            }
            seats.add(seat);
        }

        result.put("seats", seats);
        return result;
    }

    public static List<Map<String, Object>> searchTrainsByQuery(String query, String date, String quota) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (query == null || query.isBlank()) return list;

        String qClean = query.trim();
        String quotaClean = (quota == null || quota.isBlank()) ? "GENERAL" : quota.trim().toUpperCase();

        String sql = """
            SELECT 
                train_number, train_name, train_type, zone,
                from_station_code AS board_code, from_station_name AS board_name, departure_time AS board_dep,
                to_station_code AS dest_code, to_station_name AS dest_name, arrival_time AS dest_arr,
                1 AS stop_diff,
                distance_km, duration_h, duration_m,
                has_1a, has_2a, has_3a, has_sl, has_cc,
                fare_1a, fare_2a, fare_3a, fare_sl, fare_cc
            FROM trains
            WHERE train_number = ? 
               OR train_number LIKE ? 
               OR LOWER(train_name) LIKE ?
            ORDER BY 
                CASE WHEN train_number = ? THEN 1 WHEN train_number LIKE ? THEN 2 ELSE 3 END,
                train_number ASC
            LIMIT 25;
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String numPrefix = qClean + "%";
            String nameLike = "%" + qClean.toLowerCase() + "%";

            ps.setString(1, qClean);
            ps.setString(2, numPrefix);
            ps.setString(3, nameLike);
            ps.setString(4, qClean);
            ps.setString(5, numPrefix);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTrainResult(conn, rs, date, quotaClean));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in searchTrainsByQuery: " + e.getMessage());
        }
        return list;
    }

    public static List<Map<String, Object>> lookupTrains(String query, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (query == null || query.isBlank()) return results;

        String q = query.trim();
        String sql = """
            SELECT train_number, train_name, from_station_code, from_station_name, to_station_code, to_station_name, train_type
            FROM trains
            WHERE train_number = ? OR train_number LIKE ? OR LOWER(train_name) LIKE ?
            ORDER BY CASE WHEN train_number = ? THEN 1 WHEN train_number LIKE ? THEN 2 ELSE 3 END, train_name ASC
            LIMIT ?;
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String numPrefix = q + "%";
            String namePattern = "%" + q.toLowerCase() + "%";

            ps.setString(1, q);
            ps.setString(2, numPrefix);
            ps.setString(3, namePattern);
            ps.setString(4, q);
            ps.setString(5, numPrefix);
            ps.setInt(6, limit > 0 ? limit : 10);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("trainNumber", rs.getString("train_number"));
                    map.put("trainName", rs.getString("train_name"));
                    map.put("fromStationCode", rs.getString("from_station_code"));
                    map.put("fromStationName", rs.getString("from_station_name"));
                    map.put("toStationCode", rs.getString("to_station_code"));
                    map.put("toStationName", rs.getString("to_station_name"));
                    map.put("trainType", rs.getString("train_type"));
                    results.add(map);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in lookupTrains: " + e.getMessage());
        }
        return results;
    }

    public static String determineBerthType(String classType, int seatNumber) {
        if ("CC".equals(classType)) {
            int mod = seatNumber % 5;
            return switch (mod) {
                case 1 -> "Window";
                case 2 -> "Middle";
                case 3 -> "Aisle";
                case 4 -> "Aisle";
                case 0 -> "Window";
                default -> "Window";
            };
        }

        if ("1A".equals(classType)) {
            return (seatNumber % 2 == 1) ? "Lower" : "Upper";
        }

        if ("2A".equals(classType)) {
            int mod = seatNumber % 6;
            return switch (mod) {
                case 1, 3 -> "Lower";
                case 2, 4 -> "Upper";
                case 5 -> "Side Lower";
                case 0 -> "Side Upper";
                default -> "Lower";
            };
        }

        // 3A & Sleeper standard 8-berth bay
        int mod = seatNumber % 8;
        return switch (mod) {
            case 1, 4 -> "Lower";
            case 2, 5 -> "Middle";
            case 3, 6 -> "Upper";
            case 7 -> "Side Lower";
            case 0 -> "Side Upper";
            default -> "Lower";
        };
    }
}
