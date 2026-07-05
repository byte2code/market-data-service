document.addEventListener("DOMContentLoaded", () => {
    const marketBody = document.getElementById("market-pairs-body");
    const symbolElement = document.getElementById("symbol");

    if (marketBody) {
        const fetchMarketOverview = () => {
            fetch("/api/market/overview")
                .then(response => {
                    if (!response.ok) throw new Error("HTTP error " + response.status);
                    return response.json();
                })
                .then(data => {
                    marketBody.innerHTML = "";
                    if (data.length === 0) {
                        marketBody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--text-muted);">No market data available</td></tr>`;
                        return;
                    }
                    data.forEach(pair => {
                        const isPositive = !pair.changePercent24h.startsWith("-");
                        const changeClass = isPositive ? "positive" : "negative";
                        const row = document.createElement("tr");
                        row.innerHTML = `
                            <td><a href="/orderbook?symbol=${pair.symbol}" style="color: var(--primary-color); text-decoration: none; font-weight: 600;">${pair.symbol}</a></td>
                            <td>${pair.lastPrice}</td>
                            <td class="${changeClass}">${pair.changePercent24h}</td>
                            <td>${parseFloat(pair.volume24h).toLocaleString(undefined, {maximumFractionDigits: 2})}</td>
                        `;
                        marketBody.appendChild(row);
                    });
                })
                .catch(err => {
                    console.error("Failed to load market overview", err);
                });
        };

        fetchMarketOverview();
        const intervalId = setInterval(fetchMarketOverview, 5000);
        window.addEventListener("beforeunload", () => clearInterval(intervalId));
    }

    if (symbolElement) {
        const symbol = symbolElement.innerText.trim();
        const bidsBody = document.getElementById("bids-body");
        const asksBody = document.getElementById("asks-body");

        let socket;
        let reconnectTimeout;

        const connectWebSocket = () => {
            const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
            const wsUrl = `${protocol}//${window.location.host}/ws/orderbook`;
            console.log(`Connecting to WebSocket: ${wsUrl}`);
            socket = new WebSocket(wsUrl);

            socket.onopen = () => {
                console.log("WebSocket connected. Subscribing to " + symbol);
                socket.send(JSON.stringify({ action: "SUBSCRIBE", symbol: symbol }));
            };

            socket.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    if (data.symbol !== symbol) return;

                    bidsBody.innerHTML = "";
                    if (data.bids && data.bids.length > 0) {
                        data.bids.forEach(level => {
                            const row = document.createElement("tr");
                            row.innerHTML = `<td class="positive">${level.price}</td><td>${level.size}</td>`;
                            bidsBody.appendChild(row);
                        });
                    } else {
                        bidsBody.innerHTML = `<tr><td colspan="2" style="text-align: center; color: var(--text-muted);">No bids</td></tr>`;
                    }

                    asksBody.innerHTML = "";
                    if (data.asks && data.asks.length > 0) {
                        data.asks.forEach(level => {
                            const row = document.createElement("tr");
                            row.innerHTML = `<td class="negative">${level.price}</td><td>${level.size}</td>`;
                            asksBody.appendChild(row);
                        });
                    } else {
                        asksBody.innerHTML = `<tr><td colspan="2" style="text-align: center; color: var(--text-muted);">No asks</td></tr>`;
                    }
                } catch (e) {
                    console.error("Error parsing WebSocket message", e);
                }
            };

            socket.onclose = () => {
                console.log("WebSocket connection closed. Attempting reconnect...");
                reconnectTimeout = setTimeout(connectWebSocket, 2000);
            };

            socket.onerror = (error) => {
                console.error("WebSocket error", error);
            };
        };

        connectWebSocket();

        window.addEventListener("beforeunload", () => {
            if (reconnectTimeout) clearTimeout(reconnectTimeout);
            if (socket) {
                socket.onclose = null;
                socket.close();
            }
        });
    }
});
