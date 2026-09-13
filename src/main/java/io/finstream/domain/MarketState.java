package io.finstream.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketState(String symbol, Instant asOf, BigDecimal lastPrice, double return1m,
                          double return5m, double return30m, double volume1m, double volume5m,
                          BigDecimal high5m, BigDecimal low5m, double volumeRatio,
                          boolean volumeBaselineReady, Instant receivedAt) {
    public MarketState(String symbol, Instant asOf, BigDecimal lastPrice, double return1m,
            double return5m, double return30m, double volume1m, double volume5m,
            BigDecimal high5m, BigDecimal low5m, double volumeRatio,
            boolean volumeBaselineReady) {
        this(symbol, asOf, lastPrice, return1m, return5m, return30m, volume1m, volume5m,
                high5m, low5m, volumeRatio, volumeBaselineReady, asOf);
    }
}
