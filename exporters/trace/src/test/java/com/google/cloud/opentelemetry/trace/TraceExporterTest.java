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

import static com.google.cloud.opentelemetry.trace.FakeData.aSpanData;
import static com.google.cloud.opentelemetry.trace.TraceConfiguration.DEFAULT_ATTRIBUTE_MAPPING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.trace.v2.TraceServiceClient;
import com.google.cloud.trace.v2.TraceServiceSettings;
import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.ProjectName;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.devtools.cloudtrace.v2.TruncatableString;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class TraceExporterTest {

  private static final String PROJECT_ID = "test-id";
  private static final String FAKE_ENDPOINT = "fake.endpoint.com:443";
  private static final GoogleCredentials FAKE_CREDENTIAL =
      GoogleCredentials.newBuilder().setAccessToken(new AccessToken("fake", new Date(100))).build();

  @Mock private TraceServiceClient mockedTraceServiceClient;
  @Mock private CloudTraceClient mockCloudTraceClient;

  @Captor private ArgumentCaptor<List<Span>> spanListCaptor;

  @After
  public void tearDown() {
    GlobalOpenTelemetry.resetForTest();
  }

  @Test
  public void testCreateWithConfigurationSucceeds() {
    TraceConfiguration configuration =
        TraceConfiguration.builder()
            .setCredentials(FAKE_CREDENTIAL)
            .setProjectId(PROJECT_ID)
            .setTraceServiceEndpoint(FAKE_ENDPOINT)
            .build();
    SpanExporter exporter = TraceExporter.createWithConfiguration(configuration);
    assertNotNull(exporter);
  }

  @Test
  public void testExportSucceeds() {
    Map<String, AttributeValue> fixedSpanAttributes =
        Collections.singletonMap(
            "test_span", AttributeValue.newBuilder().setBoolValue(true).build());
    SpanExporter exporter =
        InternalTraceExporter.createWithClient(
            PROJECT_ID, mockCloudTraceClient, DEFAULT_ATTRIBUTE_MAPPING, fixedSpanAttributes);

    // export fake span data
    CompletableResultCode result = exporter.export(ImmutableList.of(aSpanData));
    Mockito.verify(mockCloudTraceClient, Mockito.times(1))
        .batchWriteSpans(Mockito.eq(ProjectName.of(PROJECT_ID)), spanListCaptor.capture());

    assertTrue(result.isSuccess());
    // check if the spans being exported have the expected span attributes
    spanListCaptor
        .getValue()
        .forEach(
            span -> {
              assertEquals(aSpanData.getSpanId(), span.getSpanId());
              // Check if agent label key is added as a span attribute
              assertTrue(
                  span.getAttributes()
                      .getAttributeMapMap()
                      .get("g.co/agent")
                      .getStringValue()
                      .toString()
                      .contains("google-cloud-trace-exporter"));
              assertEquals(
                  span.getAttributes().getAttributeMapMap().get("test_span"),
                  AttributeValue.newBuilder().setBoolValue(true).build());
              assertEquals(
                  span.getAttributes().getAttributeMapMap().get("desc"),
                  AttributeValue.newBuilder()
                      .setStringValue(TruncatableString.newBuilder().setValue("fake_span").build())
                      .build());
            });
  }

  @Test
  public void verifyExporterWorksWithConfiguration() {
    try (MockedStatic<TraceServiceClient> mockedTraceServiceClient =
        Mockito.mockStatic(TraceServiceClient.class)) {
      mockedTraceServiceClient
          .when(() -> TraceServiceClient.create(Mockito.any(TraceServiceSettings.class)))
          .thenReturn(this.mockedTraceServiceClient);

      TraceConfiguration configuration =
          TraceConfiguration.builder()
              .setCredentials(FAKE_CREDENTIAL)
              .setTraceServiceEndpoint(FAKE_ENDPOINT)
              .setProjectId(PROJECT_ID)
              .build();
      SpanExporter exporter = TraceExporter.createWithConfiguration(configuration);
      assertNotNull(exporter);
      generateOpenTelemetryUsingTraceExporter(exporter);
      simulateExport(exporter);

      mockedTraceServiceClient.verify(
          Mockito.times(1),
          () -> TraceServiceClient.create(Mockito.any(TraceServiceSettings.class)));
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
          Mockito.times(1), () -> TraceServiceClient.create((TraceServiceSettings) Mockito.any()));
      mockedServiceOptions.verify(Mockito.times(1), ServiceOptions::getDefaultProjectId);
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
