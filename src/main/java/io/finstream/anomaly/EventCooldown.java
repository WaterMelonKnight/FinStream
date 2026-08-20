package io.finstream.anomaly;

import io.finstream.config.FinStreamProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class EventCooldown {
    private final Duration duration;
    private final Clock clock;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public EventCooldown(FinStreamProperties properties) {
        this(properties.anomaly().cooldown(), Clock.systemUTC());
    }

    EventCooldown(Duration duration, Clock clock) {
        this.duration = duration;
        this.clock = clock;
    }

    /**
     * Reserves a key while persistence is in flight. The caller must commit the reservation after a
     * successful save or release it after a failure.
     */
    public Optional<Reservation> reserve(String symbol, String eventType) {
        String key = symbol + ":" + eventType;
        Instant now = clock.instant();
        Reservation reservation = new Reservation(key, UUID.randomUUID());
        boolean[] accepted = {false};
        entries.compute(key, (ignored, current) -> {
            if (current == null || (!current.pending() && !current.cooldownUntil().isAfter(now))) {
                accepted[0] = true;
                return Entry.pending(reservation.id());
            }
            return current;
        });
        return accepted[0] ? Optional.of(reservation) : Optional.empty();
    }

    public void commit(Reservation reservation) {
        entries.computeIfPresent(reservation.key(), (ignored, current) ->
                current.matchesPending(reservation.id())
                        ? Entry.coolingDown(reservation.id(), clock.instant().plus(duration))
                        : current);
    }

    public void release(Reservation reservation) {
        entries.computeIfPresent(reservation.key(), (ignored, current) ->
                current.matchesPending(reservation.id()) ? null : current);
    }

    public record Reservation(String key, UUID id) {}

    private record Entry(UUID reservationId, boolean pending, Instant cooldownUntil) {
        private static Entry pending(UUID id) {
            return new Entry(id, true, Instant.MIN);
        }

        private static Entry coolingDown(UUID id, Instant until) {
            return new Entry(id, false, until);
        }

        private boolean matchesPending(UUID id) {
            return pending && reservationId.equals(id);
        }
    }
}
