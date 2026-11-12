import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class BookingDAO {

    public static class PassengerRequest {
        public String name;
        public int age;
        public String gender;
        public String phone;
        public String berthPreference;

        public PassengerRequest(String name, int age, String gender, String phone, String berthPreference) {
            this.name = name;
            this.age = age;
            this.gender = gender;
            this.phone = phone;
            this.berthPreference = berthPreference;
        }
    }

    public static Map<String, Object> bookTicket(
            String trainNumber,
            String journeyDate,
            String classType,
            String quota,
            String fromStationCode,
            String toStationCode,
            List<PassengerRequest> passengers
    ) throws Exception {

        if (passengers == null || passengers.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required");
        }

        String pnr = generatePnr();
        String cType = (classType == null || classType.isBlank()) ? "3A" : classType.trim().toUpperCase();
        String q = (quota == null || quota.isBlank()) ? "GENERAL" : quota.trim().toUpperCase();

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false); // Begin ACID Transaction

            try {
                // 1. Fetch train details
                String trainSql = "SELECT train_name, from_station_name, to_station_name, fare_1a, fare_2a, fare_3a, fare_sl, fare_cc FROM trains WHERE train_number = ?;";
                String trainName = "Express";
                String fromName = fromStationCode;
                String toName = toStationCode;
                double baseFare = 1200.0;

                try (PreparedStatement ps = conn.prepareStatement(trainSql)) {
                    ps.setString(1, trainNumber);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            trainName = rs.getString("train_name");
                            fromName = rs.getString("from_station_name");
                            toName = rs.getString("to_station_name");
                            baseFare = switch (cType) {
                                case "1A" -> rs.getDouble("fare_1a");
                                case "2A" -> rs.getDouble("fare_2a");
                                case "3A" -> rs.getDouble("fare_3a");
                                case "SL" -> rs.getDouble("fare_sl");
                                case "CC" -> rs.getDouble("fare_cc");
                                default -> 1000.0;
                            };
                        }
                    }
                }

                double unitFare = TrainDAO.calculateFareForQuota(baseFare, q, cType);
                double totalFare = unitFare * passengers.size();

                // 2. Lock & count existing bookings for this train, date, and class
                String countSql = """
                    SELECT bp.coach, bp.seat_number, bp.current_status, bp.waitlist_number
                    FROM booking_passengers bp
                    JOIN bookings b ON bp.pnr = b.pnr
                    WHERE b.train_number = ? AND b.journey_date = ?::date AND b.class_type = ? AND b.booking_status != 'CANCELLED'
                    ORDER BY bp.passenger_id ASC
                    FOR UPDATE;
                """;

                Set<Integer> occupiedSeats = new HashSet<>();
                int racCount = 0;
                int wlCount = 0;

                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    ps.setString(1, trainNumber);
                    ps.setString(2, journeyDate);
                    ps.setString(3, cType);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String st = rs.getString("current_status");
                            int sNum = rs.getInt("seat_number");
                            if (sNum > 0 && "CNF".equalsIgnoreCase(st)) {
                                occupiedSeats.add(sNum);
                            } else if ("RAC".equalsIgnoreCase(st)) {
                                racCount++;
                            } else if ("WL".equalsIgnoreCase(st)) {
                                wlCount++;
                            }
                        }
                    }
                }

                int cnfCap = TrainDAO.CLASS_CAPACITIES.getOrDefault(cType, 64);
                int racCap = TrainDAO.RAC_CAPACITIES.getOrDefault(cType, 8);
                int wlCap = TrainDAO.WL_CAPACITIES.getOrDefault(cType, 30);

                // Default coach name
                String coachPrefix = switch (cType) {
                    case "1A" -> "H1";
                    case "2A" -> "A1";
                    case "3A" -> "B1";
                    case "SL" -> "S1";
                    case "CC" -> "C1";
                    default -> "B1";
                };

                // 3. Insert Master Booking Record
                String insertBookingSql = """
                    INSERT INTO bookings (pnr, train_number, train_name, journey_date, from_station_code, from_station_name, to_station_code, to_station_name, class_type, quota, total_fare, booking_status)
                    VALUES (?, ?, ?, ?::date, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMED');
                """;
                try (PreparedStatement ps = conn.prepareStatement(insertBookingSql)) {
                    ps.setString(1, pnr);
                    ps.setString(2, trainNumber);
                    ps.setString(3, trainName);
                    ps.setString(4, journeyDate);
                    ps.setString(5, fromStationCode);
                    ps.setString(6, fromName);
                    ps.setString(7, toStationCode);
                    ps.setString(8, toName);
                    ps.setString(9, cType);
                    ps.setString(10, q);
                    ps.setDouble(11, totalFare);
                    ps.executeUpdate();
                }

                // 4. Allocate Seats & Insert Passengers
                String insertPassengerSql = """
                    INSERT INTO booking_passengers (pnr, passenger_name, age, gender, phone, coach, seat_number, berth_type, booking_status, current_status, waitlist_number)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

                List<Map<String, Object>> allocatedPassengers = new ArrayList<>();

                try (PreparedStatement ps = conn.prepareStatement(insertPassengerSql)) {
                    for (PassengerRequest pr : passengers) {
                        String status;
                        String coach = null;
                        int seatNum = 0;
                        String berthType = null;
                        int wlNum = 0;

                        if (occupiedSeats.size() < cnfCap) {
                            // Find next free seat matching preference if possible
                            int assignedSeat = findBestSeat(occupiedSeats, cnfCap, cType, pr.berthPreference);
                            occupiedSeats.add(assignedSeat);
                            coach = coachPrefix;
                            seatNum = assignedSeat;
                            berthType = TrainDAO.determineBerthType(cType, assignedSeat);
                            status = "CNF";
                        } else if (racCount < racCap) {
                            racCount++;
                            status = "RAC";
                            coach = coachPrefix;
                            seatNum = 7; // Standard RAC side-lower
                            berthType = "Side Lower (RAC " + racCount + ")";
                        } else if (wlCount < wlCap) {
                            wlCount++;
                            status = "WL";
                            wlNum = wlCount;
                            berthType = "Waitlist (WL " + wlNum + ")";
                        } else {
                            throw new IllegalStateException("Booking closed: Maximum waitlist limit reached for this train and date.");
                        }

                        ps.setString(1, pnr);
                        ps.setString(2, pr.name);
                        ps.setInt(3, pr.age);
                        ps.setString(4, pr.gender);
                        ps.setString(5, pr.phone);
                        ps.setString(6, coach);
                        if (seatNum > 0) ps.setInt(7, seatNum); else ps.setNull(7, Types.INTEGER);
                        ps.setString(8, berthType);
                        ps.setString(9, status);
                        ps.setString(10, status);
                        ps.setInt(11, wlNum);
                        ps.addBatch();

                        Map<String, Object> pMap = new HashMap<>();
                        pMap.put("name", pr.name);
                        pMap.put("age", pr.age);
                        pMap.put("gender", pr.gender);
                        pMap.put("phone", pr.phone);
                        pMap.put("coach", coach);
                        pMap.put("seatNumber", seatNum > 0 ? seatNum : "None");
                        pMap.put("berthType", berthType);
                        pMap.put("bookingStatus", status);
                        pMap.put("currentStatus", status);
                        pMap.put("waitlistNumber", wlNum);
                        allocatedPassengers.add(pMap);
                    }
                    ps.executeBatch();
                }

                conn.commit(); // Transaction Committed

                Map<String, Object> response = new HashMap<>();
                response.put("pnr", pnr);
                response.put("trainNumber", trainNumber);
                response.put("trainName", trainName);
                response.put("journeyDate", journeyDate);
                response.put("fromStationCode", fromStationCode);
                response.put("fromStationName", fromName);
                response.put("toStationCode", toStationCode);
                response.put("toStationName", toName);
                response.put("classType", cType);
                response.put("quota", q);
                response.put("totalFare", totalFare);
                response.put("bookingStatus", "CONFIRMED");
                response.put("passengers", allocatedPassengers);

                return response;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static int findBestSeat(Set<Integer> occupied, int capacity, String classType, String pref) {
        // Preferred seat match first
        if (pref != null && !pref.isBlank() && !"No Preference".equalsIgnoreCase(pref)) {
            for (int i = 1; i <= capacity; i++) {
                if (!occupied.contains(i)) {
                    String b = TrainDAO.determineBerthType(classType, i);
                    if (b.equalsIgnoreCase(pref.trim())) {
                        return i;
                    }
                }
            }
        }
        // Fallback to first available
        for (int i = 1; i <= capacity; i++) {
            if (!occupied.contains(i)) {
                return i;
            }
        }
        return occupied.size() + 1;
    }

    public static Map<String, Object> getPnrStatus(String pnr) {
        if (pnr == null || pnr.isBlank()) return null;
        String cleanPnr = pnr.trim().toUpperCase();

        String bookingSql = """
            SELECT b.*, t.departure_time, t.arrival_time
            FROM bookings b
            LEFT JOIN trains t ON b.train_number = t.train_number
            WHERE b.pnr = ?;
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(bookingSql)) {
            ps.setString(1, cleanPnr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("pnr", rs.getString("pnr"));
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
                    map.put("chartStatus", "CHART PREPARED");

                    // Passengers list
                    List<Map<String, Object>> passengers = new ArrayList<>();
                    String pSql = "SELECT * FROM booking_passengers WHERE pnr = ? ORDER BY passenger_id ASC;";
                    try (PreparedStatement pPs = conn.prepareStatement(pSql)) {
                        pPs.setString(1, cleanPnr);
                        try (ResultSet prs = pPs.executeQuery()) {
                            while (prs.next()) {
                                Map<String, Object> p = new HashMap<>();
                                p.put("passengerId", prs.getInt("passenger_id"));
                                p.put("name", prs.getString("passenger_name"));
                                p.put("age", prs.getInt("age"));
                                p.put("gender", prs.getString("gender"));
                                p.put("phone", prs.getString("phone"));
                                p.put("coach", prs.getString("coach"));
                                p.put("seatNumber", prs.getInt("seat_number"));
                                p.put("berthType", prs.getString("berth_type"));
                                p.put("bookingStatus", prs.getString("booking_status"));
                                p.put("currentStatus", prs.getString("current_status"));
                                p.put("waitlistNumber", prs.getInt("waitlist_number"));
                                passengers.add(p);
                            }
                        }
                    }
                    map.put("passengers", passengers);
                    return map;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getPnrStatus: " + e.getMessage());
        }
        return null;
    }

    // Cancellation with Automated RAC & WL Promotion Engine
    public static Map<String, Object> cancelTicket(String pnr) throws Exception {
        if (pnr == null || pnr.isBlank()) {
            throw new IllegalArgumentException("PNR cannot be empty");
        }

        String cleanPnr = pnr.trim().toUpperCase();

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false); // ACID Transaction

            try {
                // 1. Fetch and Lock booking
                String bSql = "SELECT * FROM bookings WHERE pnr = ? FOR UPDATE;";
                String trainNumber = null;
                String journeyDate = null;
                String classType = null;
                double totalFare = 0.0;
                String status = null;

                try (PreparedStatement ps = conn.prepareStatement(bSql)) {
                    ps.setString(1, cleanPnr);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new NoSuchElementException("No booking found for PNR: " + cleanPnr);
                        }
                        status = rs.getString("booking_status");
                        if ("CANCELLED".equalsIgnoreCase(status)) {
                            throw new IllegalStateException("Ticket with PNR " + cleanPnr + " is already cancelled.");
                        }
                        trainNumber = rs.getString("train_number");
                        journeyDate = rs.getString("journey_date");
                        classType = rs.getString("class_type");
                        totalFare = rs.getDouble("total_fare");
                    }
                }

                // 2. Fetch passengers and vacated confirmed seats
                List<Map<String, Object>> cancelledPassengers = new ArrayList<>();
                List<Integer> vacatedSeats = new ArrayList<>();
                String coachName = "B1";

                String pSql = "SELECT * FROM booking_passengers WHERE pnr = ? FOR UPDATE;";
                try (PreparedStatement ps = conn.prepareStatement(pSql)) {
                    ps.setString(1, cleanPnr);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int sNum = rs.getInt("seat_number");
                            String cStatus = rs.getString("current_status");
                            String c = rs.getString("coach");
                            if (c != null) coachName = c;
                            if (sNum > 0 && "CNF".equalsIgnoreCase(cStatus)) {
                                vacatedSeats.add(sNum);
                            }
                            Map<String, Object> p = new HashMap<>();
                            p.put("name", rs.getString("passenger_name"));
                            p.put("seatNumber", sNum);
                            p.put("status", cStatus);
                            cancelledPassengers.add(p);
                        }
                    }
                }

                // 3. Mark booking as CANCELLED
                String updateBSql = "UPDATE bookings SET booking_status = 'CANCELLED' WHERE pnr = ?;";
                try (PreparedStatement ps = conn.prepareStatement(updateBSql)) {
                    ps.setString(1, cleanPnr);
                    ps.executeUpdate();
                }

                String updatePSql = "UPDATE booking_passengers SET current_status = 'CANCELLED' WHERE pnr = ?;";
                try (PreparedStatement ps = conn.prepareStatement(updatePSql)) {
                    ps.setString(1, cleanPnr);
                    ps.executeUpdate();
                }

                // 4. Calculate Refund (IRCTC deduction rules)
                double deductionPerPassenger = switch (classType) {
                    case "1A" -> 240.0;
                    case "2A" -> 200.0;
                    case "3A", "CC" -> 180.0;
                    default -> 120.0;
                };
                double totalDeduction = deductionPerPassenger * cancelledPassengers.size();
                double refundAmount = Math.max(0.0, totalFare - totalDeduction);

                String refundSql = "INSERT INTO cancellations_refunds (pnr, original_fare, deduction, refund_amount) VALUES (?, ?, ?, ?);";
                try (PreparedStatement ps = conn.prepareStatement(refundSql)) {
                    ps.setString(1, cleanPnr);
                    ps.setDouble(2, totalFare);
                    ps.setDouble(3, totalDeduction);
                    ps.setDouble(4, refundAmount);
                    ps.executeUpdate();
                }

                // 5. Automated Promotion Engine:
                // For each vacated seat, promote the earliest RAC passenger to CNF
                for (int seat : vacatedSeats) {
                    String racFindSql = """
                        SELECT bp.passenger_id, bp.pnr
                        FROM booking_passengers bp
                        JOIN bookings b ON bp.pnr = b.pnr
                        WHERE b.train_number = ? AND b.journey_date = ?::date AND b.class_type = ? 
                          AND bp.current_status = 'RAC' AND b.booking_status != 'CANCELLED'
                        ORDER BY bp.passenger_id ASC
                        LIMIT 1
                        FOR UPDATE;
                    """;

                    Integer racPid = null;
                    try (PreparedStatement ps = conn.prepareStatement(racFindSql)) {
                        ps.setString(1, trainNumber);
                        ps.setString(2, journeyDate);
                        ps.setString(3, classType);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                racPid = rs.getInt("passenger_id");
                            }
                        }
                    }

                    if (racPid != null) {
                        String bType = TrainDAO.determineBerthType(classType, seat);
                        String promoteRacSql = """
                            UPDATE booking_passengers 
                            SET current_status = 'CNF', coach = ?, seat_number = ?, berth_type = ?
                            WHERE passenger_id = ?;
                        """;
                        try (PreparedStatement ps = conn.prepareStatement(promoteRacSql)) {
                            ps.setString(1, coachName);
                            ps.setInt(2, seat);
                            ps.setString(3, bType);
                            ps.setInt(4, racPid);
                            ps.executeUpdate();
                        }

                        // Now promote earliest WL passenger to RAC
                        String wlFindSql = """
                            SELECT bp.passenger_id, bp.pnr
                            FROM booking_passengers bp
                            JOIN bookings b ON bp.pnr = b.pnr
                            WHERE b.train_number = ? AND b.journey_date = ?::date AND b.class_type = ? 
                              AND bp.current_status = 'WL' AND b.booking_status != 'CANCELLED'
                            ORDER BY bp.passenger_id ASC
                            LIMIT 1
                            FOR UPDATE;
                        """;

                        Integer wlPid = null;
                        try (PreparedStatement ps = conn.prepareStatement(wlFindSql)) {
                            ps.setString(1, trainNumber);
                            ps.setString(2, journeyDate);
                            ps.setString(3, classType);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    wlPid = rs.getInt("passenger_id");
                                }
                            }
                        }

                        if (wlPid != null) {
                            String promoteWlSql = """
                                UPDATE booking_passengers 
                                SET current_status = 'RAC', berth_type = 'Side Lower (RAC)'
                                WHERE passenger_id = ?;
                            """;
                            try (PreparedStatement ps = conn.prepareStatement(promoteWlSql)) {
                                ps.setInt(1, wlPid);
                                ps.executeUpdate();
                            }
                        }
                    }
                }

                conn.commit(); // Transaction Committed

                Map<String, Object> res = new HashMap<>();
                res.put("pnr", cleanPnr);
                res.put("status", "CANCELLED");
                res.put("originalFare", totalFare);
                res.put("deduction", totalDeduction);
                res.put("refundAmount", refundAmount);
                res.put("message", "Ticket cancelled successfully. Refund of ₹" + refundAmount + " has been initiated to original payment method.");
                return res;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static String generatePnr() {
        // 10-digit authentic railway PNR
        long firstThree = 200 + (long) (Math.random() * 800);
        long nextSeven = 1000000 + (long) (Math.random() * 9000000);
        return "" + firstThree + nextSeven;
    }
}
