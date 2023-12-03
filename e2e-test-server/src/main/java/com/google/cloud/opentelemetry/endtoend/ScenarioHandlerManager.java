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
package com.google.cloud.opentelemetry.endtoend;

import com.google.cloud.opentelemetry.detectors.GCPResourceProvider;
import com.google.cloud.opentelemetry.propagators.XCloudTraceContextPropagator;
import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.ResourceConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * A container for all scenarios we handle.
 *
 * <p>We "dependency inject" scenarios into the constructor.
 */
public class ScenarioHandlerManager {
  private Map<String, ScenarioHandler> scenarios = new HashMap<>();
  private static Logger LOGGER = Logger.getLogger(ScenarioHandlerManager.class.getCanonicalName());

  public ScenarioHandlerManager() {
    register("/health", this::health);
    register("/basicTrace", this::basicTrace);
    register("/basicPropagator", this::basicPropagator);
    register("/detectResource", this::detectResource);
  }

  /** Health check test. */
  private Response health(Request request) {
    return Response.ok();
  }

  /** Basic trace test. */
  private Response basicTrace(Request request) {
    LOGGER.info("Running basicTrace test, request: " + request.toString());
    return withTemporaryTracer(
        (tracer) -> {
          Span span =
              tracer
                  .spanBuilder("basicTrace")
                  .setAttribute(Constants.TEST_ID, request.testId())
                  .startSpan();
          try {
            LOGGER.info("Sending trace with ID: " + span.getSpanContext().getTraceId());
            return Response.ok(Map.of(Constants.TRACE_ID, span.getSpanContext().getTraceId()));
          } finally {
            span.end();
          }
        });
  }

  /** Test where we include resource detection */
  private Response detectResource(Request request) {
    LOGGER.info("Running detectResource test, request: " + request.toString());
    Resource gcpResource =
        new GCPResourceProvider()
            .createResource(
                DefaultConfigProperties.create(
                    Map.of("otel.traces.exporter", "none", "otel.metrics.exporter", "none")));
    Resource resource =
        Resource.getDefault()
            .merge(gcpResource)
            .merge(ResourceConfiguration.createEnvironmentResource());

    return withTemporaryOtel(
        resource,
        (sdk) -> {
          Tracer tracer = sdk.getTestTracer();
          Span span =
              tracer
                  .spanBuilder("resourceDetectionTrace")
                  .setAttribute(Constants.TEST_ID, request.testId())
                  .startSpan();
          try {
            return Response.ok(Map.of(Constants.TRACE_ID, span.getSpanContext().getTraceId()));
          } finally {
            span.end();
          }
        });
  }

  /** Basic trace test. */
  private Response basicPropagator(Request request) {
    LOGGER.info(
        "Running basicPropagator test, request headers: "
            + Arrays.toString(request.headers().entrySet().toArray()));
    // TODO - extract headers from request using text-map-propagators.
    return withTemporaryOtel(
        (ctx) -> {
          Context remoteCtx =
              ctx.getPropagators()
                  .getTextMapPropagator()
                  .extract(Context.current(), request.headers(), MAP_GETTER);

          LOGGER.info("- Found parent span: " + Span.fromContextOrNull(remoteCtx));
          // Run basic scenario in wrapped context.
          try (Scope ignored = remoteCtx.makeCurrent()) {
            Span span =
                ctx.getTestTracer()
                    .spanBuilder("basicPropagator")
                    .setAttribute(Constants.TEST_ID, request.testId())
                    // TODO - This shouldn't be needed.
                    .setParent(remoteCtx)
                    .startSpan();
            try {
              return Response.ok(Map.of(Constants.TRACE_ID, span.getSpanContext().getTraceId()));
            } finally {
              span.end();
            }
          }
        });
  }

  /** Default test scenario runner for unknown test cases. */
  private Response unimplemented(Request request) {
    return Response.unimplemented("Unhandled request: " + request.testId());
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
    return withTemporaryOtel(ctx -> handler.apply(ctx.getTestTracer()));
  }

  private static <R> R withTemporaryOtel(Function<OtelContext, R> handler) {
    return withTemporaryOtel(Resource.empty(), handler);
  }

  /**
   * Helper to configure an OTel SDK exporting to cloud trace within the context of a function call.
   */
  private static <R> R withTemporaryOtel(Resource resource, Function<OtelContext, R> handler) {
    try {
      // TODO - Use resource detectors for
      OpenTelemetrySdk sdk = setupTraceExporter(resource);
      try {
        OtelContext context =
            new OtelContext() {
              @Override
              public Tracer getTestTracer() {
                return sdk.getTracer(Constants.INSTRUMENTING_MODULE_NAME);
              }

              @Override
              public ContextPropagators getPropagators() {
                return sdk.getPropagators();
              }
            };
        return handler.apply(context);
      } finally {
        sdk.getSdkTracerProvider().shutdown();
      }
    } catch (IOException e) {
      // Lift checked exception into runtime to feed through lambda impls and make it
      // out to
      // test status.
      throw new RuntimeException(e);
    }
  }

  interface OtelContext {
    /** Retruns the tracer for this test scenario */
    Tracer getTestTracer();
    /** Returns the context propagators for this test scenario. */
    ContextPropagators getPropagators();
  }

  /** Set up an OpenTelemetrySDK w/ export to google cloud. */
  private static OpenTelemetrySdk setupTraceExporter(Resource resource) throws IOException {
    // Using default project ID and Credentials
    TraceConfiguration configuration =
        TraceConfiguration.builder()
            .setDeadline(Duration.ofMillis(30000))
            .setProjectId(!Objects.equals(Constants.PROJECT_ID, "") ? Constants.PROJECT_ID : null)
            .build();

    SpanExporter traceExporter = TraceExporter.createWithConfiguration(configuration);
    // Register the TraceExporter with OpenTelemetry
    return OpenTelemetrySdk.builder()
        .setPropagators(
            ContextPropagators.create(
                TextMapPropagator.composite(
                    W3CTraceContextPropagator.getInstance(),
                    W3CBaggagePropagator.getInstance(),
                    new XCloudTraceContextPropagator(true))))
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(traceExporter))
                .setResource(resource)
                .build())
        .build();
  }

  private static TextMapGetter<Map<String, String>> MAP_GETTER =
      new TextMapGetter<Map<String, String>>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
          return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
          LOGGER.info("Looking for header key: " + key);
          // We need to ignore case on keys.
          for (String rawKey : carrier.keySet()) {
            if (rawKey.toLowerCase(Locale.ENGLISH).equals(key)) {
              LOGGER.info("Found key: " + rawKey + ", value: " + carrier.get(rawKey));
              return carrier.get(rawKey);
            }
          }
          return null;
        }
      };
}
