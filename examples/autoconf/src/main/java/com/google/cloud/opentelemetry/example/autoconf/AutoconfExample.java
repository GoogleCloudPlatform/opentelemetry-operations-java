/*
 * Copyright 2023 Google LLC
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
package com.google.cloud.opentelemetry.example.autoconf;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class AutoconfExample {
  private static final Random random = new Random();
  private static final AttributeKey<String> DESCRIPTION_KEY = AttributeKey.stringKey("description");

  private static Tracer tracer;
  private static LongCounter useCaseCount;
  private static DoubleHistogram fakeLatency;

  private static void myUseCase(String description) {
    Span span = tracer.spanBuilder(description).startSpan();
    try (Scope scope = span.makeCurrent()) {
      useCaseCount.add(1, Attributes.of(DESCRIPTION_KEY, description));
      fakeLatency.record(1, Attributes.of(DESCRIPTION_KEY, description));
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
    Span span = tracer.spanBuilder(description).startSpan();
    try (Scope scope = span.makeCurrent()) {
      // Simulate work: this could be simulating a network request or an expensive disk operation
      long millis = 100 + random.nextInt(5) * 100;
      fakeLatency.record(millis, Attributes.of(DESCRIPTION_KEY, description.substring(0, 3)));
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      span.end();
    }
  }

  public static void main(String[] args) {
    // First, make sure we've configured opentelemetry with autoconfigure module.
    OpenTelemetrySdk openTelemetrySdk =
        AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
    Objects.requireNonNull(openTelemetrySdk, "Failed to autoconfigure opentelemetry");

    // Initialize instruments
    tracer = openTelemetrySdk.tracerBuilder("example-auto").build();
    Meter meter = openTelemetrySdk.meterBuilder("example-auto").build();

    // Initialize meters
    useCaseCount = meter.counterBuilder("use_case").build();
    fakeLatency = meter.histogramBuilder("fakeLatency").build();

    // Application-specific logic
    myUseCase("One");
    myUseCase("Two");

    // Shutdown the OpenTelemetry instance
    CompletableResultCode completableResultCode = openTelemetrySdk.shutdown();
    completableResultCode.join(10, TimeUnit.SECONDS);
  }
}
