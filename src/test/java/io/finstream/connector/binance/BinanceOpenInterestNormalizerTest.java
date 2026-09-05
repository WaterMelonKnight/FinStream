package io.finstream.connector.binance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.OpenInterestPayload;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BinanceOpenInterestNormalizerTest {
    private static final Instant RECEIVED = Instant.parse("2026-09-05T12:00:00Z");
    private final BinanceOpenInterestNormalizer normalizer = new BinanceOpenInterestNormalizer(
            new ObjectMapper(), Clock.fixed(RECEIVED, ZoneOffset.UTC));

    @Test
    void normalizesCanonicalEventWithoutLosingDecimalPrecision() throws Exception {
        var event = normalizer.normalize("""
                {"symbol":"BTCUSDT","openInterest":"12345.67890123456789","time":1591261042378}
                """);

        assertThat(event.source()).isEqualTo("BINANCE");
        assertThat(event.signalType()).isEqualTo(MarketSignalType.OPEN_INTEREST);
        assertThat(event.symbol()).isEqualTo("BTCUSDT");
        assertThat(event.eventTime()).isEqualTo(Instant.ofEpochMilli(1591261042378L));
        assertThat(event.receivedAt()).isEqualTo(RECEIVED);
        assertThat(((OpenInterestPayload) event.payload()).openInterest())
                .isEqualByComparingTo("12345.67890123456789");
    }

    @Test
    void rejectsMalformedJsonAndMissingOrInvalidFields() {
        assertThatThrownBy(() -> normalizer.normalize("{"))
                .isInstanceOf(JsonProcessingException.class);
        assertThatThrownBy(() -> normalizer.normalize("{\"symbol\":\"BTCUSDT\",\"time\":1}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing Binance open interest fields");
        assertThatThrownBy(() -> normalizer.normalize("""
                {"symbol":"BTCUSDT","openInterest":"not-a-number","time":1}
                """))
                .isInstanceOf(NumberFormatException.class);
    }
}
