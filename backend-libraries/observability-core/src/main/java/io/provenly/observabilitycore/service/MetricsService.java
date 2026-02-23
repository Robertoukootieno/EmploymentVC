package io.provenly.observabilitycore.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.TimeUnit;

/**
 * Shared metrics helper service.
 */
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public MetricsService() {
        this(new SimpleMeterRegistry());
    }

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementCounter(String name) {
        Counter.builder(name).register(meterRegistry).increment();
    }

    public void recordLatencyMillis(String name, long durationMillis) {
        Timer.builder(name).register(meterRegistry).record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public MeterRegistry meterRegistry() {
        return meterRegistry;
    }
}
