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

import static org.junit.Assert.assertNotNull;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraceExporterTest {

  private static final String PROJECT_ID = "test";

  @After
  public void tearDown() {
    GlobalOpenTelemetry.resetForTest();
  }

  @Test
  public void createWithBuiltConfiguration() {
    TraceConfiguration configuration =
        TraceConfiguration.builder().setProjectId(PROJECT_ID).build();
    try {
      SpanExporter exporter = TraceExporter.createWithConfiguration(configuration);
      assertNotNull(exporter);
      generateOpenTelemetryUsingTraceExporter(exporter);
    } catch (IOException ignored) {
    }
  }

  /**
   * This type of usage is expected to throw an {@link IllegalStateException} if client code has a
   * dependency on a library that initializes OpenTelemetry (in this case, opencensus-shim). The
   * test passes {@link TraceConfiguration} as a pre-built object to the method - {@link
   * TraceExporter#createWithConfiguration(TraceConfiguration)}. While building the
   * TraceConfiguration, internal calls are made via the GAX library, which uses opencensus, thereby
   * initializing OpenTelemetry and so it cannot be done again in client code (this test).
   *
   * @see <a
   *     href="https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/issues/141">Issue
   *     details</a>
   */
  @Test(expected = IllegalStateException.class)
  public void createWithBuiltConfigurationDefaultProjectId() {
    TraceConfiguration configuration = TraceConfiguration.builder().build();
    try {
      SpanExporter exporter = TraceExporter.createWithConfiguration(configuration);
      assertNotNull(exporter);
      generateOpenTelemetryUsingTraceExporter(exporter);
    } catch (IOException ignored) {
    }
  }

  @Test
  public void createWithConfigurationBuilder() {
    SpanExporter exporter =
        TraceExporter.createWithConfiguration(
            TraceConfiguration.builder().setProjectId(PROJECT_ID));
    assertNotNull(exporter);
    generateOpenTelemetryUsingTraceExporter(exporter);
  }

  @Test
  public void createWithConfigurationBuilderDefaultProjectId() {
    SpanExporter exporter = TraceExporter.createWithDefaultConfiguration();
    assertNotNull(exporter);
    generateOpenTelemetryUsingTraceExporter(exporter);
  }

  private void generateOpenTelemetryUsingTraceExporter(SpanExporter traceExporter) {
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build())
            .build();

    OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal();
  }
}
