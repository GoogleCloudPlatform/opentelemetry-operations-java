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
package com.google.cloud.opentelemetry.propagators;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PropagatorTest {

  private static TextMapGetter<Map<String, String>> GETTER =
      new TextMapGetter<Map<String, String>>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
          return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
          return carrier.get(key);
        }
      };

  @Test
  public void testExtractSampled() {
    Context context = Context.root();
    Map<String, String> carrier = new HashMap<>();
    carrier.put("x-cloud-trace-context", "00000000000000000000000000000010/15;o=1");
    TextMapPropagator propagator = new XCloudTraceContextPropagator(false);

    // Now try to extract the value.
    Context updated = propagator.extract(context, carrier, GETTER);
    Span span = Span.fromContext(updated);
    Assert.assertNotNull(span);
    Assert.assertEquals("00000000000000000000000000000010", span.getSpanContext().getTraceId());
    Assert.assertEquals("000000000000000f", span.getSpanContext().getSpanId());
    Assert.assertEquals(true, span.getSpanContext().getTraceFlags().isSampled());
  }

  @Test
  public void testExtractOneway() {
    Context context = Context.root();
    Map<String, String> carrier = new HashMap<>();
    carrier.put("x-cloud-trace-context", "00000000000000000000000000000010/15;o=1");
    TextMapPropagator propagator = new XCloudTraceContextPropagator(true);

    // Now try to extract the value.
    Context updated = propagator.extract(context, carrier, GETTER);
    Span span = Span.fromContext(updated);
    Assert.assertNotNull(span);
    Assert.assertEquals("00000000000000000000000000000010", span.getSpanContext().getTraceId());
    Assert.assertEquals("000000000000000f", span.getSpanContext().getSpanId());
    Assert.assertEquals(true, span.getSpanContext().getTraceFlags().isSampled());
  }

  @Test
  public void testExtractNotSampled() {
    Context context = Context.root();
    Map<String, String> carrier = new HashMap<>();
    carrier.put("x-cloud-trace-context", "00000000000000000000000000000011/31;o=0");
    TextMapPropagator propagator = new XCloudTraceContextPropagator(false);

    // Now try to extract the value.
    Context updated = propagator.extract(context, carrier, GETTER);
    Span span = Span.fromContext(updated);
    Assert.assertNotNull(span);
    Assert.assertEquals("00000000000000000000000000000011", span.getSpanContext().getTraceId());
    Assert.assertEquals("000000000000001f", span.getSpanContext().getSpanId());
    Assert.assertEquals(false, span.getSpanContext().getTraceFlags().isSampled());
  }

  @Test
  public void testExtractWithoutTraceFlags() {
    Context context = Context.root();
    Map<String, String> carrier = new HashMap<>();
    carrier.put("x-cloud-trace-context", "00000000000000000000000000000010/15");
    TextMapPropagator propagator = new XCloudTraceContextPropagator(false);

    // Now try to extract the value.
    Context updated = propagator.extract(context, carrier, GETTER);
    Span span = Span.fromContext(updated);
    Assert.assertNotNull(span);
    Assert.assertEquals("00000000000000000000000000000010", span.getSpanContext().getTraceId());
    Assert.assertEquals("000000000000000f", span.getSpanContext().getSpanId());
    Assert.assertEquals(false, span.getSpanContext().getTraceFlags().isSampled());
  }

  @Test
  public void testNotInjectOneway() {
    Span span =
        Span.wrap(
            SpanContext.create(
                "00000000000000000000000000000001",
                "0000000000000002",
                TraceFlags.getSampled(),
                TraceState.getDefault()));
    Context context = Context.root().with(span);
    Map<String, String> carrier = new HashMap<>();
    TextMapPropagator propagator = new XCloudTraceContextPropagator(true);

    // Now try to inject the value.
    propagator.inject(context, carrier, Map::put);
    Assert.assertNull(carrier.get("x-cloud-trace-context"));
  }

  @Test
  public void testInjectSampled() {
    Span span =
        Span.wrap(
            SpanContext.create(
                "00000000000000000000000000000001",
                "0000000000000002",
                TraceFlags.getSampled(),
                TraceState.getDefault()));
    Context context = Context.root().with(span);
    Map<String, String> carrier = new HashMap<>();
    TextMapPropagator propagator = new XCloudTraceContextPropagator(false);

    // Now try to inject the value.
    propagator.inject(context, carrier, Map::put);
    Assert.assertEquals(
        "00000000000000000000000000000001/2;o=1", carrier.get("x-cloud-trace-context"));
  }

  @Test
  public void testInjectNotSampled() {
    Span span =
        Span.wrap(
            SpanContext.create(
                "00000000000000000000000000000002",
                "0000000000000013",
                TraceFlags.getDefault(),
                TraceState.getDefault()));
    Context context = Context.root().with(span);
    Map<String, String> carrier = new HashMap<>();
    TextMapPropagator propagator = new XCloudTraceContextPropagator(false);

    // Now try to inject the value.
    propagator.inject(context, carrier, Map::put);
    // Note - SpanID is hex (otel) -> decimal (gcp)
    Assert.assertEquals(
        "00000000000000000000000000000002/19;o=0", carrier.get("x-cloud-trace-context"));
  }
}
