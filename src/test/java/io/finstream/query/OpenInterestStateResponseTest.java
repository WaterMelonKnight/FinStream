package io.finstream.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.finstream.domain.OpenInterestState;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OpenInterestStateResponseTest {
    @Test
    void mapsWithoutLosingPrecision() {
        var value = new BigDecimal("12345.678901234567890123");
        var response = OpenInterestStateResponse.from(new OpenInterestState(
                "BINANCE", "BTCUSDT", value, Instant.EPOCH, Instant.EPOCH.plusSeconds(1)));
        assertThat(response.openInterest()).isSameAs(value);
        assertThat(response.source()).isEqualTo("BINANCE");
        assertThat(response.symbol()).isEqualTo("BTCUSDT");
    }
}
