package com.assignment.market_data_service.client;

import com.assignment.market_data_service.exception.MarketDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
public class OkxRestClient {

    private static final Logger log = LoggerFactory.getLogger(OkxRestClient.class);
    private final WebClient okxWebClient;

    public OkxRestClient(WebClient okxWebClient) {
        this.okxWebClient = okxWebClient;
    }

    public Mono<OkxTickerResponse> fetchTopTradingPairs() {
        return okxWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v5/market/tickers")
                        .queryParam("instType", "SPOT")
                        .build())
                .retrieve()
                .bodyToMono(OkxTickerResponse.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> {
                    log.error("Failed to fetch spot tickers from OKX REST API", ex);
                    return Mono.error(new MarketDataException("OKX API is currently unavailable", ex));
                });
    }

    public static class OkxTickerResponse {
        private String code;
        private String msg;
        private List<OkxTicker> data;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }
        public List<OkxTicker> getData() { return data; }
        public void setData(List<OkxTicker> data) { this.data = data; }
    }

    public static class OkxTicker {
        private String instId;
        private String last;
        private String open24h;
        private String volCcy24h;
        private String vol24h;

        public String getInstId() { return instId; }
        public void setInstId(String instId) { this.instId = instId; }
        public String getLast() { return last; }
        public void setLast(String last) { this.last = last; }
        public String getOpen24h() { return open24h; }
        public void setOpen24h(String open24h) { this.open24h = open24h; }
        public String getVolCcy24h() { return volCcy24h; }
        public void setVolCcy24h(String volCcy24h) { this.volCcy24h = volCcy24h; }
        public String getVol24h() { return vol24h; }
        public void setVol24h(String vol24h) { this.vol24h = vol24h; }
    }
}
