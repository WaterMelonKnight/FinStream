package io.finstream.state;

import io.finstream.domain.OpenInterestState;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryOpenInterestStateStore implements OpenInterestStateStore {
    private final Map<String, OpenInterestState> latest = new ConcurrentHashMap<>();

    @Override
    public OpenInterestState update(OpenInterestState state) {
        latest.put(state.symbol(), state);
        return state;
    }

    @Override
    public Optional<OpenInterestState> get(String symbol) {
        return Optional.ofNullable(latest.get(symbol));
    }
}
