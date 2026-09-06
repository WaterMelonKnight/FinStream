package io.finstream.query;

import java.util.Locale;
import java.util.Set;

final class QueryParameters {
    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 200;
    private static final Set<String> EVENT_TYPES =
            Set.of("RAPID_DROP", "RAPID_PUMP", "ABNORMAL_VOLUME", "FUNDING_EXTREME", "OPEN_INTEREST_SURGE");

    private QueryParameters() {}

    static String symbol(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (!required) return null;
            throw new QueryException("INVALID_SYMBOL", "symbol is required", false);
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9]{2,20}")) {
            throw new QueryException("INVALID_SYMBOL", "symbol must contain 2-20 letters or digits", false);
        }
        return normalized;
    }

    static String eventType(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!EVENT_TYPES.contains(normalized)) {
            throw new QueryException("INVALID_EVENT_TYPE", "Unsupported eventType: " + value, false);
        }
        return normalized;
    }

    static int limit(Integer value) {
        if (value == null) return DEFAULT_LIMIT;
        if (value < 1) throw new QueryException("INVALID_LIMIT", "limit must be at least 1", false);
        return Math.min(value, MAX_LIMIT);
    }
}
