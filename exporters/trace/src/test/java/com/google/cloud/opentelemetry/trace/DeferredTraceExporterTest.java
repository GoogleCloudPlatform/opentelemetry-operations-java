package com.google.cloud.opentelemetry.trace;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

@RunWith(JUnit4.class)
public class DeferredTraceExporterTest {

    @Test
    public void badStuff() throws IOException {
//    TraceExporter gcpTraceExporter = TraceExporter.createWithConfiguration(TraceConfiguration.builder() // this seems to cause it
//            // .setFixedAttributes(/* ... */)
//            .build());


        SpanExporter gcpTraceExporter = TraceExporter.createWithConfiguration(TraceConfiguration.builder());

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(gcpTraceExporter).build())
                .build();


        OpenTelemetrySdk otel = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

    }
}