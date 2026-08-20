package io.finstream.anomaly;
import static org.assertj.core.api.Assertions.assertThat; import java.time.*; import org.junit.jupiter.api.Test;
class EventCooldownTest {@Test void suppressesSameTypeAndSymbol(){var c=new EventCooldown(Duration.ofMinutes(5),Clock.fixed(Instant.EPOCH,ZoneOffset.UTC)); assertThat(c.acquire("BTCUSDT","RAPID_DROP")).isTrue(); assertThat(c.acquire("BTCUSDT","RAPID_DROP")).isFalse(); assertThat(c.acquire("ETHUSDT","RAPID_DROP")).isTrue(); assertThat(c.acquire("BTCUSDT","RAPID_PUMP")).isTrue();}}
