package com.google.cloud.opentelemetry.example.trace;

import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.io.IOException;
import java.time.Duration;
import java.util.Random;

public class TraceExporterExample {
  // TODO: Update this to NON-global OpenTelemetry.  Only APIs/Frameworks should be using global.
  private static final Tracer tracer = GlobalOpenTelemetry.getTracer("io.opentelemetry.example.TraceExporterExample");
  private static final Random random = new Random();

  private static void setupTraceExporter() {
    // Using default project ID and Credentials
    TraceConfiguration configuration =
        TraceConfiguration.builder().setDeadline(Duration.ofMillis(30000)).build();

    try {
      TraceExporter traceExporter = TraceExporter.createWithConfiguration(configuration);

      // Register the TraceExporter with OpenTelemetry
      OpenTelemetrySdk.getGlobalTracerManagement()
          .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build());
    } catch (IOException e) {
      System.out.println("Uncaught Exception");
    }
  }

  private static void myUseCase(String description) {
    // Generate a span
    Span span = tracer.spanBuilder(description).startSpan();
    try (Scope scope = span.makeCurrent()) {
      span.addEvent("Event A");
      // Do some work for the use case
      for (int i=0; i<3; i++) {
        String work = String.format("%s - Work #%d", description, (i+1));
        doWork(work);
      }

      span.addEvent("Event B");
    } finally {
      span.end();
    }
  }

  private static void doWork(String description) {
    // Child span
    Span span = tracer.spanBuilder(description).startSpan();
    try (Scope scope = span.makeCurrent()) {
      // Simulate work: this could be simulating a network request or an expensive disk operation
      Thread.sleep(100 + random.nextInt(5) * 100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      span.end();
    }
  }

  public static void main(String[] args) {
    // Configure the OpenTelemetry pipeline with CloudTrace exporter
    setupTraceExporter();

    // Application-specific logic
    myUseCase("One");
    myUseCase("Two");

    // Flush all buffered traces
    OpenTelemetrySdk.getGlobalTracerManagement().shutdown();
  }
}
