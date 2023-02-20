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
package com.google.cloud.opentelemetry.trace;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A noop implementation of a {@link SpanExporter}. This class is implemented as a singleton and
 * return a successful result on all public methods without performing any action.
 */
@ThreadSafe
public class NoopTraceExporter implements SpanExporter {
  private static SpanExporter noopTraceExporter = null;

  private NoopTraceExporter() {
    // prevent explicit public call to default constructor
  }

  static synchronized SpanExporter getNoopTraceExporter() {
    if (Objects.isNull(noopTraceExporter)) {
      noopTraceExporter = new NoopTraceExporter();
    }
    return noopTraceExporter;
  }

  @Override
  public CompletableResultCode export(@Nonnull Collection<SpanData> spans) {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public String toString() {
    return "NoopTraceExporter{}";
  }
}
