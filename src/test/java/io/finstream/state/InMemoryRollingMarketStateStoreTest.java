package io.finstream.state;

import static org.assertj.core.api.Assertions.assertThat;

import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryRollingMarketStateStoreTest {
    @Test
    void calculatesWindowsAndEvictsOldTicks() {
        InMemoryRollingMarketStateStore store = new InMemoryRollingMarketStateStore();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        store.update(event(now.minusSeconds(1900), "50", "9"));
        store.update(event(now.minusSeconds(240), "100", "4"));
        store.update(event(now.minusSeconds(30), "105", "2"));

        var state = store.update(event(now, "110", "3"));

        assertThat(state.return5m()).isEqualTo(10);
        assertThat(state.volume1m()).isEqualTo(5);
        assertThat(state.volume5m()).isEqualTo(9);
        assertThat(state.high5m()).isEqualByComparingTo("110");
        assertThat(state.low5m()).isEqualByComparingTo("100");
        assertThat(state.volumeRatio()).isEqualTo(5);
        assertThat(state.volumeBaselineReady()).isFalse();
    }

    @Test
    void baselineBecomesReadyAfterFiveMinutesOfHistory() {
        InMemoryRollingMarketStateStore store = new InMemoryRollingMarketStateStore();
        Instant now = Instant.parse("2024-01-01T00:05:00Z");
        store.update(event(now.minusSeconds(300), "100", "4"));
        store.update(event(now.minusSeconds(180), "100", "4"));

        var state = store.update(event(now, "100", "6"));

        assertThat(state.volumeBaselineReady()).isTrue();
        assertThat(state.volumeRatio()).isEqualTo(3);
    }

    @Test
    void getReturnsLatestSnapshotWithoutExposingWindow() {
        InMemoryRollingMarketStateStore store = new InMemoryRollingMarketStateStore();
        Instant time = Instant.parse("2026-08-20T00:00:00Z");
        var expected = store.update(event(time, "100", "2"));

        assertThat(store.get("BTCUSDT")).contains(expected);
        assertThat(store.get("ETHUSDT")).isEmpty();
    }

    private MarketEvent event(Instant time, String price, String quantity) {
        return new MarketEvent(
                "TEST",
                "BTCUSDT",
                MarketSignalType.TRADE,
                time,
                time,
                new BigDecimal(price),
                new BigDecimal(quantity));
    }
}
