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

import com.google.cloud.ServiceOptions;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.ThrottlingLogger;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class TraceExporter implements SpanExporter {

  private static final Logger logger = Logger.getLogger(TraceExporter.class.getName());

  private final TraceConfiguration.Builder customTraceConfigurationBuilder;
  private final AtomicReference<SpanExporter> internalTraceExporter;
  private final ThrottlingLogger throttlingLogger;

  private TraceExporter(TraceConfiguration.Builder configurationBuilder) {
    this.customTraceConfigurationBuilder = configurationBuilder;
    this.internalTraceExporter = new AtomicReference<>(null);
    this.throttlingLogger = new ThrottlingLogger(logger);
  }

  private static SpanExporter generateStubTraceExporter(TraceConfiguration.Builder configBuilder) {
    return new TraceExporter(configBuilder);
  }

  private SpanExporter createActualTraceExporter() throws IOException {
    return InternalTraceExporter.createWithConfiguration(
        this.customTraceConfigurationBuilder.build());
  }

  /**
   * Method that generates an instance of {@link TraceExporter} using a minimally configured {@link
   * TraceConfiguration} object that requires no input from the user. Since no project ID is
   * specified, default project ID is used instead. See {@link ServiceOptions#getDefaultProjectId()}
   * for details.
   *
   * <p>This method defers the creation of an actual {@link TraceExporter} to a point when it is
   * actually needed - which is when the spans need to be exported. As a result, while this method
   * does not throw any exception, an exception may still be thrown during the attempt to generate
   * the actual {@link TraceExporter}.
   *
   * @return An instance of {@link TraceExporter} as a {@link SpanExporter} object.
   */
  public static SpanExporter createWithDefaultConfiguration() {
    return generateStubTraceExporter(TraceConfiguration.builder());
  }

  /**
   * Method that generates an instance of {@link TraceExporter} using a {@link
   * TraceConfiguration.Builder} that allows the user to provide preferences.
   *
   * <p>This method defers the creation of an actual {@link TraceExporter} to a point when it is
   * actually needed - which is when the spans need to be exported. As a result, while this method
   * does not throw any exception, an exception may still be thrown during the attempt to generate
   * the actual {@link TraceExporter}.
   *
   * @param configBuilder The {@link TraceConfiguration.Builder} object containing user preferences
   *     for Trace.
   * @return An instance of {@link TraceExporter} as a {@link SpanExporter} object.
   */
  public static SpanExporter createWithConfiguration(TraceConfiguration.Builder configBuilder) {
    return generateStubTraceExporter(configBuilder);
  }

  @Deprecated
  public static SpanExporter createWithConfiguration(TraceConfiguration configuration)
      throws IOException {
    return InternalTraceExporter.createWithConfiguration(configuration);
  }

  @Override
  public CompletableResultCode flush() {
    if (internalTraceExporter.get() == null) {
      return CompletableResultCode.ofFailure();
    }
    return internalTraceExporter.get().flush();
  }

  @Override
  public CompletableResultCode export(@Nonnull Collection<SpanData> spanDataList) {
    if (Objects.isNull(internalTraceExporter.get())) {
      synchronized (this) {
        try {
          internalTraceExporter.compareAndSet(null, createActualTraceExporter());
        } catch (IOException e) {
          // unable to create actual trace exporter
          throttlingLogger.log(
              Level.WARNING,
              String.format("Unable to initialize trace exporter. Error %s", e.getMessage()));
          throttlingLogger.log(Level.INFO, "Setting TraceExporter to noop.");
          internalTraceExporter.set(InternalTraceExporter.noop());
        }
      }
    }
    return internalTraceExporter.get().export(spanDataList);
  }

  @Override
  public CompletableResultCode shutdown() {
    if (internalTraceExporter.get() != null) {
      return internalTraceExporter.get().shutdown();
    } else {
      return CompletableResultCode.ofFailure();
    }
  }
}
