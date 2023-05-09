package com.google.logging.enhancers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.logback.LoggingEventEnhancer;
import com.google.appengine.api.utils.SystemProperty;

/**
 * LogEntry enhancer that correlates a log with its corresponding trace by adding trace & span IDs.
 * Can also be used to add any additional custom labels that need to be attached to the LogEntry.
 */
public class LogTraceCorrelationEnhancer implements LoggingEventEnhancer {
  private static final String PROJECT_ID = SystemProperty.applicationId.get();

  @Override
  public void enhanceLogEntry(LogEntry.Builder builder, ILoggingEvent iLoggingEvent) {
    // The OpenTelemetryAppender appends the trace ID and the span ID to the MDC property map.
    String traceId = iLoggingEvent.getMDCPropertyMap().getOrDefault("trace_id", "");
    String spanId = iLoggingEvent.getMDCPropertyMap().getOrDefault("span_id", "");
    // We extract the trace and span IDs and set it for the Google Cloud Logging LogEntry
    // in a format it expects.
    builder.setTrace(String.format("projects/%s/traces/%s", PROJECT_ID, traceId));
    builder.setSpanId(spanId);
    builder.setTraceSampled(true);
    // Optional - add any other custom labels required
    builder.addLabel("custom", "test");
  }
}
