package com.google.cloud.opentelemetry.example.trace;

import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import com.google.cloud.trace.v2.TraceServiceClient;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.trace.Span;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.trace.Tracer;
import java.time.Duration;

import java.io.IOException;

public class TraceExample {
  private TraceExporter traceExporter;

  // OTel API
  private Tracer tracer = OpenTelemetry.getTracer("io.opentelemetry.example.TraceExample");

  private void setupTraceExporter() {
    
    // using default project ID
    TraceConfiguration configuration = TraceConfiguration.builder().setDeadline(Duration.ofMillis(30000)).build();

    try {
      this.traceExporter = TraceExporter.createWithConfiguration(configuration);
    } catch(IOException e) {
      System.out.println("Uncaught Excetption");
    }

    OpenTelemetrySdk.getTracerProvider()
            .addSpanProcessor(SimpleSpanProcessor.newBuilder(this.traceExporter).build());
    System.out.println("Set up");
  }

  private void myUseCase() {
    // Generate a span
    Span span = this.tracer.spanBuilder("Start my wonderful use case").startSpan();
    span.addEvent("Event 0");
    // execute my use case - here we simulate a wait
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

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
    System.out.println("Done");

  }
}
