package io.finstream.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("finstream")
public record FinStreamProperties(Market market, Anomaly anomaly) {
    public FinStreamProperties {
        market = market == null ? new Market(null, null) : market;
        anomaly = anomaly == null ? new Anomaly(null, null, null, null) : anomaly;
    }
    public record Market(List<String> symbols, Binance binance) {
        public Market { symbols = symbols == null ? List.of("BTCUSDT", "ETHUSDT", "SOLUSDT") : List.copyOf(symbols); binance = binance == null ? new Binance(false, null) : binance; }
    }
    public record Binance(boolean enabled, String baseUrl) {
        public Binance { baseUrl = baseUrl == null ? "wss://stream.binance.com:9443/stream?streams=" : baseUrl; }
    }
    public record Anomaly(Duration cooldown, Rule rapidDrop, Rule rapidPump, VolumeRule abnormalVolume) {
        public Anomaly { cooldown = cooldown == null ? Duration.ofMinutes(10) : cooldown; rapidDrop = rapidDrop == null ? new Rule(true, Duration.ofMinutes(5), 3) : rapidDrop; rapidPump = rapidPump == null ? new Rule(true, Duration.ofMinutes(5), 3) : rapidPump; abnormalVolume = abnormalVolume == null ? new VolumeRule(true, 3) : abnormalVolume; }
    }
    public record Rule(boolean enabled, Duration window, double thresholdPercent) {}
    public record VolumeRule(boolean enabled, double ratio) {}
}
