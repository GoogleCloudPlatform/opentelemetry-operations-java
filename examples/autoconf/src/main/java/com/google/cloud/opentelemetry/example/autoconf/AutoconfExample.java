/*
 * Copyright 2021 Google
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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import java.util.Objects;
import java.util.Random;

public class AutoconfExample {
  private static final Random random = new Random();

  private final Tracer tracer = GlobalOpenTelemetry.get().tracerBuilder("example-auto").build();
  private final Meter meter = GlobalMeterProvider.get().meterBuilder("example-auto").build();
  private final AttributeKey<String> DESCRIPTION_KEY = AttributeKey.stringKey("description");
  private final LongCounter useCaseCount = meter.counterBuilder("use_case").build();

  private void myUseCase(String description) {
    Span span = tracer.spanBuilder(description).startSpan();
    try (Scope scope = span.makeCurrent()) {
      useCaseCount.add(1, Attributes.of(DESCRIPTION_KEY, description));
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

  private void doWork(String description) {
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
    // First, make sure we've configured opentelemetry with autoconfigure module.
    Objects.requireNonNull(GlobalOpenTelemetry.get(), "Failed to autoconfigure opentelemetry");
    Objects.requireNonNull(
        GlobalMeterProvider.get(), "Failed to autoconfigure opentelmetry metrics");

    AutoconfExample example = new AutoconfExample();
    // Application-specific logic
    example.myUseCase("One");
    example.myUseCase("Two");

    // Autoconf module registers shutdown hooks to flush telemetry, but metrics should be forced
    // for short-lived processes.
    IntervalMetricReader.forceFlushGlobal();
  }
}
