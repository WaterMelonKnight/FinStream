package io.finstream.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record FundingRatePayload(
        BigDecimal fundingRate, BigDecimal markPrice, BigDecimal indexPrice,
        Instant nextFundingTime) implements MarketSignalPayload {
    public FundingRatePayload {
        Objects.requireNonNull(fundingRate, "fundingRate");
        Objects.requireNonNull(markPrice, "markPrice");
        Objects.requireNonNull(indexPrice, "indexPrice");
        Objects.requireNonNull(nextFundingTime, "nextFundingTime");
    }
}
