# Train Reservation System

A console-based Train Reservation System implemented in Java.

## Features

- **Ticket Booking:** Book railway tickets with automatic seat allocation (AC, Sleeper, and General classes).
- **Ticket Cancellation:** Cancel reserved tickets and release seat capacity dynamically.
- **Passenger Search:** Search passenger records and booking details by PNR or name.
- **Reservation Chart Display:** View the complete passenger reservation chart with assigned seats.
- **Unbooked Tickets Availability:** Check remaining available seats per coach and class.

## Project Structure

```
├── ConsoleUI.java              # User interface and input handling
├── Passenger.java              # Passenger data model
├── ReservationApplication.java  # Main application entry point and orchestrator
├── TicketService.java          # Core business logic and inventory management
└── README.md                   # Project documentation
```

## How to Compile & Run

### Prerequisites
- Java Development Kit (JDK 8 or higher)

### Compilation
Open a terminal in the project directory and run:
```bash
javac *.java
```

### Execution
Run the application using:
```bash
java ReservationApplication
```
