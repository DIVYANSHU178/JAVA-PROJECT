import java.util.ArrayList;
import java.util.List;

// Responsibility: To model a complete train reservation ticket with a unique PNR.
public class Ticket {
    private String pnr;
    private String trainNumber;
    private String trainName;
    private String sourceStation;
    private String destStation;
    private String journeyDate;
    private String bookingTime;
    private int ticketClass;
    private String ticketClassName;
    private List<Passenger> passengers;
    private double totalFare;
    private String status; // "CONFIRMED", "CANCELLED"
    private double refundAmount;
    private String cancellationTime;

    public Ticket(String pnr, String trainNumber, String trainName, String sourceStation, String destStation,
                  String journeyDate, String bookingTime, int ticketClass, String ticketClassName,
                  List<Passenger> passengers, double totalFare) {
        this.pnr = pnr;
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.sourceStation = sourceStation;
        this.destStation = destStation;
        this.journeyDate = journeyDate;
        this.bookingTime = bookingTime;
        this.ticketClass = ticketClass;
        this.ticketClassName = ticketClassName;
        this.passengers = passengers != null ? new ArrayList<>(passengers) : new ArrayList<>();
        this.totalFare = totalFare;
        this.status = "CONFIRMED";
        this.refundAmount = 0.0;
        this.cancellationTime = null;
    }

    public synchronized boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }

    public synchronized void cancel(double refund, String cancelTime) {
        this.status = "CANCELLED";
        this.refundAmount = refund;
        this.cancellationTime = cancelTime;
    }

    // Getters
    public String getPnr() { return pnr; }
    public String getTrainNumber() { return trainNumber; }
    public String getTrainName() { return trainName; }
    public String getSourceStation() { return sourceStation; }
    public String getDestStation() { return destStation; }
    public String getJourneyDate() { return journeyDate; }
    public String getBookingTime() { return bookingTime; }
    public int getTicketClass() { return ticketClass; }
    public String getTicketClassName() { return ticketClassName; }
    public List<Passenger> getPassengers() { return new ArrayList<>(passengers); }
    public int getPassengerCount() { return passengers.size(); }
    public double getTotalFare() { return totalFare; }
    public synchronized String getStatus() { return status; }
    public synchronized double getRefundAmount() { return refundAmount; }
    public synchronized String getCancellationTime() { return cancellationTime; }

    public String getPrimaryContactPhone() {
        if (!passengers.isEmpty()) {
            return passengers.get(0).getPhoneNumber();
        }
        return "";
    }

    public String getPrimaryPassengerName() {
        if (!passengers.isEmpty()) {
            return passengers.get(0).getName();
        }
        return "";
    }
}
