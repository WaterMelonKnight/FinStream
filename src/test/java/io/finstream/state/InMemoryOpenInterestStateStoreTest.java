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

    @Test
    void derivesChangesOnlyAfterEventTimeWarmupAndUsesLatestSampleAtOrBeforeTarget() {
        var store = new InMemoryOpenInterestStateStore();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        store.update(state("BTCUSDT", "100", start));
        store.update(state("BTCUSDT", "102", start.plusSeconds(4 * 60 + 59)));
        assertThat(store.get("BTCUSDT").orElseThrow().change5mPercent()).isNull();

        store.update(state("BTCUSDT", "110", start.plusSeconds(5 * 60 + 20)));
        var state = store.get("BTCUSDT").orElseThrow();
        assertThat(state.change5mPercent()).isEqualByComparingTo("10");
        assertThat(state.change15mPercent()).isNull();

        store.update(state("BTCUSDT", "120", start.plusSeconds(15 * 60 + 10)));
        state = store.get("BTCUSDT").orElseThrow();
        assertThat(state.change15mPercent()).isEqualByComparingTo("20");
        assertThat(state.referenceEventTime15m()).isEqualTo(start);
        assertThat(state.change30mPercent()).isNull();
    }

    @Test
    void handlesNegativeChangeInvalidReferenceOutOfOrderAndRetention() {
        var store = new InMemoryOpenInterestStateStore();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        store.update(state("BTCUSDT", "0", start));
        store.update(state("BTCUSDT", "90", start.plusSeconds(15 * 60)));
        assertThat(store.get("BTCUSDT").orElseThrow().change15mPercent()).isNull();

        store.update(state("BTCUSDT", "100", start.plusSeconds(20 * 60)));
        store.update(state("BTCUSDT", "90", start.plusSeconds(35 * 60)));
        assertThat(store.get("BTCUSDT").orElseThrow().change15mPercent())
                .isEqualByComparingTo("-10");
        var latest = store.update(state("BTCUSDT", "999", start.plusSeconds(34 * 60)));
        assertThat(latest.openInterest()).isEqualByComparingTo("90");

        // The zero-valued sample is older than the 35-minute retention after this update.
        store.update(state("BTCUSDT", "99", start.plusSeconds(36 * 60)));
        assertThat(store.get("BTCUSDT").orElseThrow().change30mPercent()).isNull();
    }

    private OpenInterestState state(String symbol, String value, Instant time) {
        return new OpenInterestState("BINANCE", symbol, new BigDecimal(value), time, time);
    }
}
