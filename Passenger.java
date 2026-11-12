// Responsibility: To hold data for a single passenger.

public class Passenger {
    private int passengerNumber;
    private String name;
    private String phoneNumber;
    private int age;
    private int ticketClass; // 1: AC 1st Class, 2: AC 3-Tier, 3: Sleeper
    private String gender; // Male, Female, Other
    private String coach; // e.g. H1, B1, S1
    private int seatNumber; // e.g. 12
    private String berthType; // Lower, Upper, Middle, Side Lower, Side Upper, Window
    private String pnr; // Associated PNR reference

    // Default constructor for backward compatibility
    public Passenger(int passengerNumber, String name, String phoneNumber, int age, int ticketClass) {
        this(passengerNumber, name, phoneNumber, age, ticketClass, "Male", "B1", passengerNumber, "Lower", "");
    }

    // Full constructor
    public Passenger(int passengerNumber, String name, String phoneNumber, int age, int ticketClass, 
                     String gender, String coach, int seatNumber, String berthType, String pnr) {
        this.passengerNumber = passengerNumber;
        this.name = name != null ? name.trim() : "";
        this.phoneNumber = phoneNumber != null ? phoneNumber.trim() : "";
        this.age = age;
        this.ticketClass = ticketClass;
        this.gender = gender != null ? gender.trim() : "Male";
        this.coach = coach != null ? coach.trim() : "B1";
        this.seatNumber = seatNumber;
        this.berthType = berthType != null ? berthType.trim() : "Lower";
        this.pnr = pnr != null ? pnr.trim() : "";
    }

    // Getters
    public int getPassengerNumber() {
        return passengerNumber;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getAge() {
        return age;
    }

    public int getTicketClass() {
        return ticketClass;
    }

    public String getGender() {
        return gender;
    }

    public String getCoach() {
        return coach;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public String getBerthType() {
        return berthType;
    }

    public String getPnr() {
        return pnr;
    }

    // Setters
    public void setPassengerNumber(int passengerNumber) {
        this.passengerNumber = passengerNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setTicketClass(int ticketClass) {
        this.ticketClass = ticketClass;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setCoach(String coach) {
        this.coach = coach;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public void setBerthType(String berthType) {
        this.berthType = berthType;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public String getSeatAllocationDisplay() {
        return coach + "-" + seatNumber + " (" + berthType + ")";
    }

    @Override
    public String toString() {
        return passengerNumber + "\t" + name + "\t\t" + age + "\t" + gender + "\t" + phoneNumber + "\t" + getSeatAllocationDisplay();
    }
}
