package io.finstream.query;

import java.time.Instant;

public record MarketContextResponse(
        String symbol,
        Instant generatedAt,
        MarketContextSection<MarketStateResponse> market,
        MarketContextSection<FundingRateStateResponse> funding,
        MarketContextSection<OpenInterestStateResponse> openInterest) {

    public record MarketContextSection<T>(
            boolean available,
            String marketType,
            Long ageSeconds,
            Instant observedAt,
            T data) {
        public static <T> MarketContextSection<T> missing(String marketType) {
            return new MarketContextSection<>(false, marketType, null, null, null);
        }

        public static <T> MarketContextSection<T> available(
                String marketType, long ageSeconds, Instant observedAt, T data) {
            return new MarketContextSection<>(true, marketType, ageSeconds, observedAt, data);
        }
    }
}
