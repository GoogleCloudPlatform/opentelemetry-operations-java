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
package com.google.cloud.opentelemetry.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.concurrent.TimeUnit;

public class FakeData {
  private static final long START_EPOCH_NANOS = TimeUnit.SECONDS.toNanos(3000) + 200;
  private static final long END_EPOCH_NANOS = TimeUnit.SECONDS.toNanos(3001) + 255;

  static final String aTraceId = "00000000000000000000000000000001";
  static final String aSpanId = "0000000000000002";
  static final SpanContext aSpanContext =
      SpanContext.create(aTraceId, aSpanId, TraceFlags.getDefault(), TraceState.getDefault());

  static final SpanData aSpanData =
      TestSpanData.builder()
          .setHasEnded(true)
          .setSpanContext(aSpanContext)
          .setName("FakeData.Span")
          .setStatus(StatusData.ok())
          .setStartEpochNanos(START_EPOCH_NANOS)
          .setEndEpochNanos(END_EPOCH_NANOS)
          .setKind(SpanKind.SERVER)
          .setAttributes(Attributes.of(AttributeKey.stringKey("desc"), "fake_span"))
          .build();
}
