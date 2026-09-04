package io.finstream.state;

import static org.assertj.core.api.Assertions.assertThat;

import io.finstream.domain.FundingRateState;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryFundingRateStateStoreTest {
    private final InMemoryFundingRateStateStore store = new InMemoryFundingRateStateStore();

    @Test
    void updatesLatestSnapshotIndependentlyPerSymbol() {
        FundingRateState firstBtc = state("BTCUSDT", "0.0001", Instant.EPOCH);
        FundingRateState eth = state("ETHUSDT", "-0.0002", Instant.EPOCH.plusSeconds(1));
        FundingRateState latestBtc = state("BTCUSDT", "0.0003", Instant.EPOCH.plusSeconds(2));

        assertThat(store.update(firstBtc)).isSameAs(firstBtc);
        store.update(eth);
        store.update(latestBtc);

        assertThat(store.get("BTCUSDT")).contains(latestBtc);
        assertThat(store.get("ETHUSDT")).contains(eth);
        assertThat(store.get("SOLUSDT")).isEmpty();
    }

    private FundingRateState state(String symbol, String rate, Instant time) {
        return new FundingRateState(symbol, "BINANCE", new BigDecimal(rate),
                BigDecimal.TEN, BigDecimal.ONE, time.plusSeconds(100), time, time);
    }
}
