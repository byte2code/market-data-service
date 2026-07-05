package com.assignment.market_data_service.service;

import com.assignment.market_data_service.client.OkxRestClient;
import com.assignment.market_data_service.dto.MarketPairDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class MarketOverviewServiceImpl implements MarketOverviewService {

    private static final Logger log = LoggerFactory.getLogger(MarketOverviewServiceImpl.class);
    private final OkxRestClient okxRestClient;

    public MarketOverviewServiceImpl(OkxRestClient okxRestClient) {
        this.okxRestClient = okxRestClient;
    }

    @Override
    public Flux<MarketPairDto> getTopTradingPairs() {
        return okxRestClient.fetchTopTradingPairs()
                .flatMapMany(response -> {
                    if (response == null || response.getData() == null || !"0".equals(response.getCode())) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(response.getData());
                })
                .filter(ticker -> {
                    if (ticker.getVolCcy24h() == null || ticker.getInstId() == null) {
                        return false;
                    }
                    try {
                        double vol = Double.parseDouble(ticker.getVolCcy24h());
                        return vol > 0;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .collectList()
                .flatMapMany(list -> {
                    list.sort((t1, t2) -> {
                        try {
                            double v1 = Double.parseDouble(t1.getVolCcy24h());
                            double v2 = Double.parseDouble(t2.getVolCcy24h());
                            return Double.compare(v2, v1);
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    });
                    int limit = Math.min(list.size(), 20);
                    return Flux.fromIterable(list.subList(0, limit));
                })
                .map(ticker -> {
                    MarketPairDto dto = new MarketPairDto();
                    dto.setSymbol(ticker.getInstId());
                    dto.setLastPrice(ticker.getLast());
                    dto.setVolume24h(ticker.getVolCcy24h());
                    dto.setChangePercent24h(calculateChangePercent(ticker.getLast(), ticker.getOpen24h()));
                    return dto;
                });
    }

    private String calculateChangePercent(String lastStr, String open24hStr) {
        if (lastStr == null || open24hStr == null) {
            return "0.00%";
        }
        try {
            double last = Double.parseDouble(lastStr);
            double open24h = Double.parseDouble(open24hStr);
            if (open24h == 0) {
                return "0.00%";
            }
            double changePercent = ((last - open24h) / open24h) * 100;
            return String.format("%.2f%%", changePercent);
        } catch (NumberFormatException e) {
            return "0.00%";
        }
    }
}
