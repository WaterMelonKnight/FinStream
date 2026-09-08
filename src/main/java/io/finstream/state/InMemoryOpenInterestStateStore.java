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
    static final Duration REFERENCE_MAX_LAG = Duration.ofMinutes(2);
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
            // A polling snapshot is a new observation even when Binance repeats its transaction
            // timestamp. Only an exact duplicate observation replaces the prior sample.
            if (latest != null && state.eventTime().equals(latest.eventTime())
                    && state.receivedAt().equals(latest.receivedAt())) {
                history.samples.removeLast();
            }
            history.samples.addLast(state);
            Instant observationTime = state.receivedAt();
            Instant cutoff = observationTime.minus(HISTORY_RETENTION);
            while (!history.samples.isEmpty()
                    && history.samples.peekFirst().receivedAt().isBefore(cutoff)) {
                history.samples.removeFirst();
            }
            ReferenceSelection five = reference(history.samples, observationTime, FIVE_MINUTES);
            ReferenceSelection fifteen = reference(history.samples, observationTime, FIFTEEN_MINUTES);
            ReferenceSelection thirty = reference(history.samples, observationTime, THIRTY_MINUTES);
            history.latest = new OpenInterestState(state.source(), state.symbol(), state.openInterest(),
                    change(state.openInterest(), five.reference()),
                    change(state.openInterest(), fifteen.reference()),
                    change(state.openInterest(), thirty.reference()),
                    fifteen.reference() == null ? null : fifteen.reference().openInterest(),
                    fifteen.reference() == null ? null : fifteen.reference().eventTime(),
                    state.eventTime(), state.receivedAt());
            log.debug("Updated Open Interest rolling state: symbol={}, currentEventTime={}, "
                            + "receivedAt={}, historySize={}, target5m={}, reference5mEventTime={}, "
                            + "change5mPercent={}, reference5mStatus={}, target15m={}, "
                            + "reference15mEventTime={}, change15mPercent={}, reference15mStatus={}, "
                            + "target30m={}, reference30mEventTime={}, change30mPercent={}, "
                            + "reference30mStatus={}",
                    state.symbol(), state.eventTime(), state.receivedAt(), history.samples.size(),
                    five.target(), candidateEventTime(five), history.latest.change5mPercent(), five.status(),
                    fifteen.target(), candidateEventTime(fifteen), history.latest.change15mPercent(),
                    fifteen.status(), thirty.target(), candidateEventTime(thirty),
                    history.latest.change30mPercent(), thirty.status());
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
     * polling observation time ({@code receivedAt}) rather than sample count and never
     * interpolates. The candidate must be no more
     * than two minutes older than the target, tolerating short polling gaps without reusing stale
     * history as a misleading window reference.
     */
    private ReferenceSelection reference(
            Deque<OpenInterestState> samples, Instant currentTime, Duration window) {
        Instant target = currentTime.minus(window);
        OpenInterestState selected = null;
        for (OpenInterestState sample : samples) {
            if (sample.receivedAt().isAfter(target)) break;
            selected = sample;
        }
        if (selected == null) {
            return new ReferenceSelection(target, null, ReferenceStatus.NO_REFERENCE);
        }
        Reference reference = new Reference(
                selected.openInterest(), selected.eventTime(), selected.receivedAt());
        if (Duration.between(selected.receivedAt(), target).compareTo(REFERENCE_MAX_LAG) > 0) {
            return new ReferenceSelection(target, reference, ReferenceStatus.REFERENCE_TOO_STALE);
        }
        if (selected.openInterest() == null || selected.openInterest().signum() <= 0) {
            return new ReferenceSelection(target, reference, ReferenceStatus.REFERENCE_NON_POSITIVE);
        }
        return new ReferenceSelection(target, reference, ReferenceStatus.AVAILABLE);
    }

    private BigDecimal change(BigDecimal current, Reference reference) {
        if (current == null || reference == null || reference.openInterest() == null
                || reference.openInterest().signum() <= 0) return null;
        return current.subtract(reference.openInterest())
                .multiply(BigDecimal.valueOf(100))
                .divide(reference.openInterest(), 10, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private Instant candidateEventTime(ReferenceSelection selection) {
        return selection.candidate() == null ? null : selection.candidate().eventTime();
    }

    private static final class SymbolHistory {
        private final Deque<OpenInterestState> samples = new ArrayDeque<>();
        private OpenInterestState latest;
    }

    private record Reference(BigDecimal openInterest, Instant eventTime, Instant receivedAt) {}

    private record ReferenceSelection(
            Instant target, Reference candidate, ReferenceStatus status) {
        private Reference reference() {
            return status == ReferenceStatus.AVAILABLE ? candidate : null;
        }
    }

    private enum ReferenceStatus {
        AVAILABLE,
        NO_REFERENCE,
        REFERENCE_TOO_STALE,
        REFERENCE_NON_POSITIVE
    }
}
