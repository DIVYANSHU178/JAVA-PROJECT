import java.util.*;

// Lightweight, pure Java JSON serializer & simple parser with zero external dependencies.
public class JsonUtil {

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String s) return "\"" + escape(s) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Collection<?> coll) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : coll) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof Train t) return trainToJson(t);
        if (obj instanceof Ticket ticket) return ticketToJson(ticket);
        if (obj instanceof Passenger p) return passengerToJson(p);

        return "\"" + escape(obj.toString()) + "\"";
    }

    public static String trainToJson(Train t) {
        return "{" +
                "\"trainNumber\":\"" + escape(t.getTrainNumber()) + "\"," +
                "\"trainName\":\"" + escape(t.getTrainName()) + "\"," +
                "\"sourceStation\":\"" + escape(t.getSourceStation()) + "\"," +
                "\"destStation\":\"" + escape(t.getDestStation()) + "\"," +
                "\"departureTime\":\"" + escape(t.getDepartureTime()) + "\"," +
                "\"arrivalTime\":\"" + escape(t.getArrivalTime()) + "\"," +
                "\"duration\":\"" + escape(t.getDuration()) + "\"," +
                "\"maxAc1Seats\":" + t.getMaxAc1Seats() + "," +
                "\"bookedAc1Seats\":" + t.getBookedAc1Seats() + "," +
                "\"availableAc1Seats\":" + t.getAvailableSeats(1) + "," +
                "\"ac1Fare\":" + t.getAc1Fare() + "," +
                "\"maxAc3Seats\":" + t.getMaxAc3Seats() + "," +
                "\"bookedAc3Seats\":" + t.getBookedAc3Seats() + "," +
                "\"availableAc3Seats\":" + t.getAvailableSeats(2) + "," +
                "\"ac3Fare\":" + t.getAc3Fare() + "," +
                "\"maxSleeperSeats\":" + t.getMaxSleeperSeats() + "," +
                "\"bookedSleeperSeats\":" + t.getBookedSleeperSeats() + "," +
                "\"availableSleeperSeats\":" + t.getAvailableSeats(3) + "," +
                "\"sleeperFare\":" + t.getSleeperFare() +
                "}";
    }

    public static String passengerToJson(Passenger p) {
        return "{" +
                "\"passengerNumber\":" + p.getPassengerNumber() + "," +
                "\"name\":\"" + escape(p.getName()) + "\"," +
                "\"phoneNumber\":\"" + escape(p.getPhoneNumber()) + "\"," +
                "\"age\":" + p.getAge() + "," +
                "\"gender\":\"" + escape(p.getGender()) + "\"," +
                "\"ticketClass\":" + p.getTicketClass() + "," +
                "\"coach\":\"" + escape(p.getCoach()) + "\"," +
                "\"seatNumber\":" + p.getSeatNumber() + "," +
                "\"berthType\":\"" + escape(p.getBerthType()) + "\"," +
                "\"pnr\":\"" + escape(p.getPnr()) + "\"," +
                "\"seatDisplay\":\"" + escape(p.getSeatAllocationDisplay()) + "\"" +
                "}";
    }

    public static String ticketToJson(Ticket t) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"pnr\":\"").append(escape(t.getPnr())).append("\",");
        sb.append("\"trainNumber\":\"").append(escape(t.getTrainNumber())).append("\",");
        sb.append("\"trainName\":\"").append(escape(t.getTrainName())).append("\",");
        sb.append("\"sourceStation\":\"").append(escape(t.getSourceStation())).append("\",");
        sb.append("\"destStation\":\"").append(escape(t.getDestStation())).append("\",");
        sb.append("\"journeyDate\":\"").append(escape(t.getJourneyDate())).append("\",");
        sb.append("\"bookingTime\":\"").append(escape(t.getBookingTime())).append("\",");
        sb.append("\"ticketClass\":").append(t.getTicketClass()).append(",");
        sb.append("\"ticketClassName\":\"").append(escape(t.getTicketClassName())).append("\",");
        sb.append("\"totalFare\":").append(t.getTotalFare()).append(",");
        sb.append("\"status\":\"").append(escape(t.getStatus())).append("\",");
        sb.append("\"refundAmount\":").append(t.getRefundAmount()).append(",");
        sb.append("\"cancellationTime\":").append(t.getCancellationTime() == null ? "null" : "\"" + escape(t.getCancellationTime()) + "\"").append(",");
        sb.append("\"passengerCount\":").append(t.getPassengerCount()).append(",");

        sb.append("\"passengers\":[");
        List<Passenger> passengers = t.getPassengers();
        for (int i = 0; i < passengers.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(passengerToJson(passengers.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    // Simple JSON value extractor for basic JSON request payloads
    public static String extractString(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static int extractInt(String json, String key, int defaultValue) {
        if (json == null) return defaultValue;
        String pattern = "\"" + key + "\"\\s*:\\s*([0-9]+)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
