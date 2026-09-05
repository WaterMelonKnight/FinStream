package io.finstream.state;

import io.finstream.domain.OpenInterestState;
import java.util.Optional;

public interface OpenInterestStateStore {
    OpenInterestState update(OpenInterestState state);
    Optional<OpenInterestState> get(String symbol);
}
