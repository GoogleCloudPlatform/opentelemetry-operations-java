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
package com.google.cloud.opentelemetry.trace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.trace.v2.TraceServiceClient;
import com.google.cloud.trace.v2.TraceServiceSettings;
import com.google.cloud.trace.v2.stub.TraceServiceStub;
import com.google.devtools.cloudtrace.v2.ProjectName;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.Collections;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TraceExporterTest {

  private static final String PROJECT_ID = "test-id";
  @Mock private TraceServiceClient mockedTraceServiceClient;
  @Mock private TraceServiceStub mockedTraceServiceStub;

  @After
  public void tearDown() {
    GlobalOpenTelemetry.resetForTest();
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void verifyExporterWorksWithConfiguration() {
    try (MockedStatic<TraceServiceClient> mockedTraceServiceClient =
        Mockito.mockStatic(TraceServiceClient.class)) {
      mockedTraceServiceClient
          .when(() -> TraceServiceClient.create(Mockito.eq(mockedTraceServiceStub)))
          .thenReturn(this.mockedTraceServiceClient);

      TraceConfiguration configuration =
          TraceConfiguration.builder()
              .setTraceServiceStub(mockedTraceServiceStub)
              .setProjectId(PROJECT_ID)
              .build();
      SpanExporter exporter = TraceExporter.createWithConfiguration(configuration);
      assertNotNull(exporter);
      generateOpenTelemetryUsingTraceExporter(exporter);
      simulateExport(exporter);

      mockedTraceServiceClient.verify(
          () -> TraceServiceClient.create(Mockito.eq(mockedTraceServiceStub)));
      Mockito.verify(this.mockedTraceServiceClient)
          .batchWriteSpans((ProjectName) Mockito.any(), Mockito.anyList());
    }
  }

  @Test
  public void verifyExporterWorksWithDefaultConfiguration() {
    try (MockedStatic<TraceServiceClient> mockedTraceServiceClient =
            Mockito.mockStatic(TraceServiceClient.class);
        MockedStatic<ServiceOptions> mockedServiceOptions =
            Mockito.mockStatic(ServiceOptions.class);
        MockedStatic<GoogleCredentials> mockedGoogleCredentials =
            Mockito.mockStatic(GoogleCredentials.class)) {
      mockedServiceOptions.when(ServiceOptions::getDefaultProjectId).thenReturn(PROJECT_ID);
      mockedGoogleCredentials
          .when(GoogleCredentials::getApplicationDefault)
          .thenReturn(Mockito.mock(GoogleCredentials.class));
      mockedTraceServiceClient
          .when(() -> TraceServiceClient.create(Mockito.any(TraceServiceSettings.class)))
          .thenReturn(this.mockedTraceServiceClient);

      SpanExporter exporter = TraceExporter.createWithDefaultConfiguration();
      assertNotNull(exporter);
      generateOpenTelemetryUsingTraceExporter(exporter);
      simulateExport(exporter);

      mockedTraceServiceClient.verify(
          () -> TraceServiceClient.create((TraceServiceSettings) Mockito.any()), Mockito.times(1));
      mockedServiceOptions.verify(ServiceOptions::getDefaultProjectId, Mockito.times(1));
      Mockito.verify(this.mockedTraceServiceClient)
          .batchWriteSpans((ProjectName) Mockito.any(), Mockito.anyList());
    }
  }

  @Test
  public void verifyExporterCreationErrorDoesNotBreakTraceExporter() {
    try (MockedStatic<InternalTraceExporter> mockedInternalTraceExporter =
        Mockito.mockStatic(InternalTraceExporter.class)) {
      mockedInternalTraceExporter
          .when(() -> InternalTraceExporter.createWithConfiguration(Mockito.any()))
          .thenThrow(IOException.class);

      SpanExporter traceExporter = TraceExporter.createWithDefaultConfiguration();
      assertNotNull(traceExporter);

      SpanData mockSpanData = Mockito.mock(SpanData.class);
      // verify trace exporter still works without any additional exceptions
      assertEquals(
          CompletableResultCode.ofSuccess(),
          traceExporter.export(Collections.singleton(mockSpanData)));
      assertEquals(CompletableResultCode.ofSuccess(), traceExporter.flush());
      assertEquals(CompletableResultCode.ofSuccess(), traceExporter.shutdown());
    }
  }

  private void generateOpenTelemetryUsingTraceExporter(SpanExporter traceExporter) {
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build())
            .build();

    OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal();
  }

  private void simulateExport(SpanExporter exporter) {
    exporter.export(Collections.emptyList());
  }
}
