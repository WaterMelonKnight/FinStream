package io.finstream.state;

import io.finstream.domain.OpenInterestState;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InMemoryOpenInterestStateStore implements OpenInterestStateStore {
    private static final Logger log = LoggerFactory.getLogger(InMemoryOpenInterestStateStore.class);
    static final Duration HISTORY_RETENTION = Duration.ofMinutes(35);
    private static final Duration FIVE_MINUTES = Duration.ofMinutes(5);
    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    private static final Duration THIRTY_MINUTES = Duration.ofMinutes(30);
    private final Map<String, SymbolHistory> histories = new ConcurrentHashMap<>();

    @Override
    public OpenInterestState update(OpenInterestState state) {
        SymbolHistory history = histories.computeIfAbsent(state.symbol(), ignored -> new SymbolHistory());
        synchronized (history) {
            OpenInterestState latest = history.latest;
            if (latest != null && state.eventTime().isBefore(latest.eventTime())) {
                log.debug("Ignoring out-of-order Open Interest state for {} at {}; latest is {}",
                        state.symbol(), state.eventTime(), latest.eventTime());
                return latest;
            }
            // Equal timestamps deterministically replace the prior observation.
            if (latest != null && state.eventTime().equals(latest.eventTime())) {
                history.samples.removeLast();
            }
            history.samples.addLast(state);
            Instant cutoff = state.eventTime().minus(HISTORY_RETENTION);
            while (!history.samples.isEmpty()
                    && history.samples.peekFirst().eventTime().isBefore(cutoff)) {
                history.samples.removeFirst();
            }
            Reference five = reference(history.samples, state.eventTime(), FIVE_MINUTES);
            Reference fifteen = reference(history.samples, state.eventTime(), FIFTEEN_MINUTES);
            Reference thirty = reference(history.samples, state.eventTime(), THIRTY_MINUTES);
            history.latest = new OpenInterestState(state.source(), state.symbol(), state.openInterest(),
                    change(state.openInterest(), five), change(state.openInterest(), fifteen),
                    change(state.openInterest(), thirty),
                    fifteen == null ? null : fifteen.openInterest(),
                    fifteen == null ? null : fifteen.eventTime(), state.eventTime(), state.receivedAt());
            return history.latest;
        }
    }

    @Override
    public Optional<OpenInterestState> get(String symbol) {
        SymbolHistory history = histories.get(symbol);
        if (history == null) return Optional.empty();
        synchronized (history) {
            return Optional.ofNullable(history.latest);
        }
    }

    /**
     * Selects the newest observation at or before {@code currentTime - window}. This is based on
     * event time rather than sample count, tolerates polling gaps, and never interpolates.
     */
    private Reference reference(Deque<OpenInterestState> samples, Instant currentTime, Duration window) {
        Instant target = currentTime.minus(window);
        OpenInterestState selected = null;
        for (OpenInterestState sample : samples) {
            if (sample.eventTime().isAfter(target)) break;
            selected = sample;
        }
        return selected == null ? null : new Reference(selected.openInterest(), selected.eventTime());
    }

    private BigDecimal change(BigDecimal current, Reference reference) {
        if (current == null || reference == null || reference.openInterest() == null
                || reference.openInterest().signum() <= 0) return null;
        return current.subtract(reference.openInterest())
                .multiply(BigDecimal.valueOf(100))
                .divide(reference.openInterest(), 10, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private static final class SymbolHistory {
        private final Deque<OpenInterestState> samples = new ArrayDeque<>();
        private OpenInterestState latest;
    }

    private record Reference(BigDecimal openInterest, Instant eventTime) {}
}
