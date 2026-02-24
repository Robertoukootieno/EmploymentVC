-- Create verification_records table
CREATE TABLE IF NOT EXISTS verification_records (
    id BIGSERIAL PRIMARY KEY,
    verification_id VARCHAR(255) NOT NULL UNIQUE,
    verified BOOLEAN NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    message TEXT,
    presentation TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on verification_id for faster lookups
CREATE INDEX idx_verification_id ON verification_records(verification_id);

-- Create index on timestamp for time-based queries
CREATE INDEX idx_timestamp ON verification_records(timestamp);
