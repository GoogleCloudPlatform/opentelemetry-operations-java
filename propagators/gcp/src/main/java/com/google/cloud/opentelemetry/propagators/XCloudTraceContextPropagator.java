/*
 * Copyright 2023 Google
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
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A context propagator that leverages the X-Cloud-Trace-Context header.
 *
 * <p>See: <a href="https://cloud.google.com/trace/docs/setup#force-trace">Google Cloud Trace
 * Documentation</a> for details.
 */
public final class XCloudTraceContextPropagator implements TextMapPropagator {

  private static String FIELD = "x-cloud-trace-context";
  private static Collection<String> FIELDS = Collections.singletonList(FIELD);
  private static Pattern VALUE_PATTERN =
      Pattern.compile("(?<traceid>[0-9a-f]{32})\\/(?<spanid>[\\d]{1,20});o=(?<sampled>\\d+)");
  private static Logger LOGGER = Logger.getLogger("XCloudTraceContextPropogator");

  private final boolean oneway;

  /**
   * Constructs a new text map propogator that leverages the X-Cloud-Trace-Context header.
   *
   * @param oneway boolean to configure if the trace should propagate in a single direction.
   */
  public XCloudTraceContextPropagator(boolean oneway) {
    this.oneway = oneway;
  }

  @Override
  public Collection<String> fields() {
    return FIELDS;
  }

  @Override
  public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
    // We never inject cloud trace context information in oneway mode.
    if (oneway) {
      return;
    }
    Span current = Span.fromContext(context);
    if (!current.getSpanContext().isValid()) {
      return;
    }
    String sampledString = current.getSpanContext().isSampled() ? "1" : "0";
    String spanIdString =
        java.lang.Long.toUnsignedString(
            java.lang.Long.parseUnsignedLong(current.getSpanContext().getSpanId(), 16));
    String value =
        current.getSpanContext().getTraceId() + "/" + spanIdString + ";o=" + sampledString;
    setter.set(carrier, FIELD, value);
  }

  @Override
  public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
    if (context == null || getter == null) {
      return context;
    }
    // Ignore the scenario where another propagator already filled out a field.
    if (Span.fromContext(context).getSpanContext().isRemote()) {
      return context;
    }
    // TODO - Do we need to iterate over keys and lowercase them, or is java doing that for us?
    String value = getter.get(carrier, FIELD);
    if (value == null) {
      return context;
    }
    Matcher matcher = VALUE_PATTERN.matcher(value);
    if (!matcher.matches()) {
      LOGGER.fine(() -> "Found x-cloud-trace-context header with invalid format: " + value);
      return context;
    }
    String traceId = matcher.group("traceid");
    if (!TraceId.isValid(traceId)) {
      LOGGER.warning(
          () ->
              "Found x-cloud-trace-context header with invalid trace: "
                  + traceId
                  + ", header: "
                  + value);
      return context;
    }
    String spanId = SpanId.fromLong(java.lang.Long.parseUnsignedLong(matcher.group("spanid")));
    if (!SpanId.isValid(spanId)) {
      LOGGER.warning(
          () ->
              "Found x-cloud-trace-context header with invalid span: "
                  + spanId
                  + ", header: "
                  + value);
      return context;
    }
    boolean sampled = "1".equals(matcher.group("sampled"));
    SpanContext parent =
        SpanContext.createFromRemoteParent(
            traceId,
            spanId,
            sampled ? TraceFlags.getSampled() : TraceFlags.getDefault(),
            TraceState.getDefault());
    return context.with(Span.wrap(parent));
  }
}
