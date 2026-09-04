package io.finstream.state;

import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketState;
import io.finstream.domain.TradePayload;
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
    private final Map<String, MarketState> latest = new ConcurrentHashMap<>();

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
            MarketState state = snapshot(event, ticks);
            latest.put(event.symbol(), state);
            return state;
        }
    }

    @Override
    public java.util.Optional<MarketState> get(String symbol) {
        return java.util.Optional.ofNullable(latest.get(symbol));
    }

    private MarketState snapshot(MarketEvent current, Deque<MarketEvent> ticks) {
        Instant now = current.eventTime();
        TradePayload currentTrade = tradePayload(current);
        BigDecimal high = currentTrade.price();
        BigDecimal low = currentTrade.price();
        double volume1m = 0;
        double volume5m = 0;
        double baselineVolume = 0;
        boolean baselineReady = false;
        BigDecimal price1m = null;
        BigDecimal price5m = null;
        BigDecimal price30m = tradePayload(ticks.peekFirst()).price();

        for (MarketEvent tick : ticks) {
            TradePayload trade = tradePayload(tick);
            Duration age = Duration.between(tick.eventTime(), now);
            if (age.compareTo(PRICE_WINDOW) >= 0) {
                baselineReady = true;
            }
            if (age.compareTo(PRICE_WINDOW) <= 0) {
                if (price5m == null) {
                    price5m = trade.price();
                }
                volume5m += trade.quantity().doubleValue();
                high = high.max(trade.price());
                low = low.min(trade.price());
            }
            if (age.compareTo(CURRENT_VOLUME_WINDOW) <= 0) {
                if (price1m == null) {
                    price1m = trade.price();
                }
                volume1m += trade.quantity().doubleValue();
            } else if (age.compareTo(PRICE_WINDOW) <= 0) {
                baselineVolume += trade.quantity().doubleValue();
            }
        }

        // Compare the current minute with the per-minute average of the preceding four minutes.
        double perMinuteBaseline = baselineVolume / 4.0;
        double volumeRatio = perMinuteBaseline > 0 ? volume1m / perMinuteBaseline : 0;
        return new MarketState(
                current.symbol(),
                now,
                currentTrade.price(),
                percentageChange(currentTrade.price(), price1m),
                percentageChange(currentTrade.price(), price5m),
                percentageChange(currentTrade.price(), price30m),
                volume1m,
                volume5m,
                high,
                low,
                volumeRatio,
                baselineReady);
    }

    private TradePayload tradePayload(MarketEvent event) {
        if (event.payload() instanceof TradePayload trade) {
            return trade;
        }
        throw new IllegalArgumentException("MarketStateStore requires a TradePayload");
    }

    private double percentageChange(BigDecimal current, BigDecimal old) {
        if (old == null || old.signum() == 0) {
            return 0;
        }
        return current.subtract(old).divide(old, 10, RoundingMode.HALF_UP).doubleValue() * 100;
    }
}
