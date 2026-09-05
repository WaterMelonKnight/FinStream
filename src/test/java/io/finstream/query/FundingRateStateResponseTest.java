package io.finstream.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.finstream.domain.FundingRateState;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FundingRateStateResponseTest {
    @ParameterizedTest
    @CsvSource({"0.001, 0.1", "0.0001, 0.01"})
    void mapsDecimalRateToPercentWithoutLosingSnapshotValues(String rate, String percent) {
        Instant nextFundingTime = Instant.parse("2026-09-05T08:00:00Z");
        Instant eventTime = Instant.parse("2026-09-05T07:00:00Z");
        Instant receivedAt = Instant.parse("2026-09-05T07:00:01Z");
        FundingRateState state = new FundingRateState(
                "BTCUSDT", "BINANCE", new BigDecimal(rate), new BigDecimal("111234.50"),
                new BigDecimal("111200.25"), nextFundingTime, eventTime, receivedAt);

        FundingRateStateResponse response = FundingRateStateResponse.from(state);

        assertThat(response.source()).isEqualTo("BINANCE");
        assertThat(response.symbol()).isEqualTo("BTCUSDT");
        assertThat(response.fundingRate()).isEqualByComparingTo(rate);
        assertThat(response.fundingRatePercent()).isEqualByComparingTo(percent);
        assertThat(response.markPrice()).isEqualByComparingTo("111234.50");
        assertThat(response.indexPrice()).isEqualByComparingTo("111200.25");
        assertThat(response.nextFundingTime()).isEqualTo(nextFundingTime);
        assertThat(response.eventTime()).isEqualTo(eventTime);
        assertThat(response.receivedAt()).isEqualTo(receivedAt);
    }
}
