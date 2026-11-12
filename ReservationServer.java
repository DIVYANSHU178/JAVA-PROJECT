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

        // Real IRCTC API Endpoints
        server.createContext("/api/stations/search", new StationSearchHandler());
        server.createContext("/api/trains/search", new TrainSearchHandler());
        server.createContext("/api/trains/availability", new AvailabilityMatrixHandler());
        server.createContext("/api/coach/layout", new CoachLayoutHandler());
        server.createContext("/api/book", new BookingHandler());
        server.createContext("/api/pnr", new PnrStatusHandler());
        server.createContext("/api/cancel", new CancelTicketHandler());
        server.createContext("/api/stats", new StatsHandler());

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

    // 2. GET /api/trains/search?from=...&to=...&date=...&quota=...
    private static class TrainSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String from = params.get("from");
            String to = params.get("to");
            String date = params.get("date");
            String quota = params.getOrDefault("quota", "GENERAL");

            if (from == null || to == null) {
                sendResponse(exchange, 400, "{\"error\":\"from and to query parameters are required\"}", "application/json");
                return;
            }

            List<Map<String, Object>> trains = TrainDAO.searchTrains(from, to, date, quota);
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

    // 4. GET /api/coach/layout?trainNumber=...&classType=...&coach=...&date=...
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

    // 5. POST /api/book
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

                if (trainNumber == null || journeyDate == null) {
                    sendResponse(exchange, 400, "{\"error\":\"Missing required booking parameters\"}", "application/json");
                    return;
                }

                List<BookingDAO.PassengerRequest> passengers = parsePassengers(body);
                if (passengers.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"At least one passenger is required\"}", "application/json");
                    return;
                }

                Map<String, Object> booking = BookingDAO.bookTicket(trainNumber, journeyDate, classType, quota, fromCode, toCode, passengers);
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
                    if (name != null && !name.isBlank()) {
                        list.add(new BookingDAO.PassengerRequest(
                                name,
                                age > 0 ? age : 25,
                                gender != null ? gender : "Male",
                                phone != null ? phone : "9876543210",
                                berth != null ? berth : "No Preference"
                        ));
                    }
                }
            }
            return list;
        }
    }

    // 6. GET /api/pnr?pnr=...
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

    // 7. POST /api/cancel
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

    // 8. GET /api/stats
    private static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

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
