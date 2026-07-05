# Market Data Service

## Overview
This application is a clean-architecture Spring Boot platform designed to retrieve real-time financial market data from the public OKX exchange APIs and display it on a clean web interface. 

The backend acts as a proxy between the browser and OKX APIs:
* The frontend never communicates directly with OKX, preventing rate limits and ensuring complete architectural isolation.
* The project uses **Spring Boot, Thymeleaf, Spring Security, WebClient, and Spring WebSocket**.

---

## Features
- **Hardcoded Login Authentication**: Simple security gateway utilizing memory-backed user configuration.
- **Market Overview (Top 20 Spot Trading Pairs)**: Ranked and sorted in real-time by 24h trading volume.
- **Live Order Book**: Streams top 15 bids and asks with tick-by-tick real-time updates.
- **Backend WebSocket Proxy**: Multiplexes browser client subscriptions over a shared, single asynchronous connection to OKX.
- **Single WebSocket Session Per User**: Automatically closes duplicate/stale browser sessions upon new log ins.
- **Automatic Market Overview Refresh**: Fetches and re-ranks spot tickers every 5 seconds.
- **Thread-safe Session Management**: Prevents concurrency issues and memory leaks.
- **Clean Architecture**: Follows clean separation of concerns across clients, services, handlers, and controllers.

---

## Architecture Flow

```text
                    +-----------------------+
                    |      Browser UI       |
                    |  Thymeleaf + JS       |
                    +-----------+-----------+
                                |
              REST              |           WebSocket
      /api/market/overview      |        /ws/orderbook
                                |
                                v
                  +-----------------------------+
                  |      Spring Boot Backend     |
                  |-----------------------------|
                  | Security                    |
                  | Controllers                 |
                  | Services                    |
                  | Session Manager             |
                  | WebSocket Handler           |
                  | OKX REST Client             |
                  | OKX WebSocket Client        |
                  +-------------+---------------+
                                |
                     REST API & WebSocket
                                |
                                v
                    +----------------------+
                    |      OKX Exchange     |
                    | Public APIs           |
                    +----------------------+
```

---

## Tech Stack
- **Backend Framework**: Spring Boot 3.5.16
  - *Spring Security*: Core authentication & session enforcement.
  - *Spring WebFlux (WebClient)*: Asynchronous HTTP requests to OKX REST API.
  - *Spring WebSocket*: Managing client WebSocket proxy connections.
- **Frontend Engine**: Thymeleaf (HTML5 integration)
- **Styling & Interaction**: Vanilla CSS (glassmorphism/dark mode) and Vanilla JavaScript (native WebSocket API).

---

## Prerequisites
- **Java**: JDK 21 (compatible with records, patterns, virtual threads)
- **Build Tool**: Maven (wrapper included)
- **Network**: Internet connection is required to resolve and connect to public OKX endpoints.

---

## How to Run Locally
1. Clone the repository and navigate to the project root directory:
   ```bash
   cd market-data-service
   ```
2. Build the project:
   ```bash
   ./mvnw clean install
   ```
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Open your browser and navigate to:
   ```
   http://localhost:8080/login
   ```

---

## Login Credentials
Access to the dashboard is secured. Use the following hardcoded account details to log in:
- **Username**: `admin`
- **Password**: `admin123`

---

## Application Flow
1. **Login**: Navigate to `/login`, input credentials, and submit.
2. **Market Overview**: Upon successful validation, the user is redirected to `/market` where a table displays the top 20 spot trading pairs by 24h volume.
3. **Select Trading Pair**: Click on any trading pair symbol (e.g., `USDT-TRY`) in the table.
4. **Live Order Book**: You will be routed to `/orderbook?symbol=USDT-TRY` which initiates a WebSocket session to stream the top 15 bids and asks in real time.
5. **Logout**: Click the Logout button in the header to terminate the security session.

---

## How to Test
A reviewer can verify the application functions using the following steps:

1. **Authentication Redirect**: Navigate directly to `/market` or `/orderbook` without logging in. Verify you are automatically redirected to `/login`.
2. **REST Overview & Volume Ranking**: Login with `admin` / `admin123`. Verify exactly 20 spot trading pairs are rendered, ordered from highest 24h volume to lowest.
3. **Auto-refresh**: Inspect the network tab or watch the prices on `/market`. Verify they update every 5 seconds without a page refresh.
4. **WebSocket Streaming**: Click a symbol. Verify that bids and asks populate on the Order Book view in real time.
5. **Single-Session Enforcement**:
   - Log in using Browser A (e.g., Chrome) and open the orderbook.
   - Open Browser B (e.g., Firefox or Chrome Incognito), log in as the same user (`admin`), and open the orderbook.
   - Verify that Browser A's connection is closed instantly with the WebSocket status `POLICY_VIOLATION` (close code `1008`).
6. **Resource Cleanup**: Close the browser tab. In the application logs (or using `/api/market/debug`), verify that the WebSocket connection cleanup runs, removing the session and unsubscribing from OKX for that symbol if no other active clients are listening.

---

## Technical Notes & Resolved Challenges
To ensure the service runs robustly and complies with OKX limits, the following design adjustments were made:
- **Buffer Overflow Prevention (REST)**: Raised WebClient's maximum in-memory size to **10 MB** (via `maxInMemorySize`) to prevent `DataBufferLimitException` when loading large spot tickers payload from OKX.
- **WebSocket Buffer Expansion**: Increased the Tomcat client `WebSocketContainer` text/binary message buffer sizes to **10 MB** to prevent connection termination errors (`code=1009`) when receiving large OKX JSON order book snapshot payloads.
- **Keep-Alives**: Integrated a heartbeat ping task (every 20s) to keep backend connections alive and avoid OKX's 30-second idle timeouts.
- **Thread-safe Sessions**: Enforced single-session limits using atomic mapping updates in a `ConcurrentHashMap` with clean subscription unregistering on session replacement.
