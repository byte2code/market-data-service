# Market Data Service

A clean-architecture Spring Boot 3 (Java 21, Maven) application that fetches live spot market data from the OKX API and streams it to a Thymeleaf-based web interface via WebSockets.

---

## 1. Project Overview
This application acts as a secure, real-time proxy/aggregator between browser clients and the public OKX exchange API. 

### High-Level Architecture
- **Proxy/Aggregator Pattern**: Clients never communicate with OKX directly. The backend orchestrates all OKX interactions and broadcasts streams locally.
- **Shared Connection Multiplexing**: To prevent IP-based rate limiting, the backend maintains a single, shared asynchronous WebSocket connection to OKX. Browser clients register subscriptions locally, and the service aggregates demands, subscribing or unsubscribing from OKX on their behalf.
- **Security Interceptor**: The Spring Security context is integrated with WebSocket handler handshakes to validate credentials and authenticate streams.

---

## 2. Tech Stack
- **Backend Framework**: Spring Boot 3.5.16
  - *Spring Security*: Core authentication & session management.
  - *Spring WebFlux (WebClient)*: Asynchronous HTTP requests to OKX REST API.
  - *Spring WebSocket*: Managing client WebSocket proxy connections.
- **Frontend Engine**: Thymeleaf (HTML5 integration)
- **Styling & Interaction**: Vanilla CSS (glassmorphism/dark mode) and Vanilla JavaScript (native WebSocket API).

---

## 3. Prerequisites
- **Java**: JDK 21 (compatible with records, patterns, virtual threads)
- **Build Tool**: Maven (wrapper included)
- **Network**: Internet connection is required to resolve and fetch data from the OKX public endpoints.

---

## 4. How to Run Locally
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

## 5. Login Credentials
Access to the dashboard is secured. Use the following hardcoded account details to log in:
- **Username**: `admin`
- **Password**: `admin123`

---

## 6. Application Flow
1. **Login**: Navigate to `/login`, input credentials, and submit.
2. **Market Overview**: Upon successful validation, the user is redirected to `/market` where a table displays the top 20 spot trading pairs by 24h volume.
3. **Select Trading Pair**: Click on any trading pair symbol (e.g., `BTC-USDT`) in the table.
4. **Live Order Book**: You will be routed to `/orderbook?symbol=BTC-USDT` which initiates a WebSocket session to stream the top 15 bids and asks in real time.
5. **Logout**: Click the Logout button in the header to terminate the security session.

---

## 7. How to Test
A reviewer can verify the application functions using the following steps:

1. **Authentication Redirect**: Navigate directly to `/market` or `/orderbook` without logging in. Verify you are automatically redirected to `/login`.
2. **REST Overview & Volume Ranking**: Login with `admin` / `admin123`. Verify exactly 20 spot trading pairs are rendered, ordered from highest 24h volume to lowest.
3. **Auto-refresh**: Inspect the network tab or watch the prices on `/market`. Verify they update every 5 seconds without a page refresh.
4. **WebSocket Streaming**: Click a symbol. Verify that bids and asks populate on the Order Book view in real time.
5. **Single-Session Enforcement (Crucial Test)**:
   - Log in using Browser A (e.g., Chrome) and open the orderbook.
   - Open Browser B (e.g., Firefox or Chrome Incognito), log in as the same user (`admin`), and open the orderbook.
   - Verify that Browser A's connection is closed instantly with the WebSocket status `POLICY_VIOLATION` (close code `1008`).
6. **Resource Cleanup**: Close the browser tab. In the application logs, verify that the WebSocket connection cleanup runs, removing the session and unsubscribing from OKX for that symbol if no other active clients are listening.

---

## 8. API Endpoints
- **GET `/api/market/overview`**: REST API fetching the current top 20 spot pairs sorted by volume.
- **WebSocket `/ws/orderbook`**: Native WebSocket route handling client connections. Accepts commands:
  - `{"action": "SUBSCRIBE", "symbol": "BTC-USDT"}`
  - `{"action": "UNSUBSCRIBE", "symbol": "BTC-USDT"}`

---

## 9. Assumptions
- **Public Datastreams**: OKX market data API is accessed via public endpoints (no API key required).
- **Regional Domains**: `https://openapi.okx.com` and `wss://ws.okx.com:8443/ws/v5/public` are utilized to ensure the best global availability and routing.
- **Authentication**: A simple memory-backed UserDetailsService satisfies the requirement for a hardcoded user store.

---

## 10. Known Limitations
- **No Persistence Layer**: Database integration is omitted.
- **Static Credentials**: Single login account (`admin` / `admin123`) is configured in the security beans.
- **Server Restart Impact**: As the session registry and OKX socket instance are memory-backed, a server restart terminates current sessions.

---

## 11. Single Session Enforcement
The application implements a strict *one-active-session-per-user* rule:
1. When a browser initiates a WebSocket connection, the security `Principal` is extracted from the session handshake context.
2. The `SessionManager` registry records active user-to-session mappings in a thread-safe `ConcurrentHashMap`.
3. If the user already has a running session registered, the manager:
   - Queries `OrderBookServiceImpl` to remove all active symbol subscriptions registered for the old session to prevent resource leaks.
   - Triggers `existingSession.close(CloseStatus.POLICY_VIOLATION)` to notify the browser client.
4. The old session is replaced, and the new session is added.
5. The `SessionManager` uses `ConcurrentHashMap.remove(username, session)` to safely unregister sessions without introducing race conditions during parallel disconnects.

---

## 12. Project Structure
The project follows clean architecture separation of concerns:
- **`config`**: Defines the `WebClientConfig` (rest builder) and `WebSocketConfig` (websockets endpoints registry).
- **`controller`**:
  - `LoginController`: Serves Thymeleaf pages (`login`, `market`, `orderbook`).
  - `MarketApiController`: Serves JSON-based REST overview.
- **`service`**:
  - `MarketOverviewService`: Processes, filters, and sorts REST ticker data.
  - `OrderBookService`: Manages WebSocket client registries.
- **`client`**:
  - `OkxRestClient`: Fetches OKX HTTP tickers.
  - `OkxWebSocketClient`: Manages connection/subscription frames with the OKX WebSocket feed.
- **`websocket`**:
  - `ClientWebSocketHandler`: Integrates browser commands with the proxy service.
  - `SessionManager`: Enforces one WebSocket connection per user.
- **`dto` / `model`**: Immutable records/classes mapping representations.
- **`exception`**: Handles runtime API mapping errors.

---

## 13. Future Improvements
- **Exponential Backoff**: Implement backoff logic for OKX connection recovery.
- **Caching Layer**: Cache the REST market overview for 1–2 seconds to prevent excessive outgoing OKX HTTP requests if hundreds of clients fetch it simultaneously.
- **Unit and Integration Testing**: Add Mockito/Spring Security test coverages.
- **Containerization**: Add a `Dockerfile` for standardized deployments.
