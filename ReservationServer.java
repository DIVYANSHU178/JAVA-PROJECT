import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Responsibility: High-performance, lightweight embedded HTTP Web Server for RailStay.
public class ReservationServer {

    private final int port;
    private HttpServer server;
    private final TicketService ticketService;

    public ReservationServer(int port) {
        this.port = port;
        this.ticketService = TicketService.getInstance();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor()); // Java 21+ virtual threads for high concurrency

        // Contexts
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/trains", new TrainsApiHandler());
        server.createContext("/api/book", new BookingApiHandler());
        server.createContext("/api/pnr", new PnrApiHandler());
        server.createContext("/api/cancel", new CancelApiHandler());
        server.createContext("/api/chart", new ChartApiHandler());
        server.createContext("/api/stats", new StatsApiHandler());

        server.start();
        System.out.println("==================================================================");
        System.out.println("🚆 RailStay Express Train Reservation Web Server is running!");
        System.out.println("🌐 Access Web Site UI at: http://localhost:" + port);
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

    // GET /api/trains
    private class TrainsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
                return;
            }

            Collection<Train> trains = ticketService.getAllTrains();
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Train t : trains) {
                if (!first) sb.append(",");
                sb.append(JsonUtil.trainToJson(t));
                first = false;
            }
            sb.append("]");

            sendResponse(exchange, 200, sb.toString(), "application/json; charset=UTF-8");
        }
    }

    // POST /api/book
    private class BookingApiHandler implements HttpHandler {
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

            String requestBody = readRequestBody(exchange);
            try {
                String trainNumber = JsonUtil.extractString(requestBody, "trainNumber");
                String journeyDate = JsonUtil.extractString(requestBody, "journeyDate");
                int ticketClass = JsonUtil.extractInt(requestBody, "ticketClass", 2);

                // Parse passengers list
                List<TicketService.PassengerInput> passengers = parsePassengers(requestBody);
                if (passengers.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"At least one passenger is required\"}", "application/json");
                    return;
                }

                Ticket ticket = ticketService.bookTicketAdvanced(trainNumber, journeyDate, ticketClass, passengers);
                String responseJson = JsonUtil.ticketToJson(ticket);
                sendResponse(exchange, 201, responseJson, "application/json; charset=UTF-8");

            } catch (IllegalStateException e) {
                sendResponse(exchange, 409, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            }
        }

        private List<TicketService.PassengerInput> parsePassengers(String json) {
            List<TicketService.PassengerInput> list = new ArrayList<>();
            Pattern pArrayPattern = Pattern.compile("\"passengers\"\\s*:\\s*\\[([^\\]]+)\\]", Pattern.DOTALL);
            Matcher m = pArrayPattern.matcher(json);
            if (m.find()) {
                String arrayContent = m.group(1);
                // Split objects roughly by "}"
                String[] objects = arrayContent.split("\\},");
                for (String obj : objects) {
                    String name = JsonUtil.extractString(obj, "name");
                    String phone = JsonUtil.extractString(obj, "phone");
                    int age = JsonUtil.extractInt(obj, "age", 25);
                    String gender = JsonUtil.extractString(obj, "gender");
                    String berth = JsonUtil.extractString(obj, "berthPreference");
                    if (name != null && !name.isBlank()) {
                        list.add(new TicketService.PassengerInput(
                                name,
                                phone != null ? phone : "",
                                age > 0 ? age : 25,
                                gender != null ? gender : "Male",
                                berth != null ? berth : "Lower"
                        ));
                    }
                }
            }
            return list;
        }
    }

    // GET /api/pnr?pnr=...
    private class PnrApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String pnr = params.get("pnr");

            if (pnr == null || pnr.isBlank()) {
                sendResponse(exchange, 400, "{\"error\":\"PNR query parameter is required\"}", "application/json");
                return;
            }

            Ticket ticket = ticketService.findTicketByPnr(pnr);
            if (ticket == null) {
                // Also search by phone or name
                List<Ticket> matches = ticketService.searchTickets(pnr);
                if (!matches.isEmpty()) {
                    ticket = matches.get(0);
                }
            }

            if (ticket != null) {
                sendResponse(exchange, 200, JsonUtil.ticketToJson(ticket), "application/json; charset=UTF-8");
            } else {
                sendResponse(exchange, 404, "{\"error\":\"No booking found for PNR or Query: " + JsonUtil.escape(pnr) + "\"}", "application/json");
            }
        }
    }

    // POST /api/cancel
    private class CancelApiHandler implements HttpHandler {
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
                Ticket cancelledTicket = ticketService.cancelTicketByPnr(pnr);
                sendResponse(exchange, 200, JsonUtil.ticketToJson(cancelledTicket), "application/json; charset=UTF-8");
            } catch (NoSuchElementException e) {
                sendResponse(exchange, 404, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            } catch (IllegalStateException e) {
                sendResponse(exchange, 409, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"Cancellation failed: " + JsonUtil.escape(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    // GET /api/chart?trainNumber=...&journeyDate=...
    private class ChartApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String trainNumber = params.get("trainNumber");
            String journeyDate = params.get("journeyDate");

            List<Passenger> chart = ticketService.getReservationChart(trainNumber, journeyDate);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < chart.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(JsonUtil.passengerToJson(chart.get(i)));
            }
            sb.append("]");

            sendResponse(exchange, 200, sb.toString(), "application/json; charset=UTF-8");
        }
    }

    // GET /api/stats
    private class StatsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
                return;
            }

            Map<String, Object> stats = ticketService.getSystemStats();
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
