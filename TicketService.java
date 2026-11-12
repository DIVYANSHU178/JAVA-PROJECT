import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// Responsibility: To handle core business logic, thread-safe inventory,
// seat allocation, bookings, cancellations, and reservation charts.
public class TicketService {

    private final Map<String, Train> trains = new LinkedHashMap<>();
    private final Map<String, Ticket> ticketsByPnr = new ConcurrentHashMap<>();
    private final List<Passenger> allPassengers = Collections.synchronizedList(new ArrayList<>());

    private final AtomicInteger nextPassengerId = new AtomicInteger(1);
    private final AtomicInteger pnrSequence = new AtomicInteger(1000 + new Random().nextInt(8000));

    // Default primary train for legacy console UI compatibility
    private Train primaryTrain;

    // Singleton instance for web server and application
    private static TicketService instance;

    public static synchronized TicketService getInstance() {
        if (instance == null) {
            instance = new TicketService();
        }
        return instance;
    }

    // Default constructor initializing standard trains
    public TicketService() {
        this(50, 100, 150);
    }

    // Legacy constructor supporting custom initial capacities
    public TicketService(int initialMaxAc, int initialMaxFirst, int initialMaxSleeper) {
        initTrains(initialMaxAc, initialMaxFirst, initialMaxSleeper);
        seedSampleData();
        synchronized (TicketService.class) {
            if (instance == null) {
                instance = this;
            }
        }
    }

    private void initTrains(int acCap, int firstCap, int sleeperCap) {
        trains.clear();

        // 1. Mumbai Rajdhani Express
        Train t1 = new Train("12951", "Mumbai Rajdhani Express", "New Delhi (NDLS)", "Mumbai Central (MMCT)",
                "16:55", "08:35", "15h 40m", acCap, 3200.0, firstCap, 1850.0, sleeperCap, 750.0);
        trains.put(t1.getTrainNumber(), t1);
        primaryTrain = t1;

        // 2. Vande Bharat Express
        Train t2 = new Train("22436", "Vande Bharat Express", "New Delhi (NDLS)", "Varanasi Jn (BSB)",
                "06:00", "14:00", "08h 00m", 40, 2400.0, 80, 1500.0, 0, 0.0);
        trains.put(t2.getTrainNumber(), t2);

        // 3. Bhopal Shatabdi Express
        Train t3 = new Train("12002", "Bhopal Shatabdi Express", "New Delhi (NDLS)", "Rani Kamlapati (RKMP)",
                "06:00", "14:40", "08h 40m", 45, 2150.0, 90, 1320.0, 120, 580.0);
        trains.put(t3.getTrainNumber(), t3);

        // 4. Howrah Rajdhani Express
        Train t4 = new Train("12302", "Howrah Rajdhani Express", "New Delhi (NDLS)", "Howrah Jn (HWH)",
                "16:50", "09:55", "17h 05m", 50, 3100.0, 100, 1800.0, 140, 720.0);
        trains.put(t4.getTrainNumber(), t4);

        // 5. Karnataka Express
        Train t5 = new Train("12628", "Karnataka Express", "New Delhi (NDLS)", "KSR Bengaluru (SBC)",
                "20:20", "12:00", "39h 40m", 35, 3450.0, 90, 1950.0, 180, 820.0);
        trains.put(t5.getTrainNumber(), t5);
    }

    private void seedSampleData() {
        // Seed initial confirmed demo bookings for immediate visibility in UI
        try {
            List<PassengerInput> p1 = List.of(
                    new PassengerInput("Aarav Sharma", "9876543210", 32, "Male", "Lower"),
                    new PassengerInput("Priya Sharma", "9876543210", 29, "Female", "Middle")
            );
            bookTicketAdvanced("12951", "2026-11-20", 2, p1);

            List<PassengerInput> p2 = List.of(
                    new PassengerInput("Col. Rajesh Verma", "9811223344", 58, "Male", "Lower")
            );
            bookTicketAdvanced("22436", "2026-11-22", 1, p2);

            List<PassengerInput> p3 = List.of(
                    new PassengerInput("Sneha Patel", "9422334455", 24, "Female", "Window"),
                    new PassengerInput("Karan Patel", "9422334455", 27, "Male", "Side Lower")
            );
            bookTicketAdvanced("12002", "2026-11-25", 2, p3);
        } catch (Exception ignored) {
        }
    }

    // Helper data carrier for multiple passenger inputs
    public record PassengerInput(String name, String phone, int age, String gender, String berthPreference) {}

    // Advanced booking with automatic coach/seat allocation and thread-safe inventory
    public synchronized Ticket bookTicketAdvanced(String trainNumber, String journeyDate, int ticketClass, List<PassengerInput> passengerInputs) {
        if (passengerInputs == null || passengerInputs.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required for booking.");
        }

        Train train = trains.get(trainNumber);
        if (train == null) {
            train = primaryTrain;
        }

        int count = passengerInputs.size();
        if (!train.hasAvailability(ticketClass, count)) {
            throw new IllegalStateException("Requested " + count + " seats are not available in " + train.getClassName(ticketClass));
        }

        // Allocate inventory atomically
        if (!train.allocateSeats(ticketClass, count)) {
            throw new IllegalStateException("Failed to allocate inventory seats.");
        }

        // Generate unique PNR
        String pnr = "PNR-" + (System.currentTimeMillis() % 1000000) + pnrSequence.incrementAndGet();

        // Assign coaches & seats
        List<Passenger> bookedPassengers = new ArrayList<>();
        String coachPrefix = switch (ticketClass) {
            case 1 -> "H1";
            case 2 -> (train.getBookedAc3Seats() % 2 == 0) ? "B1" : "B2";
            case 3 -> (train.getBookedSleeperSeats() % 2 == 0) ? "S1" : "S2";
            default -> "GEN";
        };

        String[] berths = switch (ticketClass) {
            case 1 -> new String[]{"Lower Berth", "Upper Berth", "Cabin Lower", "Cabin Upper"};
            case 2 -> new String[]{"Lower Berth", "Middle Berth", "Upper Berth", "Side Lower", "Side Upper"};
            case 3 -> new String[]{"Lower Berth", "Middle Berth", "Upper Berth", "Side Lower", "Side Upper"};
            default -> new String[]{"Standard Seat"};
        };

        int baseSeat = (ticketClass == 1 ? train.getBookedAc1Seats() :
                        ticketClass == 2 ? train.getBookedAc3Seats() :
                        train.getBookedSleeperSeats()) - count + 1;

        for (int i = 0; i < count; i++) {
            PassengerInput in = passengerInputs.get(i);
            int seatNum = baseSeat + i;
            String berth = (in.berthPreference() != null && !in.berthPreference().isBlank())
                    ? in.berthPreference() + " (" + berths[i % berths.length] + ")"
                    : berths[i % berths.length];

            Passenger p = new Passenger(
                    nextPassengerId.getAndIncrement(),
                    in.name(),
                    in.phone(),
                    in.age(),
                    ticketClass,
                    in.gender(),
                    coachPrefix,
                    seatNum,
                    berth,
                    pnr
            );
            bookedPassengers.add(p);
            allPassengers.add(p);
        }

        double totalFare = train.getFare(ticketClass) * count;
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));

        Ticket ticket = new Ticket(
                pnr,
                train.getTrainNumber(),
                train.getTrainName(),
                train.getSourceStation(),
                train.getDestStation(),
                journeyDate != null && !journeyDate.isBlank() ? journeyDate : "2026-11-20",
                nowStr,
                ticketClass,
                train.getClassName(ticketClass),
                bookedPassengers,
                totalFare
        );

        ticketsByPnr.put(pnr, ticket);
        return ticket;
    }

    // Advanced cancellation with transparent refund and atomic inventory release
    public synchronized Ticket cancelTicketByPnr(String pnr) {
        if (pnr == null || pnr.isBlank()) {
            throw new IllegalArgumentException("PNR cannot be empty.");
        }

        Ticket ticket = ticketsByPnr.get(pnr.trim().toUpperCase());
        if (ticket == null) {
            // Also search without case sensitivity
            ticket = ticketsByPnr.values().stream()
                    .filter(t -> t.getPnr().equalsIgnoreCase(pnr.trim()))
                    .findFirst()
                    .orElse(null);
        }

        if (ticket == null) {
            throw new NoSuchElementException("No active booking found for PNR: " + pnr);
        }

        if (ticket.isCancelled()) {
            throw new IllegalStateException("Ticket with PNR " + pnr + " is already CANCELLED.");
        }

        // Release seats back to the train
        Train train = trains.get(ticket.getTrainNumber());
        if (train == null) {
            train = primaryTrain;
        }

        int seatCount = ticket.getPassengerCount();
        train.releaseSeats(ticket.getTicketClass(), seatCount);

        // Remove from active passenger reservation lists
        for (Passenger p : ticket.getPassengers()) {
            allPassengers.removeIf(existing -> existing.getPassengerNumber() == p.getPassengerNumber());
        }

        // Calculate transparent refund: 85% refunded, 15% cancellation fee
        double cancellationFee = ticket.getTotalFare() * 0.15;
        double refund = Math.max(0.0, ticket.getTotalFare() - cancellationFee);

        String cancelTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
        ticket.cancel(refund, cancelTime);

        return ticket;
    }

    // Find ticket by PNR
    public Ticket findTicketByPnr(String pnr) {
        if (pnr == null) return null;
        return ticketsByPnr.values().stream()
                .filter(t -> t.getPnr().equalsIgnoreCase(pnr.trim()))
                .findFirst()
                .orElse(null);
    }

    // Search tickets by PNR, passenger name, or phone number
    public List<Ticket> searchTickets(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(ticketsByPnr.values());
        }
        String q = query.trim().toLowerCase();
        return ticketsByPnr.values().stream()
                .filter(t -> t.getPnr().toLowerCase().contains(q)
                        || t.getPrimaryContactPhone().contains(q)
                        || t.getPrimaryPassengerName().toLowerCase().contains(q)
                        || t.getTrainName().toLowerCase().contains(q)
                        || t.getTrainNumber().contains(q))
                .sorted((a, b) -> b.getBookingTime().compareTo(a.getBookingTime()))
                .collect(Collectors.toList());
    }

    // Get reservation chart for a train and journey date
    public List<Passenger> getReservationChart(String trainNumber, String journeyDate) {
        return ticketsByPnr.values().stream()
                .filter(t -> !t.isCancelled())
                .filter(t -> trainNumber == null || trainNumber.isBlank() || t.getTrainNumber().equalsIgnoreCase(trainNumber))
                .filter(t -> journeyDate == null || journeyDate.isBlank() || t.getJourneyDate().equals(journeyDate))
                .flatMap(t -> t.getPassengers().stream())
                .sorted(Comparator.comparingInt(Passenger::getPassengerNumber))
                .collect(Collectors.toList());
    }

    // Dashboard metrics
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long totalBookings = ticketsByPnr.size();
        long confirmedBookings = ticketsByPnr.values().stream().filter(t -> !t.isCancelled()).count();
        long cancelledBookings = ticketsByPnr.values().stream().filter(Ticket::isCancelled).count();
        long totalPassengers = ticketsByPnr.values().stream().filter(t -> !t.isCancelled()).mapToInt(Ticket::getPassengerCount).sum();
        double totalRevenue = ticketsByPnr.values().stream().filter(t -> !t.isCancelled()).mapToDouble(Ticket::getTotalFare).sum();

        int totalSeats = 0;
        int totalAvailableSeats = 0;
        for (Train t : trains.values()) {
            totalSeats += t.getMaxAc1Seats() + t.getMaxAc3Seats() + t.getMaxSleeperSeats();
            totalAvailableSeats += t.getAvailableSeats(1) + t.getAvailableSeats(2) + t.getAvailableSeats(3);
        }

        stats.put("totalBookings", totalBookings);
        stats.put("confirmedBookings", confirmedBookings);
        stats.put("cancelledBookings", cancelledBookings);
        stats.put("totalPassengers", totalPassengers);
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalSeats", totalSeats);
        stats.put("totalAvailableSeats", totalAvailableSeats);
        stats.put("totalTrains", trains.size());
        return stats;
    }

    public Collection<Train> getAllTrains() {
        return trains.values();
    }

    public Train getTrain(String trainNumber) {
        return trains.get(trainNumber);
    }

    // =========================================================================
    // LEGACY METHODS PRESERVED FOR BACKWARD COMPATIBILITY WITH ConsoleUI
    // =========================================================================

    public boolean checkAvailability(int ticketClass, int numberOfTickets) {
        return primaryTrain.hasAvailability(ticketClass, numberOfTickets);
    }

    public Optional<Passenger> bookTicket(String name, String phone, int age, int ticketClass, int numberOfTickets) {
        if (!checkAvailability(ticketClass, numberOfTickets)) {
            return Optional.empty();
        }

        List<PassengerInput> inputs = new ArrayList<>();
        for (int i = 0; i < numberOfTickets; i++) {
            inputs.add(new PassengerInput(
                    i == 0 ? name : name + " (Guest " + (i + 1) + ")",
                    phone,
                    age,
                    "Male",
                    "Lower"
            ));
        }

        Ticket ticket = bookTicketAdvanced(primaryTrain.getTrainNumber(), "2026-11-20", ticketClass, inputs);
        if (!ticket.getPassengers().isEmpty()) {
            return Optional.of(ticket.getPassengers().get(0));
        }
        return Optional.empty();
    }

    public double calculatePrice(int ticketClass, int numberOfTickets) {
        return primaryTrain.getFare(ticketClass) * numberOfTickets;
    }

    public boolean cancelTicket(int passengerNumber) {
        Optional<Passenger> passengerOpt = findPassengerByNumber(passengerNumber);
        if (passengerOpt.isPresent()) {
            Passenger p = passengerOpt.get();
            if (p.getPnr() != null && !p.getPnr().isBlank()) {
                try {
                    cancelTicketByPnr(p.getPnr());
                    return true;
                } catch (Exception ignored) {
                }
            }
            allPassengers.remove(p);
            primaryTrain.releaseSeats(p.getTicketClass(), 1);
            return true;
        }
        return false;
    }

    public double calculateRefund(int ticketClass) {
        return primaryTrain.getFare(ticketClass) * 0.85;
    }

    public Optional<Passenger> findPassengerByNumber(int passengerNumber) {
        return allPassengers.stream()
                .filter(p -> p.getPassengerNumber() == passengerNumber)
                .findFirst();
    }

    public List<Passenger> getPassengersByClass(int ticketClass) {
        return allPassengers.stream()
                .filter(p -> p.getTicketClass() == ticketClass)
                .collect(Collectors.toList());
    }

    public List<Passenger> getAllPassengers() {
        return new ArrayList<>(allPassengers);
    }

    public int getRemainingAcTickets() {
        return primaryTrain.getAvailableSeats(1);
    }

    public int getRemainingFirstClassTickets() {
        return primaryTrain.getAvailableSeats(2);
    }

    public int getRemainingSleeperTickets() {
        return primaryTrain.getAvailableSeats(3);
    }
}
