package io.finstream.state;

import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketState;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRollingMarketStateStore implements MarketStateStore {
    private static final Duration MAX_WINDOW = Duration.ofMinutes(30);
    private static final Duration PRICE_WINDOW = Duration.ofMinutes(5);
    private static final Duration CURRENT_VOLUME_WINDOW = Duration.ofMinutes(1);

    private final Map<String, Deque<MarketEvent>> windows = new ConcurrentHashMap<>();

    @Override
    public MarketState update(MarketEvent event) {
        Deque<MarketEvent> ticks =
                windows.computeIfAbsent(event.symbol(), ignored -> new ArrayDeque<>());
        synchronized (ticks) {
            ticks.addLast(event);
            Instant cutoff = event.eventTime().minus(MAX_WINDOW);
            while (!ticks.isEmpty() && ticks.peekFirst().eventTime().isBefore(cutoff)) {
                ticks.removeFirst();
            }
            return snapshot(event, ticks);
        }
    }

    private MarketState snapshot(MarketEvent current, Deque<MarketEvent> ticks) {
        Instant now = current.eventTime();
        BigDecimal high = current.price();
        BigDecimal low = current.price();
        double volume1m = 0;
        double volume5m = 0;
        double baselineVolume = 0;
        boolean baselineReady = false;
        BigDecimal price1m = null;
        BigDecimal price5m = null;
        BigDecimal price30m = ticks.peekFirst().price();

        for (MarketEvent tick : ticks) {
            Duration age = Duration.between(tick.eventTime(), now);
            if (age.compareTo(PRICE_WINDOW) >= 0) {
                baselineReady = true;
            }
            if (age.compareTo(PRICE_WINDOW) <= 0) {
                if (price5m == null) {
                    price5m = tick.price();
                }
                volume5m += tick.quantity().doubleValue();
                high = high.max(tick.price());
                low = low.min(tick.price());
            }
            if (age.compareTo(CURRENT_VOLUME_WINDOW) <= 0) {
                if (price1m == null) {
                    price1m = tick.price();
                }
                volume1m += tick.quantity().doubleValue();
            } else if (age.compareTo(PRICE_WINDOW) <= 0) {
                baselineVolume += tick.quantity().doubleValue();
            }
        }

        // Compare the current minute with the per-minute average of the preceding four minutes.
        double perMinuteBaseline = baselineVolume / 4.0;
        double volumeRatio = perMinuteBaseline > 0 ? volume1m / perMinuteBaseline : 0;
        return new MarketState(
                current.symbol(),
                now,
                current.price(),
                percentageChange(current.price(), price1m),
                percentageChange(current.price(), price5m),
                percentageChange(current.price(), price30m),
                volume1m,
                volume5m,
                high,
                low,
                volumeRatio,
                baselineReady);
    }

    private double percentageChange(BigDecimal current, BigDecimal old) {
        if (old == null || old.signum() == 0) {
            return 0;
        }
        return current.subtract(old).divide(old, 10, RoundingMode.HALF_UP).doubleValue() * 100;
    }
}
