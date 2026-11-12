// Responsibility: To model a train with schedule and seat capacity per class.

public class Train {
    private String trainNumber;
    private String trainName;
    private String sourceStation;
    private String destStation;
    private String departureTime;
    private String arrivalTime;
    private String duration;

    // Capacity and pricing per class
    // Class 1: AC 1st Class (H1 coach)
    private int maxAc1Seats;
    private int bookedAc1Seats;
    private double ac1Fare;

    // Class 2: AC 3-Tier (B1, B2 coaches)
    private int maxAc3Seats;
    private int bookedAc3Seats;
    private double ac3Fare;

    // Class 3: Sleeper (S1, S2, S3 coaches)
    private int maxSleeperSeats;
    private int bookedSleeperSeats;
    private double sleeperFare;

    public Train(String trainNumber, String trainName, String sourceStation, String destStation,
                 String departureTime, String arrivalTime, String duration,
                 int maxAc1, double ac1Fare, int maxAc3, double ac3Fare, int maxSleeper, double sleeperFare) {
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.sourceStation = sourceStation;
        this.destStation = destStation;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.duration = duration;

        this.maxAc1Seats = maxAc1;
        this.bookedAc1Seats = 0;
        this.ac1Fare = ac1Fare;

        this.maxAc3Seats = maxAc3;
        this.bookedAc3Seats = 0;
        this.ac3Fare = ac3Fare;

        this.maxSleeperSeats = maxSleeper;
        this.bookedSleeperSeats = 0;
        this.sleeperFare = sleeperFare;
    }

    public synchronized boolean hasAvailability(int ticketClass, int seatsRequested) {
        if (seatsRequested <= 0) return false;
        return getAvailableSeats(ticketClass) >= seatsRequested;
    }

    public synchronized boolean allocateSeats(int ticketClass, int count) {
        if (!hasAvailability(ticketClass, count)) {
            return false;
        }
        switch (ticketClass) {
            case 1 -> bookedAc1Seats += count;
            case 2 -> bookedAc3Seats += count;
            case 3 -> bookedSleeperSeats += count;
            default -> { return false; }
        }
        return true;
    }

    public synchronized boolean releaseSeats(int ticketClass, int count) {
        if (count <= 0) return false;
        switch (ticketClass) {
            case 1 -> bookedAc1Seats = Math.max(0, bookedAc1Seats - count);
            case 2 -> bookedAc3Seats = Math.max(0, bookedAc3Seats - count);
            case 3 -> bookedSleeperSeats = Math.max(0, bookedSleeperSeats - count);
            default -> { return false; }
        }
        return true;
    }

    public synchronized int getAvailableSeats(int ticketClass) {
        return switch (ticketClass) {
            case 1 -> maxAc1Seats - bookedAc1Seats;
            case 2 -> maxAc3Seats - bookedAc3Seats;
            case 3 -> maxSleeperSeats - bookedSleeperSeats;
            default -> 0;
        };
    }

    public double getFare(int ticketClass) {
        return switch (ticketClass) {
            case 1 -> ac1Fare;
            case 2 -> ac3Fare;
            case 3 -> sleeperFare;
            default -> 0.0;
        };
    }

    public String getClassName(int ticketClass) {
        return switch (ticketClass) {
            case 1 -> "AC 1st Class (1A)";
            case 2 -> "AC 3-Tier (3A)";
            case 3 -> "Sleeper Class (SL)";
            default -> "Standard";
        };
    }

    // Getters
    public String getTrainNumber() { return trainNumber; }
    public String getTrainName() { return trainName; }
    public String getSourceStation() { return sourceStation; }
    public String getDestStation() { return destStation; }
    public String getDepartureTime() { return departureTime; }
    public String getArrivalTime() { return arrivalTime; }
    public String getDuration() { return duration; }

    public synchronized int getMaxAc1Seats() { return maxAc1Seats; }
    public synchronized int getBookedAc1Seats() { return bookedAc1Seats; }
    public double getAc1Fare() { return ac1Fare; }

    public synchronized int getMaxAc3Seats() { return maxAc3Seats; }
    public synchronized int getBookedAc3Seats() { return bookedAc3Seats; }
    public double getAc3Fare() { return ac3Fare; }

    public synchronized int getMaxSleeperSeats() { return maxSleeperSeats; }
    public synchronized int getBookedSleeperSeats() { return bookedSleeperSeats; }
    public double getSleeperFare() { return sleeperFare; }

    public synchronized void setBookedAc1Seats(int count) { this.bookedAc1Seats = count; }
    public synchronized void setBookedAc3Seats(int count) { this.bookedAc3Seats = count; }
    public synchronized void setBookedSleeperSeats(int count) { this.bookedSleeperSeats = count; }
}
