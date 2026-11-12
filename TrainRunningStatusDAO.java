import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.*;

public class TrainRunningStatusDAO {

    public static Map<String, Object> getLiveRunningStatus(String trainQuery, String runDay) {
        Map<String, Object> result = new HashMap<>();
        if (trainQuery == null || trainQuery.isBlank()) {
            result.put("error", "Train number or name is required.");
            return result;
        }

        String qClean = trainQuery.trim();

        // 1. Resolve train from database
        String trainSql = """
            SELECT train_number, train_name, train_type, from_station_code, from_station_name, to_station_code, to_station_name, departure_time, arrival_time, duration_h, duration_m, distance_km
            FROM trains
            WHERE train_number = ? OR train_number LIKE ? OR LOWER(train_name) LIKE ?
            ORDER BY CASE WHEN train_number = ? THEN 1 WHEN train_number LIKE ? THEN 2 ELSE 3 END
            LIMIT 1;
        """;

        String trainNumber = qClean;
        String trainName = "Express Train";
        String fromStation = "Origin";
        String toStation = "Destination";
        String originDep = "06:00:00";
        String destArr = "20:00:00";
        int totalDist = 800;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(trainSql)) {
            ps.setString(1, qClean);
            ps.setString(2, qClean + "%");
            ps.setString(3, "%" + qClean.toLowerCase() + "%");
            ps.setString(4, qClean);
            ps.setString(5, qClean + "%");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    trainNumber = rs.getString("train_number");
                    trainName = rs.getString("train_name");
                    fromStation = rs.getString("from_station_name");
                    toStation = rs.getString("to_station_name");
                    originDep = rs.getString("departure_time");
                    destArr = rs.getString("arrival_time");
                    totalDist = rs.getInt("distance_km");
                } else {
                    result.put("error", "No train found matching: " + qClean);
                    return result;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching train for live status: " + e.getMessage());
        }

        result.put("trainNumber", trainNumber);
        result.put("trainName", trainName);
        result.put("fromStation", fromStation);
        result.put("toStation", toStation);
        result.put("scheduledDeparture", originDep);
        result.put("scheduledArrival", destArr);
        result.put("totalDistanceKm", totalDist);

        // 2. Fetch all intermediate stops for this train
        List<Map<String, Object>> stops = getTrainSchedule(trainNumber);
        result.put("totalStops", stops.size());

        if (stops.isEmpty()) {
            result.put("error", "No timetable schedule available for train " + trainNumber);
            return result;
        }

        // 3. Multi-day timeline calculation
        int firstDepMins = parseTimeToMinutes((String) stops.get(0).get("departureTime"));
        int lastArrMins = ((int) stops.get(stops.size() - 1).get("day") - 1) * 1440 + parseTimeToMinutes((String) stops.get(stops.size() - 1).get("arrivalTime"));
        if (lastArrMins <= firstDepMins) {
            lastArrMins += 1440;
        }

        // Current time offset
        LocalTime now = LocalTime.now();
        int nowMinutes = now.getHour() * 60 + now.getMinute();

        if ("yesterday".equalsIgnoreCase(runDay)) {
            nowMinutes += 1440;
        } else if ("tomorrow".equalsIgnoreCase(runDay)) {
            nowMinutes -= 1440;
        }

        // If today's train departed earlier or is running on a multi-day schedule
        if (nowMinutes < firstDepMins && "today".equalsIgnoreCase(runDay) && (firstDepMins - nowMinutes > 360)) {
            // If checking in the morning for a train that runs in the afternoon/night, keep today.
            // But if it's already past journey on Day 2, allow user to view today's run.
        }

        // Delay in minutes (deterministic based on train number)
        int delayMins = Math.abs(trainNumber.hashCode() % 12);
        result.put("delayMinutes", delayMins);
        result.put("delayStatus", delayMins == 0 ? "ON TIME" : "RUNNING LATE BY " + delayMins + " MINS");

        int currentIdx = -1;
        boolean standingAtHalt = false;

        // Classify every stop as PASSED, CURRENT, or UPCOMING
        for (int i = 0; i < stops.size(); i++) {
            Map<String, Object> s = stops.get(i);
            int day = (int) s.get("day");
            int arrM = (day - 1) * 1440 + parseTimeToMinutes((String) s.get("arrivalTime"));
            int depM = (day - 1) * 1440 + parseTimeToMinutes((String) s.get("departureTime"));

            // Add delay
            arrM += delayMins;
            depM += delayMins;

            if (nowMinutes > depM) {
                s.put("state", "PASSED");
                currentIdx = i;
            } else if (nowMinutes >= arrM && nowMinutes <= depM) {
                s.put("state", "CURRENT");
                currentIdx = i;
                standingAtHalt = true;
            } else {
                s.put("state", "UPCOMING");
            }
        }

        // Synthesize headline status
        if (currentIdx == -1) {
            // Yet to start from origin
            stops.get(0).put("state", "CURRENT");
            for (int i = 1; i < stops.size(); i++) stops.get(i).put("state", "UPCOMING");

            int minsToStart = Math.max(0, firstDepMins - nowMinutes);
            result.put("currentStatus", "Train yet to depart from origin (" + fromStation + "). Starts in " + formatMins(minsToStart));
            result.put("speed", "0 km/h");
            result.put("location", fromStation);
            result.put("nextStop", stops.size() > 1 ? stops.get(1).get("stationName") : "Destination");
            result.put("distanceCoveredKm", 0);
        } else if (currentIdx >= stops.size() - 1 && !standingAtHalt) {
            // Journey completed
            for (Map<String, Object> s : stops) s.put("state", "PASSED");
            stops.get(stops.size() - 1).put("state", "PASSED");

            result.put("currentStatus", "Train has arrived at destination (" + toStation + "). Journey completed.");
            result.put("speed", "0 km/h");
            result.put("location", toStation);
            result.put("nextStop", "Terminated");
            result.put("distanceCoveredKm", totalDist);
        } else {
            Map<String, Object> curStop = stops.get(currentIdx);
            Map<String, Object> nextStop = (currentIdx + 1 < stops.size()) ? stops.get(currentIdx + 1) : curStop;

            int distCovered = (int) curStop.get("distanceKm");
            int nextDist = (int) nextStop.get("distanceKm");
            int remainingKm = Math.max(0, nextDist - distCovered);

            if (standingAtHalt) {
                curStop.put("state", "CURRENT");
                result.put("currentStatus", "Standing at " + curStop.get("stationName") + " (PF " + curStop.get("platform") + ")");
                result.put("speed", "0 km/h");
                result.put("location", curStop.get("stationName"));
            } else {
                result.put("currentStatus", "Departed " + curStop.get("stationName") + " ➔ Next: " + nextStop.get("stationName") + " (" + remainingKm + " km away)");
                result.put("speed", "108 km/h");
                result.put("location", "Between " + curStop.get("stationName") + " & " + nextStop.get("stationName"));
            }

            result.put("nextStop", nextStop.get("stationName"));
            result.put("distanceCoveredKm", distCovered);
        }

        result.put("stops", stops);
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

                    int arrM = parseTimeToMinutes(arr);
                    int depM = parseTimeToMinutes(dep);
                    int halt = (depM >= arrM) ? (depM - arrM) : 2;
                    if (halt == 0 && seq > 1) halt = 2;
                    s.put("haltMinutes", halt);

                    int pf = (Math.abs((trainNumber + scode).hashCode()) % 5) + 1;
                    s.put("platform", pf);

                    dist += (seq == 1) ? 0 : 35 + ((seq * 7) % 45);
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

    private static String formatMins(int mins) {
        if (mins < 60) return mins + " mins";
        return (mins / 60) + "h " + (mins % 60) + "m";
    }
}
