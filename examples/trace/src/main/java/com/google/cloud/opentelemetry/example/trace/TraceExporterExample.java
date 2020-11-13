package com.google.cloud.opentelemetry.example.trace;

import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.io.IOException;
import java.time.Duration;

public class TraceExporterExample {
  private TraceExporter traceExporter;

  private Tracer tracer = OpenTelemetry.getGlobalTracer("io.opentelemetry.example.TraceExporterExample");

  private void setupTraceExporter() {
    // Using default project ID and Credentials
    TraceConfiguration configuration =
        TraceConfiguration.builder().setDeadline(Duration.ofMillis(30000)).build();

    try {
      this.traceExporter = TraceExporter.createWithConfiguration(configuration);

      // Register the TraceExporter with OpenTelemetry
      OpenTelemetrySdk.getGlobalTracerManagement()
          .addSpanProcessor(BatchSpanProcessor.builder(this.traceExporter).build());
    } catch (IOException e) {
      System.out.println("Uncaught Exception");
    }
  }

  private void myUseCase() {
    // Generate a span
    Span span = this.tracer.spanBuilder("Start my use case").startSpan();
    span.addEvent("Event 0");
    // Simulate work: this could be simulating a network request or an expensive disk operation
    doWork();

    span.addEvent("Event 1");
    span.end();
  }

  private void doWork() {
    try {
      Thread.sleep((int) (Math.random() * 1000) + 1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    TraceExporterExample example = new TraceExporterExample();
    example.setupTraceExporter();
    example.myUseCase();
  }
}
