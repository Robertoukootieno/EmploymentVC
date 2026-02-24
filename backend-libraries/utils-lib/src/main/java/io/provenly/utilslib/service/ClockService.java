package io.provenly.utilslib.service;

import java.time.Instant;

/**
 * Centralized time source abstraction for deterministic testing.
 */
public class ClockService {

    public Instant now() {
        return Instant.now();
    }
}
