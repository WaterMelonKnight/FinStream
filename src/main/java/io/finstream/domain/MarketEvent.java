package io.finstream.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketEvent(String source, String symbol, MarketSignalType signalType,
                          Instant eventTime, Instant receivedAt, BigDecimal price,
                          BigDecimal quantity) {}
