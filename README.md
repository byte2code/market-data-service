# Market Data Service

A Spring Boot 3 (Java 21, Maven) application that aggregates live spot market data from the OKX API and streams it to a Thymeleaf-based web interface via WebSockets.

## Tech Stack
- Java 21
- Spring Boot 3 (Spring Web, Spring Security, Spring WebFlux/WebClient, Spring WebSocket)
- Thymeleaf (Frontend templates)
- Vanilla HTML, CSS, JavaScript

## Features
- **Secure Authentication**: Simple, form-based Spring Security login.
- **Market Overview**: Lists the top 20 spot trading pairs by 24h volume from OKX. Refreshes automatically every 5 seconds.
- **Live Order Book Depth**: Real-time bids and asks (top 15 levels) streamed via WebSockets from OKX through the backend proxy.
- **Single-Session Enforcement**: Enforces a strict one active WebSocket session per user policy.

## Getting Started

### Prerequisites
- JDK 21
- Maven (or using the included wrapper `./mvnw`)

### Configuration
The application parameters are configured in `src/main/resources/application.properties`:
- `okx.base-url`: The public OKX REST API base URL.
- `okx.websocket-url`: The public OKX WebSocket endpoint.

### Running Locally
1. Start the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```
2. Navigate to your browser:
   ```
   http://localhost:8080/market
   ```
3. Log in with the hardcoded credentials:
   - **Username**: `admin`
   - **Password**: `admin123`

---

## Technical Details & Architecture

### Single-Session Enforcement
To meet the connection management requirement, a dedicated **[SessionManager](file:///Users/bipinverma/Downloads/market-data-service/src/main/java/com/assignment/market_data_service/websocket/SessionManager.java)** registry tracks active user WebSocket connections.
1. When a client establishes a connection on `/ws/orderbook`, the user's security `Principal` username is extracted.
2. If the user already has a running session in the registry, the manager:
   - Deregisters all active symbol subscriptions for the old session.
   - Terminates the old connection with a `CloseStatus.POLICY_VIOLATION` code.
3. The new connection is then registered as active.
4. Uses atomic, thread-safe removals (`ConcurrentHashMap.remove(key, value)`) to prevent registration race conditions.

### Stream Proxy Design
Rather than initiating one connection to OKX for every browser tab (which causes severe IP rate limits), the backend acts as a proxy:
- Maintain a single, shared asynchronous connection via **[OkxWebSocketClient](file:///Users/bipinverma/Downloads/market-data-service/src/main/java/com/assignment/market_data_service/client/OkxWebSocketClient.java)** to the OKX server.
- The **[OrderBookServiceImpl](file:///Users/bipinverma/Downloads/market-data-service/src/main/java/com/assignment/market_data_service/service/OrderBookServiceImpl.java)** keeps track of what symbols are active. It registers subscriptions and unsubscribes from OKX only when symbol demand drops to zero (avoiding resource leaks).
- Incoming OKX updates are deserialized exactly once, trimmed to 15 depth levels, and broadcast to all browser sessions matching the symbol.

---

## Assumptions & Limitations
- **User Authentication**: Credentials are hardcoded in the Spring Security configuration (`InMemoryUserDetailsManager`) for simplicity.
- **Order Book Depth**: Slicing is performed on the standard 400-level `books` channel to obtain the required 15 levels of depth.
