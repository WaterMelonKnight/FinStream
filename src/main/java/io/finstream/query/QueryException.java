package io.finstream.query;

public class QueryException extends RuntimeException {
    private final String code;
    private final boolean notFound;

    public QueryException(String code, String message, boolean notFound) {
        super(message);
        this.code = code;
        this.notFound = notFound;
    }

    public String code() { return code; }
    public boolean notFound() { return notFound; }
}
