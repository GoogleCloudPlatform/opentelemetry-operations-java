/*
 * Copyright 2022 Google
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
package com.google.example;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import com.google.cloud.ServiceOptions;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;

/** This is a "filter" for logback that simple appends opencensus trace context to the MDC. */
public class CloudTraceLogAttacher extends Filter<ILoggingEvent> {
  private static final String TRACE_ID = "gcp.trace_id";
  private static final String SPAN_ID = "gcp.span_id";
  private static final String SAMPLED = "gcp.trace_sampled";

  private final String projectId;
  private final String tracePrefix;

  public CloudTraceLogAttacher() {
    this.projectId = lookUpProjectId();
    this.tracePrefix = "projects/" + (projectId == null ? "" : projectId) + "/traces/";
  }

  @Override
  public ch.qos.logback.core.spi.FilterReply decide(
      ch.qos.logback.classic.spi.ILoggingEvent event) {
    SpanContext context = Span.fromContext(Context.current()).getSpanContext();
    if (context.isValid()) {
      org.slf4j.MDC.put(TRACE_ID, tracePrefix + context.getTraceId());
      org.slf4j.MDC.put(SPAN_ID, context.getSpanId());
      org.slf4j.MDC.put(SAMPLED, Boolean.toString(context.isSampled()));
    }
    return ch.qos.logback.core.spi.FilterReply.ACCEPT;
  }

  private static String lookUpProjectId() {
    return ServiceOptions.getDefaultProjectId();
  }
}
