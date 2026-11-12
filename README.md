# 🚆 RailStay Express — Train Ticket Reservation System

A high-performance, consistent, and user-friendly Train Ticket Reservation System implemented in pure **Java** with an interactive **Modern Web Application UI** (Single-Page Application).

![Java 21+](https://img.shields.io/badge/Java-21%2B-orange.svg)
![Web UI](https://img.shields.io/badge/Interface-Web%20%26%20Console-blue.svg)
![Zero Dependencies](https://img.shields.io/badge/Dependencies-Standard%20JDK%20Only-green.svg)
![Concurrency](https://img.shields.io/badge/Inventory-Thread--Safe%20Atomic-purple.svg)

---

## 🌟 Features & Highlights

- **Modern Responsive Web UI:**
  - Single-Page Application (SPA) accessible directly in any web browser at `http://localhost:8080`.
  - Realistic **Indian Railways Electronic Reservation Slip (ERS)** / Boarding Pass generator.
  - Interactive class selection cards with live seat counters and fare calculation.
  - Printable / Downloadable E-Tickets with QR and barcode styling.
- **Data Integrity & Consistency (Solved Booking Problems):**
  - **Atomic Seat Allocation:** Thread-safe seat allocation preventing race conditions, overbooking, and double-allocation.
  - **Dynamic Multi-Passenger Booking:** Allocates specific coach numbers (e.g. `H1`, `B1`, `S1`), seat numbers, and berth types (Lower, Middle, Upper, Side Lower, Window) per passenger.
  - **Leak-Free Cancellation & Refund:** Cancelling a ticket dynamically releases all booked seats back to the train's inventory immediately, calculating transparent refunds (85% refund, 15% cancellation fee).
  - **10-Character Unique PNRs:** Instant lookup of bookings by PNR or registered contact mobile number.
  - **Real-Time Passenger Manifest:** Live reservation chart filtered by train and journey date.
- **Zero Third-Party Dependencies:**
  - Built strictly on the standard Java Development Kit (`jdk.httpserver`).
  - No external build tools or heavy frameworks required.

---

## 🚀 Quick Start: How to Run

### Prerequisites
- **Java Development Kit (JDK 17 or higher)** installed.

### 1. Compile All Sources
Open PowerShell or Terminal in the project directory:
```bash
javac *.java
```

### 2. Launch the Web Application
Start the embedded HTTP server:
```bash
java ReservationServer
```
Then open your web browser and navigate to:
```
http://localhost:8080
```

### 3. (Optional) Run the Console Mode
If you prefer terminal-based interactive booking:
```bash
java ReservationApplication
```
*(Note: Running `ReservationApplication` automatically starts the Web UI on `http://localhost:8080` in the background as well!)*

---

## 🖥️ Web Interface Overview

| Tab | Feature | Description |
| :--- | :--- | :--- |
| **🎫 Book Ticket** | Reservation Engine | Select train, journey date, travel class (AC 1A, AC 3A, Sleeper), add multiple passengers, and book with instant seat allocation. |
| **🔍 PNR Status** | Digital E-Ticket | Enter PNR to view the verified boarding pass with coach/seat details and a **"Print E-Ticket"** option. |
| **❌ Cancel Ticket** | Instant Refund | Enter PNR to preview booking, view net refund calculation, and cancel with atomic seat inventory return. |
| **📋 Reservation Chart** | Live Passenger Manifest | View all confirmed passengers, coach allocations, and seat numbers for any train. |
| **📊 Live Inventory** | Train Schedule & Capacity | Real-time visual progress bars showing booked vs available seats across all 5 express routes. |

---

## 📡 REST API Endpoints

The embedded server exposes clean JSON endpoints for integration:

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Serves the web frontend single-page application |
| `GET` | `/api/trains` | Returns all available express trains and live seat inventory |
| `POST` | `/api/book` | Books ticket for 1 or more passengers; returns PNR, seat allocations, and fare |
| `GET` | `/api/pnr?pnr={id}` | Fetches ticket details by PNR or contact phone number |
| `POST` | `/api/cancel` | Cancels reservation, refunds fare, and releases seats back into inventory |
| `GET` | `/api/chart?trainNumber={id}` | Returns confirmed passenger list for reservation chart |
| `GET` | `/api/stats` | Dashboard metrics (total bookings, active passengers, total revenue, seats) |

---

## 📂 Project Architecture

```
C:\CLASS\JAVA PROJECT\
├── web/
│   └── index.html               # Modern Single-Page Application (HTML5/CSS3/Vanilla JS)
├── ConsoleUI.java              # Interactive console interface (backward compatible)
├── JsonUtil.java               # Pure Java JSON serializer & lightweight parser
├── Passenger.java              # Passenger data model with seat and berth attributes
├── ReservationApplication.java  # Main application launcher coordinating Web & Console UI
├── ReservationServer.java       # Embedded Java HTTP Web Server (jdk.httpserver)
├── Ticket.java                 # Complete reservation model with PNR & status
├── TicketService.java          # Thread-safe business logic, inventory, & seat allocation
├── Train.java                  # Train schedule, route, and capacity per coach class
├── WebUI.java                  # Static web content provider
└── README.md                   # System documentation
```
