package io.finstream.connector.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.finstream.domain.FundingRatePayload;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BinanceFundingRateNormalizer {
    private final ObjectMapper mapper;
    private final Clock clock;

    @Autowired
    public BinanceFundingRateNormalizer(ObjectMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    BinanceFundingRateNormalizer(ObjectMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public MarketEvent normalize(String json) throws Exception {
        JsonNode data = mapper.readTree(json);
        if (!data.hasNonNull("symbol") || !data.hasNonNull("lastFundingRate")
                || !data.hasNonNull("markPrice") || !data.hasNonNull("indexPrice")
                || !data.hasNonNull("nextFundingTime") || !data.hasNonNull("time")) {
            throw new IllegalArgumentException("Missing Binance funding rate fields");
        }
        return new MarketEvent(
                "BINANCE",
                data.get("symbol").asText(),
                MarketSignalType.FUNDING_RATE,
                Instant.ofEpochMilli(data.get("time").asLong()),
                clock.instant(),
                new FundingRatePayload(
                        decimal(data, "lastFundingRate"), decimal(data, "markPrice"),
                        decimal(data, "indexPrice"),
                        Instant.ofEpochMilli(data.get("nextFundingTime").asLong())));
    }

    private BigDecimal decimal(JsonNode data, String field) {
        return new BigDecimal(data.get(field).asText());
    }
}
