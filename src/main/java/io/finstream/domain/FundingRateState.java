package io.finstream.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record FundingRateState(
        String symbol,
        String source,
        BigDecimal fundingRate,
        BigDecimal markPrice,
        BigDecimal indexPrice,
        Instant nextFundingTime,
        Instant eventTime,
        Instant receivedAt) {}
