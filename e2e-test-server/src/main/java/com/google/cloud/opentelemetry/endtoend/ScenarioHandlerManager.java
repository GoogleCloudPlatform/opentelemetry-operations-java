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
package com.google.cloud.opentelemetry.endtoend;

import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A container for all scenarios we handle.
 *
 * <p>We "dependency inject" scenarios into the constructor.
 */
public class ScenarioHandlerManager {
  private Map<String, ScenarioHandler> scenarios = new HashMap<>();

  public ScenarioHandlerManager() {
    register("/health", this::health);
    register("/basicTrace", this::basicTrace);
  }

  /** Health check test. */
  private Response health(Request request) {
    return Response.ok();
  }

  /** Basic trace test. */
  private Response basicTrace(Request request) {
    return withTemporaryTracer(
        (tracer) -> {
          Span span =
              tracer
                  .spanBuilder("basicTrace")
                  .setAttribute(Constants.TEST_ID, request.testId())
                  .startSpan();
          try {
            return Response.ok();
          } finally {
            span.end();
          }
        });
  }

  /** Default test scenario runner for unknown test cases. */
  private Response unimplemented(Request request) {
    return Response.unimplemented("UNhandled request: " + request.testId());
  }

  /** Registers test scenario "urls" with handlers for the requests. */
  private void register(String scenario, ScenarioHandler handler) {
    scenarios.putIfAbsent(scenario, handler);
  }

  /** Handles a test scenario, or use `unimplemented`. */
  public Response handleScenario(String scenario, Request request) {
    ScenarioHandler handler = scenarios.getOrDefault(scenario, this::unimplemented);
    return handler.handle(request);
  }

  /**
   * Helper to configure an OTel SDK exporting to cloud trace within the context of a function call.
   */
  private static <R> R withTemporaryTracer(Function<Tracer, R> handler) {
    try {
      OpenTelemetrySdk sdk = setupTraceExporter();
      try {
        Tracer tracer = sdk.getTracer(Constants.INSTRUMENTING_MODULE_NAME);
        return handler.apply(tracer);
      } finally {
        sdk.getSdkTracerProvider().shutdown();
      }
    } catch (IOException e) {
      // Lift checked exception into runtime to feed through lambda impls and make it out to
      // test status.
      throw new RuntimeException(e);
    }
  }

  /** Set up an OpenTelemetrySDK w/ export to google cloud. */
  private static OpenTelemetrySdk setupTraceExporter() throws IOException {
    // Using default project ID and Credentials
    TraceConfiguration configuration =
        TraceConfiguration.builder().setDeadline(Duration.ofMillis(30000)).build();

    TraceExporter traceExporter = TraceExporter.createWithConfiguration(configuration);
    // Register the TraceExporter with OpenTelemetry
    return OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build())
                .build())
        .build();
  }
}
