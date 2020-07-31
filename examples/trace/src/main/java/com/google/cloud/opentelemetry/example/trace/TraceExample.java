package com.google.cloud.opentelemetry.example.trace;

import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

import java.io.IOException;
import java.time.Duration;

public class TraceExample {
  private TraceExporter traceExporter;

  private Tracer tracer = OpenTelemetry.getTracer("io.opentelemetry.example.TraceExample");

  private void setupTraceExporter() {
    // using default project ID
    TraceConfiguration configuration =
        TraceConfiguration.builder().setDeadline(Duration.ofMillis(30000)).build();

    try {
      this.traceExporter = TraceExporter.createWithConfiguration(configuration);

      OpenTelemetrySdk.getTracerProvider()
          .addSpanProcessor(SimpleSpanProcessor.newBuilder(this.traceExporter).build());
    } catch (IOException e) {
      System.out.println("Uncaught Excetption");
    }
  }

  private void myUseCase() {
    // generate a span
    Span span = this.tracer.spanBuilder("Start my use case").startSpan();
    span.addEvent("Event 0");
    // simulate work
    doWork();
    span.addEvent("Event 1");
    span.end();
  }

  private void doWork() {
    try {
      Thread.sleep((int) (Math.random() * 1000) + 1000);
    } catch (InterruptedException e) {
    }
  }

  public static void main(String[] args) {
    TraceExample example = new TraceExample();
    example.setupTraceExporter();

    while (true) {
      example.myUseCase();
      try {
        // one hour
        Thread.sleep(1000 * 60 * 60);
      } catch (InterruptedException e) {
      }
    }
  }
}
