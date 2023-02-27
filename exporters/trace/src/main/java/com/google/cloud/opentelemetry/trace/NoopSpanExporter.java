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
package com.google.cloud.opentelemetry.trace;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import javax.annotation.Nonnull;

/** A noop implementation of a {@link SpanExporter}. */
final class NoopSpanExporter implements SpanExporter {

  NoopSpanExporter() {
    // prevent explicit public call to default constructor
  }

  /**
   * Noop implementation for exporting spans.
   *
   * @param spans The {@link Collection} of {@link SpanData} that need to be exported.
   * @return a failure result code indicated via {@link CompletableResultCode#ofFailure()}.
   */
  @Override
  public CompletableResultCode export(@Nonnull Collection<SpanData> spans) {
    return CompletableResultCode.ofFailure();
  }

  /**
   * Noop implementation for flushing current spans.
   *
   * @return a success result code indicated via {@link CompletableResultCode#ofSuccess()}.
   */
  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  /**
   * Noop implementation for shutting down the current exporter.
   *
   * @return a success result code indicated via {@link CompletableResultCode#ofSuccess()}.
   */
  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }
}
