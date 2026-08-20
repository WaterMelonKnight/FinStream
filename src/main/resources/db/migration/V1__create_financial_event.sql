CREATE TABLE financial_event (
    id UUID PRIMARY KEY,
    source VARCHAR(32) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    severity VARCHAR(16) NOT NULL,
    anomaly_score DOUBLE PRECISION NOT NULL,
    summary TEXT NOT NULL,
    metrics JSONB NOT NULL,
    evidence JSONB NOT NULL
);
CREATE INDEX idx_financial_event_symbol_time ON financial_event(symbol, event_time DESC);
CREATE INDEX idx_financial_event_type_time ON financial_event(event_type, event_time DESC);
