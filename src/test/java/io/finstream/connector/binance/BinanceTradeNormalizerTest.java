package io.finstream.connector.binance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.TradePayload;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BinanceTradeNormalizerTest {
    @Test
    void normalizesCombinedTrade() throws Exception {
        BinanceTradeNormalizer normalizer = new BinanceTradeNormalizer(
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2024-01-01T00:00:01Z"), ZoneOffset.UTC));

        var event = normalizer.normalize(
                "{\"stream\":\"btcusdt@aggTrade\",\"data\":{\"s\":\"BTCUSDT\","
                        + "\"p\":\"42000.50\",\"q\":\"0.25\",\"T\":1704067200000}}");

        assertThat(event.source()).isEqualTo("BINANCE");
        assertThat(event.symbol()).isEqualTo("BTCUSDT");
        assertThat(event.signalType()).isEqualTo(MarketSignalType.TRADE);
        assertThat(event.payload()).isInstanceOf(TradePayload.class);
        TradePayload payload = (TradePayload) event.payload();
        assertThat(payload.price().toPlainString()).isEqualTo("42000.50");
        assertThat(event.receivedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:01Z"));
    }
}
