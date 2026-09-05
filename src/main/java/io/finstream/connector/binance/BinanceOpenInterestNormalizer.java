package io.finstream.connector.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.OpenInterestPayload;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BinanceOpenInterestNormalizer {
    private final ObjectMapper mapper;
    private final Clock clock;

    @Autowired
    public BinanceOpenInterestNormalizer(ObjectMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    BinanceOpenInterestNormalizer(ObjectMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public MarketEvent normalize(String json) throws Exception {
        JsonNode data = mapper.readTree(json);
        if (data == null || !data.isObject() || !data.hasNonNull("symbol")
                || data.get("symbol").asText().isBlank()
                || !data.hasNonNull("openInterest") || !data.hasNonNull("time")
                || !data.get("time").isIntegralNumber()
                || !data.get("time").canConvertToLong()) {
            throw new IllegalArgumentException("Missing Binance open interest fields");
        }
        BigDecimal openInterest = new BigDecimal(data.get("openInterest").asText());
        return new MarketEvent(
                "BINANCE", data.get("symbol").asText(), MarketSignalType.OPEN_INTEREST,
                Instant.ofEpochMilli(data.get("time").asLong()), clock.instant(),
                new OpenInterestPayload(openInterest));
    }
}
