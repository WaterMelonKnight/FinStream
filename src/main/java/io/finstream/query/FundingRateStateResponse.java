package io.finstream.query;

import io.finstream.domain.FundingRateState;
import java.math.BigDecimal;
import java.time.Instant;

public record FundingRateStateResponse(
        String source,
        String symbol,
        BigDecimal fundingRate,
        BigDecimal fundingRatePercent,
        BigDecimal markPrice,
        BigDecimal indexPrice,
        Instant nextFundingTime,
        Instant eventTime,
        Instant receivedAt) {
    public static FundingRateStateResponse from(FundingRateState state) {
        return new FundingRateStateResponse(
                state.source(),
                state.symbol(),
                state.fundingRate(),
                state.fundingRate().movePointRight(2),
                state.markPrice(),
                state.indexPrice(),
                state.nextFundingTime(),
                state.eventTime(),
                state.receivedAt());
    }
}
