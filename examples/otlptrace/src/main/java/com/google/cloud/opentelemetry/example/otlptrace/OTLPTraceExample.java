/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.opentelemetry.example.otlptrace;

import com.google.auth.oauth2.GoogleCredentials;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class OTLPTraceExample {
  private static final String INSTRUMENTATION_SCOPE_NAME = OTLPTraceExample.class.getName();
  private static final Random random = new Random();

  private static OpenTelemetrySdk openTelemetrySdk;

  private static OpenTelemetrySdk setupTraceExporter() throws IOException {
    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

    // Register the TraceExporter with OpenTelemetry
    return OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addSpanProcessor(
                    BatchSpanProcessor.builder(
                            OtlpHttpSpanExporter.builder()
                                .addHeader(
                                    "Authorization", "Bearer " + credentials.getAccessToken())
                                .build())
                        .build())
                .build())
        .buildAndRegisterGlobal();
  }

  private static void myUseCase(String description) {
    // Generate a span
    Span span =
        openTelemetrySdk.getTracer(INSTRUMENTATION_SCOPE_NAME).spanBuilder(description).startSpan();
    try (Scope scope = span.makeCurrent()) {
      span.addEvent("Event A");
      // Do some work for the use case
      for (int i = 0; i < 3; i++) {
        String work = String.format("%s - Work #%d", description, (i + 1));
        doWork(work);
      }

      span.addEvent("Event B");
    } finally {
      span.end();
    }
  }

  private static void doWork(String description) {
    // Child span
    Span span =
        openTelemetrySdk.getTracer(INSTRUMENTATION_SCOPE_NAME).spanBuilder(description).startSpan();
    try (Scope scope = span.makeCurrent()) {
      // Simulate work: this could be simulating a network request or an expensive disk operation
      Thread.sleep(100 + random.nextInt(5) * 100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      span.end();
    }
  }

  public static void main(String[] args) throws IOException {
    // Configure the OpenTelemetry pipeline with CloudTrace exporter
    openTelemetrySdk = setupTraceExporter();

    // Application-specific logic
    myUseCase("One");
    myUseCase("Two");

    // Flush all buffered traces
    CompletableResultCode completableResultCode =
        openTelemetrySdk.getSdkTracerProvider().shutdown();
    // wait till export finishes
    completableResultCode.join(10000, TimeUnit.MILLISECONDS);
  }
}
