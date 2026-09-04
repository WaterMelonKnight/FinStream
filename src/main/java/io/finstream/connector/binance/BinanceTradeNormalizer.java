package io.finstream.connector.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class BinanceTradeNormalizer {
    private final ObjectMapper mapper;
    private final Clock clock;

    @Autowired
    public BinanceTradeNormalizer(ObjectMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    BinanceTradeNormalizer(ObjectMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public MarketEvent normalize(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode data = root.has("data") ? root.get("data") : root;
        if (!data.hasNonNull("s")
                || !data.hasNonNull("p")
                || !data.hasNonNull("q")
                || !data.hasNonNull("T")) {
            throw new IllegalArgumentException("Missing Binance trade fields");
        }
        return new MarketEvent(
                "BINANCE",
                data.get("s").asText(),
                MarketSignalType.TRADE,
                Instant.ofEpochMilli(data.get("T").asLong()),
                clock.instant(),
                new BigDecimal(data.get("p").asText()),
                new BigDecimal(data.get("q").asText()));
    }
}
