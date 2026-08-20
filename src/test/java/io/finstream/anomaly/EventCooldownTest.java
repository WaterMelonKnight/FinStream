package io.finstream.anomaly;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class EventCooldownTest {
    @Test
    void commitStartsCooldownForSameTypeAndSymbol() {
        EventCooldown cooldown = fixedCooldown();
        EventCooldown.Reservation reservation =
                cooldown.reserve("BTCUSDT", "RAPID_DROP").orElseThrow();

        assertThat(cooldown.reserve("BTCUSDT", "RAPID_DROP")).isEmpty();
        cooldown.commit(reservation);

        assertThat(cooldown.reserve("BTCUSDT", "RAPID_DROP")).isEmpty();
        assertThat(cooldown.reserve("ETHUSDT", "RAPID_DROP")).isPresent();
        assertThat(cooldown.reserve("BTCUSDT", "RAPID_PUMP")).isPresent();
    }

    @Test
    void releaseAfterPersistenceFailureAllowsRetry() {
        EventCooldown cooldown = fixedCooldown();
        EventCooldown.Reservation reservation =
                cooldown.reserve("BTCUSDT", "RAPID_DROP").orElseThrow();

        cooldown.release(reservation);

        assertThat(cooldown.reserve("BTCUSDT", "RAPID_DROP")).isPresent();
    }

    private EventCooldown fixedCooldown() {
        return new EventCooldown(
                Duration.ofMinutes(5), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    }
}
