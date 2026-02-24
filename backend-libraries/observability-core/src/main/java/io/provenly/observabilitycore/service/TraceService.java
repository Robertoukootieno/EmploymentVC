package io.provenly.observabilitycore.service;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

/**
 * Shared tracing helper using OpenTelemetry global SDK.
 */
public class TraceService {

    private final Tracer tracer;

    public TraceService(String instrumentationName) {
        this.tracer = GlobalOpenTelemetry.getTracer(instrumentationName);
    }

    public Span startSpan(String spanName) {
        return tracer.spanBuilder(spanName).startSpan();
    }

    public void endSpan(Span span) {
        if (span != null) {
            span.end();
        }
    }
}
