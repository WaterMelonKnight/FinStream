package io.finstream.connector.binance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.finstream.domain.FundingRatePayload;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BinanceFundingRateNormalizerTest {
    private static final Instant RECEIVED_AT = Instant.parse("2026-09-04T12:00:00Z");
    private final BinanceFundingRateNormalizer normalizer = new BinanceFundingRateNormalizer(
            new ObjectMapper(), Clock.fixed(RECEIVED_AT, ZoneOffset.UTC));

    @Test
    void normalizesPremiumIndexResponse() throws Exception {
        MarketEvent event = normalizer.normalize("""
                {"symbol":"BTCUSDT","markPrice":"111234.50000000",
                 "indexPrice":"111200.25000000","lastFundingRate":"0.00001234",
                 "nextFundingTime":1788566400000,"time":1788523200123}
                """);

        assertThat(event.source()).isEqualTo("BINANCE");
        assertThat(event.signalType()).isEqualTo(MarketSignalType.FUNDING_RATE);
        assertThat(event.symbol()).isEqualTo("BTCUSDT");
        assertThat(event.eventTime()).isEqualTo(Instant.ofEpochMilli(1788523200123L));
        assertThat(event.receivedAt()).isEqualTo(RECEIVED_AT);
        FundingRatePayload payload = (FundingRatePayload) event.payload();
        assertThat(payload.fundingRate()).isEqualByComparingTo("0.00001234");
        assertThat(payload.markPrice()).isEqualByComparingTo("111234.50000000");
        assertThat(payload.indexPrice()).isEqualByComparingTo("111200.25000000");
        assertThat(payload.nextFundingTime()).isEqualTo(Instant.ofEpochMilli(1788566400000L));
    }

    @Test
    void rejectsMalformedResponse() {
        assertThatThrownBy(() -> normalizer.normalize("{\"symbol\":\"BTCUSDT\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing Binance funding rate fields");
        assertThatThrownBy(() -> normalizer.normalize("not-json"))
                .isInstanceOf(Exception.class);
    }
}
