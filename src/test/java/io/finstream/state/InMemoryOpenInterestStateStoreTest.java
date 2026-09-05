package io.finstream.state;

import static org.assertj.core.api.Assertions.assertThat;

import io.finstream.domain.OpenInterestState;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryOpenInterestStateStoreTest {
    @Test
    void keepsLatestSnapshotForEachSymbolAndMissingIsEmpty() {
        var store = new InMemoryOpenInterestStateStore();
        var oldBtc = state("BTCUSDT", "1", Instant.EPOCH);
        var newBtc = state("BTCUSDT", "2", Instant.EPOCH.plusSeconds(1));
        var eth = state("ETHUSDT", "3", Instant.EPOCH);

        assertThat(store.get("BTCUSDT")).isEmpty();
        assertThat(store.update(oldBtc)).isEqualTo(oldBtc);
        store.update(eth);
        store.update(newBtc);
        assertThat(store.get("BTCUSDT")).contains(newBtc);
        assertThat(store.get("ETHUSDT")).contains(eth);
    }

    private OpenInterestState state(String symbol, String value, Instant time) {
        return new OpenInterestState("BINANCE", symbol, new BigDecimal(value), time, time);
    }
}
