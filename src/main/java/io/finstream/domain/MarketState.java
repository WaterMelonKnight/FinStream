package io.finstream.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketState(String symbol, Instant asOf, BigDecimal lastPrice, double return1m,
                          double return5m, double return30m, double volume1m, double volume5m,
                          BigDecimal high5m, BigDecimal low5m, double volumeRatio) {}
