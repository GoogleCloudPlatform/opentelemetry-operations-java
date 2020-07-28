package com.google.cloud.opentelemetry.example.trace;

import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import com.google.cloud.trace.v2.TraceServiceClient;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
//import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

import java.io.IOException;

public class TraceExample {
  private TraceExporter traceExporter;

  // OTel API
  private Tracer tracer = OpenTelemetry.getTracer("io.opentelemetry.example.TraceExample");

  private void setupTraceExporter() {
    
    // using default configuration
    TraceConfiguration configuration = TraceConfiguration.builder().build();

//    OpenTelemetrySdk.getTracerProvider()
//            .addSpanProcessor(SimpleSpansProcessor.newBuilder(this.traceExporter).build());
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
    // System.out.println("TODO: create some spans");
  }
}
