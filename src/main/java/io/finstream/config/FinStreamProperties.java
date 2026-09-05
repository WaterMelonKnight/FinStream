package io.finstream.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("finstream")
public record FinStreamProperties(Market market, Anomaly anomaly) {
    public FinStreamProperties {
        market = market == null ? new Market(null, null) : market;
        anomaly = anomaly == null ? new Anomaly(null, null, null, null, null) : anomaly;
    }

    public record Market(List<String> symbols, Binance binance) {
        public Market {
            symbols = symbols == null
                    ? List.of("BTCUSDT", "ETHUSDT", "SOLUSDT")
                    : List.copyOf(symbols);
            binance = binance == null ? new Binance(false, null) : binance;
        }
    }

    public record Binance(
            boolean enabled, String baseUrl, FundingRate fundingRate,
            OpenInterest openInterest) {
        public Binance(boolean enabled, String baseUrl) {
            this(enabled, baseUrl, null, null);
        }

        public Binance(boolean enabled, String baseUrl, FundingRate fundingRate) {
            this(enabled, baseUrl, fundingRate, null);
        }

        public Binance {
            baseUrl = baseUrl == null
                    ? "wss://stream.binance.com:9443/stream?streams="
                    : baseUrl;
            fundingRate = fundingRate == null ? new FundingRate(false, null, null) : fundingRate;
            openInterest = openInterest == null ? new OpenInterest(false, null, null) : openInterest;
        }
    }

    public record OpenInterest(boolean enabled, String baseUrl, Duration pollInterval) {
        public OpenInterest {
            baseUrl = baseUrl == null ? "https://fapi.binance.com" : baseUrl;
            pollInterval = pollInterval == null ? Duration.ofSeconds(30) : pollInterval;
            if (pollInterval.isZero() || pollInterval.isNegative()) {
                throw new IllegalArgumentException("Open interest poll interval must be positive");
            }
        }
    }

    public record FundingRate(boolean enabled, String baseUrl, Duration pollInterval) {
        public FundingRate {
            baseUrl = baseUrl == null ? "https://fapi.binance.com" : baseUrl;
            pollInterval = pollInterval == null ? Duration.ofSeconds(60) : pollInterval;
            if (pollInterval.isZero() || pollInterval.isNegative()) {
                throw new IllegalArgumentException("Funding rate poll interval must be positive");
            }
        }
    }

    public record Anomaly(
            Duration cooldown, Rule rapidDrop, Rule rapidPump, VolumeRule abnormalVolume,
            FundingExtreme fundingExtreme) {
        public Anomaly(Duration cooldown, Rule rapidDrop, Rule rapidPump, VolumeRule abnormalVolume) {
            this(cooldown, rapidDrop, rapidPump, abnormalVolume, null);
        }

        public Anomaly {
            cooldown = cooldown == null ? Duration.ofMinutes(10) : cooldown;
            rapidDrop = rapidDrop == null ? new Rule(true, 3) : rapidDrop;
            rapidPump = rapidPump == null ? new Rule(true, 3) : rapidPump;
            abnormalVolume = abnormalVolume == null ? new VolumeRule(true, 3) : abnormalVolume;
            fundingExtreme = fundingExtreme == null
                    ? new FundingExtreme(true, new BigDecimal("0.001"))
                    : fundingExtreme;
        }
    }

    public record Rule(boolean enabled, double thresholdPercent) {}

    public record VolumeRule(boolean enabled, double ratio) {}

    /** Threshold is a decimal rate: 0.001 means 0.1%. */
    public record FundingExtreme(boolean enabled, BigDecimal threshold) {
        public FundingExtreme {
            threshold = threshold == null ? new BigDecimal("0.001") : threshold;
            if (threshold.signum() <= 0) {
                throw new IllegalArgumentException("Funding extreme threshold must be positive");
            }
        }
    }
}
