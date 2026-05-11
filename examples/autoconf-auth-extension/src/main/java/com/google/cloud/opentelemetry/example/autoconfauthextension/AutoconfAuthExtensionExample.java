/*
 * Copyright 2026 Google LLC
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
package com.google.cloud.opentelemetry.example.autoconfauthextension;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class AutoconfAuthExtensionExample {
  private static final String INSTRUMENTATION_SCOPE_NAME =
      AutoconfAuthExtensionExample.class.getName();
  private static final Random RANDOM = new Random();

  private static OpenTelemetrySdk openTelemetrySdk;

  private static void myUseCase(String description, LongCounter counter) {
    // Generate a span
    Span span =
        openTelemetrySdk.getTracer(INSTRUMENTATION_SCOPE_NAME).spanBuilder(description).startSpan();
    try (Scope scope = span.makeCurrent()) {
      span.addEvent("Event A");
      // Do some work for the use case
      for (int i = 0; i < 3; i++) {
        String work = String.format("%s - Work #%d", description, (i + 1));
        doWork(work, counter);
      }
      span.addEvent("Event B");
    } finally {
      span.end();
    }
  }

  private static void doWork(String description, LongCounter counter) {
    // Child span
    Span span =
        openTelemetrySdk.getTracer(INSTRUMENTATION_SCOPE_NAME).spanBuilder(description).startSpan();
    try (Scope scope = span.makeCurrent()) {
      // Simulate work: this could be simulating a network request or an expensive disk operation
      int randomSleep = RANDOM.nextInt(5) * 100;
      span.setAttribute("RandomSleep", randomSleep);
      Thread.sleep(100 + randomSleep);

      // Record a metric
      counter.add(1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      span.end();
    }
  }

  public static void main(String[] args) {
    // Configure the OpenTelemetry pipeline with Auto configuration
    // The gcp-auth-extension is picked up automatically via SPI if present on classpath
    openTelemetrySdk = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();

    // Create a counter
    LongCounter counter =
        openTelemetrySdk
            .getMeter(INSTRUMENTATION_SCOPE_NAME)
            .counterBuilder("example_work_counter")
            .setDescription("Counts the number of work items processed")
            .setUnit("1")
            .build();

    // Application-specific logic
    myUseCase("One", counter);
    myUseCase("Two", counter);

    System.out.println("Telemetry generated. Shutting down SDK...");

    // Flush all buffered telemetry
    CompletableResultCode traceShutdown = openTelemetrySdk.getSdkTracerProvider().shutdown();
    CompletableResultCode meterShutdown = openTelemetrySdk.getSdkMeterProvider().shutdown();

    // Wait for export to finish
    traceShutdown.join(10000, TimeUnit.MILLISECONDS);
    meterShutdown.join(10000, TimeUnit.MILLISECONDS);

    System.out.println("SDK shut down complete.");
  }
}
