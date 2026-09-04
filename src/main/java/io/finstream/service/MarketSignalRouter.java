package io.finstream.service;

import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MarketSignalRouter {
    private static final Logger log = LoggerFactory.getLogger(MarketSignalRouter.class);

    private final Map<MarketSignalType, MarketSignalProcessor> processors;

    public MarketSignalRouter(List<MarketSignalProcessor> processors) {
        Map<MarketSignalType, MarketSignalProcessor> byType = new EnumMap<>(MarketSignalType.class);
        for (MarketSignalProcessor processor : processors) {
            if (byType.putIfAbsent(processor.signalType(), processor) != null) {
                throw new IllegalArgumentException(
                        "Multiple processors registered for " + processor.signalType());
            }
        }
        this.processors = Map.copyOf(byType);
    }

    public List<FinancialEvent> route(MarketEvent event) {
        MarketSignalProcessor processor = processors.get(event.signalType());
        if (processor == null) {
            log.debug("No processor registered for market signal {}", event.signalType());
            return List.of();
        }
        return processor.process(event);
    }
}
