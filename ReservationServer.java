import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// High-performance, lightweight embedded HTTP Web Server for RailStay IRCTC Engine.
public class ReservationServer {

    private final int port;
    private HttpServer server;

    public ReservationServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor()); // Java 21+ virtual threads

        // Static Web UI
        server.createContext("/", new StaticFileHandler());

        // Train & Station Endpoints
        server.createContext("/api/stations/search", new StationSearchHandler());
        server.createContext("/api/trains/search", new TrainSearchHandler());
        server.createContext("/api/trains/lookup", new TrainLookupHandler());
        server.createContext("/api/trains/availability", new AvailabilityMatrixHandler());
        server.createContext("/api/trains/schedule", new TrainScheduleHandler());
        server.createContext("/api/trains/live-status", new LiveRunningStatusHandler());
        server.createContext("/api/coach/layout", new CoachLayoutHandler());
        server.createContext("/api/tatkal/status", new TatkalStatusHandler());

        // Booking, PNR, & Cancellation
        server.createContext("/api/book", new BookingHandler());
        server.createContext("/api/bookings/create", new BookingHandler());
        server.createContext("/api/pnr", new PnrStatusHandler());
        server.createContext("/api/cancel", new CancelTicketHandler());
        server.createContext("/api/stats", new StatsHandler());

        // User Auth, Master Passenger List, & History
        server.createContext("/api/auth/register", new RegisterHandler());
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/user/bookings", new UserBookingsHandler());
        server.createContext("/api/user/master-passengers", new MasterPassengersHandler());
        server.createContext("/api/user/master-passengers/delete", new DeleteMasterPassengerHandler());

        server.start();
        System.out.println("==================================================================");
        System.out.println("🚆 RailStay IRCTC-Grade Railway Reservation Engine Running!");
        System.out.println("🌐 Access Web Interface: http://localhost:" + port);
        System.out.println("🗄️ Database: PostgreSQL (railstay_db on port 7777)");
        System.out.println("==================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // Static Web UI Handler
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path) || "/index.html".equals(path)) {
                String html = WebUI.getHtmlContent();
                sendResponse(exchange, 200, html, "text/html; charset=UTF-8");
            } else {
                sendResponse(exchange, 404, "Page Not Found", "text/plain");
            }
        }
    }

    // 1. GET /api/stations/search?q=...
    private static class StationSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String q = params.getOrDefault("q", "");
            int limit = 15;
            try {
                if (params.containsKey("limit")) limit = Integer.parseInt(params.get("limit"));
            } catch (NumberFormatException ignored) {}

            List<Map<String, Object>> stations = StationDAO.searchStations(q, limit);
            sendResponse(exchange, 200, JsonUtil.toJson(stations), "application/json; charset=UTF-8");
        }
    }

    // 2. GET /api/trains/search?from=...&to=...&date=...&quota=... OR ?query=...
    private static class TrainSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String query = params.get("query");
            if (query == null || query.isBlank()) query = params.get("q");
            if (query == null || query.isBlank()) query = params.get("trainNumber");
            String from = params.get("from");
            String to = params.get("to");
            String date = params.get("date");
            String quota = params.getOrDefault("quota", "GENERAL");

            if (query != null && !query.isBlank()) {
                List<Map<String, Object>> trains = TrainDAO.searchTrainsByQuery(query, date, quota);
                sendResponse(exchange, 200, JsonUtil.toJson(trains), "application/json; charset=UTF-8");
                return;
            }

            if (from == null || to == null) {
                sendResponse(exchange, 400, "{\"error\":\"from and to, or query parameter is required\"}", "application/json");
                return;
            }

            List<Map<String, Object>> trains = TrainDAO.searchTrains(from, to, date, quota);
            sendResponse(exchange, 200, JsonUtil.toJson(trains), "application/json; charset=UTF-8");
        }
    }

    // 2b. GET /api/trains/lookup?q=...
    private static class TrainLookupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String q = params.getOrDefault("q", "");
            List<Map<String, Object>> trains = TrainDAO.lookupTrains(q, 10);
            sendResponse(exchange, 200, JsonUtil.toJson(trains), "application/json; charset=UTF-8");
        }
    }

    // 3. GET /api/trains/availability?trainNumber=...&classType=...&date=...
    private static class AvailabilityMatrixHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String tNum = params.get("trainNumber");
            String cType = params.getOrDefault("classType", "3A");
            String date = params.get("date");

            if (tNum == null) {
                sendResponse(exchange, 400, "{\"error\":\"trainNumber parameter required\"}", "application/json");
                return;
            }

            List<Map<String, Object>> matrix = TrainDAO.getAvailabilityMatrix(tNum, cType, date);
            sendResponse(exchange, 200, JsonUtil.toJson(matrix), "application/json; charset=UTF-8");
        }
    }

    // 4. GET /api/trains/schedule?trainNumber=...
    private static class TrainScheduleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String tNum = params.get("trainNumber");
            if (tNum == null) {
                sendResponse(exchange, 400, "{\"error\":\"trainNumber is required\"}", "application/json");
                return;
            }

            List<Map<String, Object>> stops = TrainRunningStatusDAO.getTrainSchedule(tNum);
            sendResponse(exchange, 200, JsonUtil.toJson(stops), "application/json; charset=UTF-8");
        }
    }

    // 5. GET /api/trains/live-status?trainNumber=...&date=...
    private static class LiveRunningStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String tNum = params.get("trainNumber");
            if (tNum == null || tNum.isBlank()) tNum = params.get("q");
            String dayOffset = params.getOrDefault("dayOffset", "today");

            if (tNum == null || tNum.isBlank()) {
                sendResponse(exchange, 400, "{\"error\":\"trainNumber or q is required\"}", "application/json");
                return;
            }

            Map<String, Object> status = TrainRunningStatusDAO.getLiveRunningStatus(tNum, dayOffset);
            sendResponse(exchange, 200, JsonUtil.toJson(status), "application/json; charset=UTF-8");
        }
    }

    // 6. GET /api/tatkal/status
    private static class TatkalStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            LocalTime now = LocalTime.now();
            int currentMins = now.getHour() * 60 + now.getMinute();

            // AC Tatkal opens at 10:00 AM (600 mins), Non-AC opens at 11:00 AM (660 mins)
            boolean acOpen = currentMins >= 600;
            boolean nonAcOpen = currentMins >= 660;

            Map<String, Object> res = new HashMap<>();
            res.put("currentTime", now.toString());
            res.put("acTatkalOpen", acOpen);
            res.put("nonAcTatkalOpen", nonAcOpen);
            res.put("acOpeningTime", "10:00 AM IST");
            res.put("nonAcOpeningTime", "11:00 AM IST");
            res.put("message", acOpen && nonAcOpen ? "Tatkal Booking is Currently OPEN" : "Tatkal Booking opens at 10:00 AM (AC) & 11:00 AM (Non-AC)");

            sendResponse(exchange, 200, JsonUtil.toJson(res), "application/json; charset=UTF-8");
        }
    }

    // 7. GET /api/coach/layout?trainNumber=...&classType=...&coach=...&date=...
    private static class CoachLayoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String tNum = params.get("trainNumber");
            String cType = params.getOrDefault("classType", "3A");
            String coach = params.get("coach");
            String date = params.get("date");

            if (tNum == null || date == null) {
                sendResponse(exchange, 400, "{\"error\":\"trainNumber and date parameters required\"}", "application/json");
                return;
            }

            Map<String, Object> layout = TrainDAO.getCoachLayout(tNum, cType, coach, date);
            sendResponse(exchange, 200, JsonUtil.toJson(layout), "application/json; charset=UTF-8");
        }
    }

    // 8. POST /api/book
    private static class BookingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
                return;
            }

            String body = readRequestBody(exchange);
            try {
                String trainNumber = JsonUtil.extractString(body, "trainNumber");
                String journeyDate = JsonUtil.extractString(body, "journeyDate");
                String classType = JsonUtil.extractString(body, "classType");
                String quota = JsonUtil.extractString(body, "quota");
                String fromCode = JsonUtil.extractString(body, "fromStationCode");
                String toCode = JsonUtil.extractString(body, "toStationCode");
                String paymentMethod = JsonUtil.extractString(body, "paymentMethod");
                String paymentId = JsonUtil.extractString(body, "paymentId");
                int userId = JsonUtil.extractInt(body, "userId", 0);
                boolean insurance = body.contains("\"insuranceOpted\":true");

                if (trainNumber == null || journeyDate == null) {
                    sendResponse(exchange, 400, "{\"error\":\"Missing required booking parameters\"}", "application/json");
                    return;
                }

                List<BookingDAO.PassengerRequest> passengers = parsePassengers(body);
                if (passengers.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"At least one passenger is required\"}", "application/json");
                    return;
                }

                Map<String, Object> booking = BookingDAO.bookTicket(
                        userId > 0 ? userId : null,
                        trainNumber,
                        journeyDate,
                        classType,
                        quota,
                        fromCode,
                        toCode,
                        passengers,
                        paymentMethod != null ? paymentMethod : "UPI",
                        paymentId != null ? paymentId : "PAY-SIM-" + System.currentTimeMillis(),
                        insurance
                );
                sendResponse(exchange, 201, JsonUtil.toJson(booking), "application/json; charset=UTF-8");

            } catch (IllegalStateException e) {
                sendResponse(exchange, 409, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            }
        }

        private List<BookingDAO.PassengerRequest> parsePassengers(String json) {
            List<BookingDAO.PassengerRequest> list = new ArrayList<>();
            Pattern pArrayPattern = Pattern.compile("\"passengers\"\\s*:\\s*\\[([^\\]]+)\\]", Pattern.DOTALL);
            Matcher m = pArrayPattern.matcher(json);
            if (m.find()) {
                String arrayContent = m.group(1);
                String[] objects = arrayContent.split("\\},");
                for (String obj : objects) {
                    String name = JsonUtil.extractString(obj, "name");
                    String phone = JsonUtil.extractString(obj, "phone");
                    int age = JsonUtil.extractInt(obj, "age", 25);
                    String gender = JsonUtil.extractString(obj, "gender");
                    String berth = JsonUtil.extractString(obj, "berthPreference");
                    int seatNumber = JsonUtil.extractInt(obj, "seatNumber", 0);
                    if (seatNumber <= 0) {
                        seatNumber = JsonUtil.extractInt(obj, "preferredSeatNumber", 0);
                    }
                    String coach = JsonUtil.extractString(obj, "coach");
                    if (coach == null || coach.isBlank()) {
                        coach = JsonUtil.extractString(obj, "preferredCoach");
                    }

                    if (name != null && !name.isBlank()) {
                        list.add(new BookingDAO.PassengerRequest(
                                name,
                                age > 0 ? age : 25,
                                gender != null ? gender : "Male",
                                phone != null ? phone : "9876543210",
                                berth != null ? berth : "No Preference",
                                coach,
                                seatNumber > 0 ? seatNumber : null
                        ));
                    }
                }
            }
            return list;
        }
    }

    // 9. GET /api/pnr?pnr=...
    private static class PnrStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String pnr = params.get("pnr");
            if (pnr == null || pnr.isBlank()) {
                sendResponse(exchange, 400, "{\"error\":\"PNR parameter is required\"}", "application/json");
                return;
            }

            Map<String, Object> res = BookingDAO.getPnrStatus(pnr);
            if (res != null) {
                sendResponse(exchange, 200, JsonUtil.toJson(res), "application/json; charset=UTF-8");
            } else {
                sendResponse(exchange, 404, "{\"error\":\"PNR " + JsonUtil.escape(pnr) + " not found or expired.\"}", "application/json");
            }
        }
    }

    // 10. POST /api/cancel
    private static class CancelTicketHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
                return;
            }

            String body = readRequestBody(exchange);
            String pnr = JsonUtil.extractString(body, "pnr");
            if (pnr == null || pnr.isBlank()) {
                sendResponse(exchange, 400, "{\"error\":\"PNR parameter is required\"}", "application/json");
                return;
            }

            try {
                Map<String, Object> result = BookingDAO.cancelTicket(pnr);
                sendResponse(exchange, 200, JsonUtil.toJson(result), "application/json; charset=UTF-8");
            } catch (NoSuchElementException e) {
                sendResponse(exchange, 404, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            } catch (IllegalStateException e) {
                sendResponse(exchange, 409, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"Cancellation failed: " + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    // 11. POST /api/auth/register
    private static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
                return;
            }

            String body = readRequestBody(exchange);
            try {
                String username = JsonUtil.extractString(body, "username");
                String password = JsonUtil.extractString(body, "password");
                String fullName = JsonUtil.extractString(body, "fullName");
                String email = JsonUtil.extractString(body, "email");
                String phone = JsonUtil.extractString(body, "phone");

                Map<String, Object> user = UserDAO.register(username, password, fullName, email, phone);
                sendResponse(exchange, 201, JsonUtil.toJson(user), "application/json; charset=UTF-8");
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    // 12. POST /api/auth/login
    private static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
                return;
            }

            String body = readRequestBody(exchange);
            try {
                String username = JsonUtil.extractString(body, "username");
                String password = JsonUtil.extractString(body, "password");

                Map<String, Object> user = UserDAO.login(username, password);
                if (user != null) {
                    sendResponse(exchange, 200, JsonUtil.toJson(user), "application/json; charset=UTF-8");
                } else {
                    sendResponse(exchange, 401, "{\"error\":\"Invalid credentials\"}", "application/json");
                }
            } catch (Exception e) {
                sendResponse(exchange, 401, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    // 13. GET /api/user/bookings?userId=...
    private static class UserBookingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            int userId = 1;
            try {
                if (params.containsKey("userId")) userId = Integer.parseInt(params.get("userId"));
            } catch (NumberFormatException ignored) {}

            List<Map<String, Object>> bookings = UserDAO.getUserBookings(userId);
            sendResponse(exchange, 200, JsonUtil.toJson(bookings), "application/json; charset=UTF-8");
        }
    }

    // 14. GET & POST /api/user/master-passengers
    private static class MasterPassengersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                int userId = 1;
                try {
                    if (params.containsKey("userId")) userId = Integer.parseInt(params.get("userId"));
                } catch (NumberFormatException ignored) {}

                List<Map<String, Object>> list = UserDAO.getMasterPassengers(userId);
                sendResponse(exchange, 200, JsonUtil.toJson(list), "application/json; charset=UTF-8");

            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                try {
                    int userId = JsonUtil.extractInt(body, "userId", 1);
                    String name = JsonUtil.extractString(body, "name");
                    int age = JsonUtil.extractInt(body, "age", 30);
                    String gender = JsonUtil.extractString(body, "gender");
                    String berth = JsonUtil.extractString(body, "berthPreference");
                    String idType = JsonUtil.extractString(body, "idCardType");
                    String idNum = JsonUtil.extractString(body, "idCardNumber");

                    Map<String, Object> p = UserDAO.addMasterPassenger(userId, name, age, gender, berth, idType, idNum);
                    sendResponse(exchange, 201, JsonUtil.toJson(p), "application/json; charset=UTF-8");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            }
        }
    }

    // 15. POST /api/user/master-passengers/delete
    private static class DeleteMasterPassengerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            String body = readRequestBody(exchange);
            int id = JsonUtil.extractInt(body, "id", 0);
            int userId = JsonUtil.extractInt(body, "userId", 1);

            boolean ok = UserDAO.deleteMasterPassenger(id, userId);
            sendResponse(exchange, 200, "{\"success\":" + ok + "}", "application/json; charset=UTF-8");
        }
    }

    // 16. GET /api/stats
    private static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            Map<String, Object> stats = new HashMap<>();
            try (Connection conn = DatabaseConfig.getConnection();
                 Statement st = conn.createStatement()) {

                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM stations;")) {
                    if (rs.next()) stats.put("totalStations", rs.getInt(1));
                }
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM trains;")) {
                    if (rs.next()) stats.put("totalTrains", rs.getInt(1));
                }
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM train_route_stops;")) {
                    if (rs.next()) stats.put("totalRouteStops", rs.getInt(1));
                }
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM bookings;")) {
                    if (rs.next()) stats.put("totalBookings", rs.getInt(1));
                }
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM booking_passengers WHERE current_status = 'CNF';")) {
                    if (rs.next()) stats.put("confirmedPassengers", rs.getInt(1));
                }
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM users;")) {
                    if (rs.next()) stats.put("totalUsers", rs.getInt(1));
                }
            } catch (Exception e) {
                stats.put("error", e.getMessage());
            }

            sendResponse(exchange, 200, JsonUtil.toJson(stats), "application/json; charset=UTF-8");
        }
    }

    // HTTP Helper Methods
    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            } else if (kv.length == 1) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), "");
            }
        }
        return map;
    }

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        try {
            ReservationServer server = new ReservationServer(port);
            server.start();
        } catch (IOException e) {
            System.err.println("Could not start server on port " + port + ": " + e.getMessage());
        }
    }
}
