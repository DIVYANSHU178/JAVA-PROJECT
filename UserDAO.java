import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;

public class UserDAO {

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    public static Map<String, Object> register(String username, String password, String fullName, String email, String phone) throws Exception {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password cannot be empty");
        }

        String sql = """
            INSERT INTO users (username, password_hash, full_name, email, phone)
            VALUES (?, ?, ?, ?, ?)
            RETURNING user_id, username, full_name, email, phone, created_at;
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, hashPassword(password));
            ps.setString(3, fullName != null ? fullName.trim() : username);
            ps.setString(4, email != null ? email.trim() : username + "@irctc.portal");
            ps.setString(5, phone != null ? phone.trim() : "9876543210");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> u = new HashMap<>();
                    u.put("userId", rs.getInt("user_id"));
                    u.put("username", rs.getString("username"));
                    u.put("fullName", rs.getString("full_name"));
                    u.put("email", rs.getString("email"));
                    u.put("phone", rs.getString("phone"));
                    return u;
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("users_username_key")) {
                throw new IllegalStateException("Username already taken. Please choose another.");
            } else if (e.getMessage().contains("users_email_key")) {
                throw new IllegalStateException("Email address already registered.");
            }
            throw e;
        }
        return null;
    }

    public static Map<String, Object> login(String username, String password) throws Exception {
        if (username == null || password == null) return null;
        String cleanUser = username.trim().toLowerCase();
        String passHash = hashPassword(password);

        String sql = "SELECT user_id, username, password_hash, full_name, email, phone FROM users WHERE LOWER(username) = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    // Support both plain text legacy and SHA-256 hash
                    if (storedHash.equals(password) || storedHash.equals(passHash)) {
                        Map<String, Object> u = new HashMap<>();
                        u.put("userId", rs.getInt("user_id"));
                        u.put("username", rs.getString("username"));
                        u.put("fullName", rs.getString("full_name"));
                        u.put("email", rs.getString("email"));
                        u.put("phone", rs.getString("phone"));
                        return u;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Invalid username or password.");
    }

    public static List<Map<String, Object>> getMasterPassengers(int userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM master_passengers WHERE user_id = ? ORDER BY id ASC;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("id", rs.getInt("id"));
                    p.put("passengerName", rs.getString("passenger_name"));
                    p.put("age", rs.getInt("age"));
                    p.put("gender", rs.getString("gender"));
                    p.put("berthPreference", rs.getString("berth_preference"));
                    p.put("idCardType", rs.getString("id_card_type"));
                    p.put("idCardNumber", rs.getString("id_card_number"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching master passengers: " + e.getMessage());
        }
        return list;
    }

    public static Map<String, Object> addMasterPassenger(int userId, String name, int age, String gender, String berthPref, String idType, String idNum) throws SQLException {
        String sql = """
            INSERT INTO master_passengers (user_id, passenger_name, age, gender, berth_preference, id_card_type, id_card_number)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id, passenger_name, age, gender, berth_preference, id_card_type, id_card_number;
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setInt(3, age);
            ps.setString(4, gender);
            ps.setString(5, berthPref != null ? berthPref : "No Preference");
            ps.setString(6, idType != null ? idType : "Aadhaar Card");
            ps.setString(7, idNum != null ? idNum : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("id", rs.getInt("id"));
                    p.put("passengerName", rs.getString("passenger_name"));
                    p.put("age", rs.getInt("age"));
                    p.put("gender", rs.getString("gender"));
                    p.put("berthPreference", rs.getString("berth_preference"));
                    p.put("idCardType", rs.getString("id_card_type"));
                    p.put("idCardNumber", rs.getString("id_card_number"));
                    return p;
                }
            }
        }
        return null;
    }

    public static boolean deleteMasterPassenger(int masterId, int userId) {
        String sql = "DELETE FROM master_passengers WHERE id = ? AND user_id = ?;";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, masterId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static List<Map<String, Object>> getUserBookings(int userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT b.*, t.departure_time, t.arrival_time
            FROM bookings b
            LEFT JOIN trains t ON b.train_number = t.train_number
            WHERE b.user_id = ?
            ORDER BY b.created_at DESC;
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String pnr = rs.getString("pnr");
                    Map<String, Object> map = new HashMap<>();
                    map.put("pnr", pnr);
                    map.put("trainNumber", rs.getString("train_number"));
                    map.put("trainName", rs.getString("train_name"));
                    map.put("journeyDate", rs.getString("journey_date"));
                    map.put("fromStationCode", rs.getString("from_station_code"));
                    map.put("fromStationName", rs.getString("from_station_name"));
                    map.put("toStationCode", rs.getString("to_station_code"));
                    map.put("toStationName", rs.getString("to_station_name"));
                    map.put("departureTime", rs.getString("departure_time"));
                    map.put("arrivalTime", rs.getString("arrival_time"));
                    map.put("classType", rs.getString("class_type"));
                    map.put("quota", rs.getString("quota"));
                    map.put("totalFare", rs.getDouble("total_fare"));
                    map.put("bookingStatus", rs.getString("booking_status"));
                    map.put("paymentMethod", rs.getString("payment_method"));
                    map.put("insuranceOpted", rs.getBoolean("insurance_opted"));

                    // Fetch passengers for this booking
                    List<Map<String, Object>> passengers = new ArrayList<>();
                    String pSql = "SELECT * FROM booking_passengers WHERE pnr = ? ORDER BY passenger_id ASC;";
                    try (PreparedStatement pPs = conn.prepareStatement(pSql)) {
                        pPs.setString(1, pnr);
                        try (ResultSet prs = pPs.executeQuery()) {
                            while (prs.next()) {
                                Map<String, Object> p = new HashMap<>();
                                p.put("name", prs.getString("passenger_name"));
                                p.put("age", prs.getInt("age"));
                                p.put("gender", prs.getString("gender"));
                                p.put("coach", prs.getString("coach"));
                                p.put("seatNumber", prs.getInt("seat_number"));
                                p.put("berthType", prs.getString("berth_type"));
                                p.put("currentStatus", prs.getString("current_status"));
                                passengers.add(p);
                            }
                        }
                    }
                    map.put("passengers", passengers);
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getUserBookings: " + e.getMessage());
        }
        return list;
    }
}
