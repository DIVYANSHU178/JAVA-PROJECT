import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TrainRunningStatusDAO {

    public static Map<String, Object> getLiveRunningStatus(String trainNumber, String journeyDate) {
        Map<String, Object> result = new HashMap<>();
        String tNum = trainNumber != null ? trainNumber.trim().toUpperCase() : "";

        // 1. Get Train info
        String trainSql = "SELECT train_number, train_name, train_type, from_station_name, to_station_name, departure_time, arrival_time FROM trains WHERE train_number = ?;";
        String trainName = "Express Train";
        String fromStation = "Origin";
        String toStation = "Destination";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(trainSql)) {
            ps.setString(1, tNum);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    trainName = rs.getString("train_name");
                    fromStation = rs.getString("from_station_name");
                    toStation = rs.getString("to_station_name");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching train: " + e.getMessage());
        }

        result.put("trainNumber", tNum);
        result.put("trainName", trainName);
        result.put("fromStation", fromStation);
        result.put("toStation", toStation);
        result.put("journeyDate", journeyDate);

        // 2. Fetch all route stops
        List<Map<String, Object>> stops = getTrainSchedule(tNum);
        result.put("totalStops", stops.size());

        if (stops.isEmpty()) {
            result.put("statusMessage", "Schedule not available for this train number.");
            result.put("stops", stops);
            return result;
        }

        // 3. Simulate Live NTES Position based on current time
        LocalTime now = LocalTime.now();
        int nowMinutes = now.getHour() * 60 + now.getMinute();

        int passedCount = 0;
        int delayMins = (int) ((trainNumber.hashCode() % 15 + 15) % 15); // Deterministic realistic delay 0-14 mins
        Map<String, Object> currentHalt = null;
        Map<String, Object> lastPassed = null;
        Map<String, Object> nextHalt = null;

        for (int i = 0; i < stops.size(); i++) {
            Map<String, Object> s = stops.get(i);
            String depStr = (String) s.get("departureTime");
            String arrStr = (String) s.get("arrivalTime");
            int depMins = parseTimeToMinutes(depStr);
            int arrMins = parseTimeToMinutes(arrStr);

            if (nowMinutes >= depMins && depMins > 0) {
                s.put("state", "PASSED");
                lastPassed = s;
                passedCount++;
            } else if (nowMinutes >= arrMins && nowMinutes < depMins) {
                s.put("state", "CURRENT");
                currentHalt = s;
                break;
            } else {
                s.put("state", "UPCOMING");
                if (nextHalt == null) {
                    nextHalt = s;
                }
            }
        }

        // If journey ended or not started yet
        if (passedCount == 0 && nextHalt == null && !stops.isEmpty()) {
            nextHalt = stops.get(0);
        } else if (passedCount == stops.size() && !stops.isEmpty()) {
            lastPassed = stops.get(stops.size() - 1);
        } else if (nextHalt == null && passedCount < stops.size()) {
            nextHalt = stops.get(passedCount);
        }

        result.put("stops", stops);
        result.put("delayMinutes", delayMins);
        result.put("delayStatus", delayMins == 0 ? "RIGHT TIME" : "RUNNING LATE BY " + delayMins + " MINS");

        if (currentHalt != null) {
            result.put("currentStatus", "Standing at " + currentHalt.get("stationName") + " (PF " + currentHalt.get("platform") + ")");
            result.put("speed", "0 km/h");
            result.put("location", currentHalt.get("stationName"));
        } else if (lastPassed != null && nextHalt != null) {
            result.put("currentStatus", "Departed " + lastPassed.get("stationName") + " ➔ Next Stop: " + nextHalt.get("stationName"));
            result.put("speed", "104 km/h");
            result.put("location", "Between " + lastPassed.get("stationName") + " & " + nextHalt.get("stationName"));
        } else if (lastPassed != null && nextHalt == null) {
            result.put("currentStatus", "Train reached destination " + lastPassed.get("stationName"));
            result.put("speed", "0 km/h");
            result.put("location", lastPassed.get("stationName"));
        } else {
            result.put("currentStatus", "Train yet to depart from origin " + fromStation);
            result.put("speed", "0 km/h");
            result.put("location", fromStation);
        }

        return result;
    }

    public static List<Map<String, Object>> getTrainSchedule(String trainNumber) {
        List<Map<String, Object>> stops = new ArrayList<>();
        String sql = """
            SELECT station_code, station_name, arrival_time, departure_time, stop_day, stop_sequence
            FROM train_route_stops
            WHERE train_number = ?
            ORDER BY stop_sequence ASC;
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trainNumber);
            try (ResultSet rs = ps.executeQuery()) {
                int dist = 0;
                while (rs.next()) {
                    Map<String, Object> s = new HashMap<>();
                    String scode = rs.getString("station_code");
                    String sname = rs.getString("station_name");
                    String arr = rs.getString("arrival_time");
                    String dep = rs.getString("departure_time");
                    int day = rs.getInt("stop_day");
                    int seq = rs.getInt("stop_sequence");

                    s.put("sequence", seq);
                    s.put("stationCode", scode);
                    s.put("stationName", sname);
                    s.put("arrivalTime", arr);
                    s.put("departureTime", dep);
                    s.put("day", day);

                    // Calculate realistic halt minutes
                    int arrM = parseTimeToMinutes(arr);
                    int depM = parseTimeToMinutes(dep);
                    int halt = (depM >= arrM) ? (depM - arrM) : 2;
                    if (halt == 0 && seq > 1) halt = 2;
                    s.put("haltMinutes", halt);

                    // Platform assignment
                    int pf = (Math.abs((trainNumber + scode).hashCode()) % 5) + 1;
                    s.put("platform", pf);

                    dist += (seq == 1) ? 0 : 45 + (seq * 8 % 60);
                    s.put("distanceKm", dist);

                    stops.add(s);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getTrainSchedule: " + e.getMessage());
        }
        return stops;
    }

    private static int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.isBlank() || "None".equalsIgnoreCase(timeStr)) return 0;
        try {
            String[] parts = timeStr.trim().split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return h * 60 + m;
        } catch (Exception e) {
            return 0;
        }
    }
}
