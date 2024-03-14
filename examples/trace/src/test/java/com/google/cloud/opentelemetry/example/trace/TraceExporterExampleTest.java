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
package com.google.cloud.opentelemetry.example.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TraceExporterExampleTest {

  private OpenTelemetrySdk testOTelSdk;
  private InMemorySpanExporter testInMemorySpanExporter;

  @BeforeEach
  public void setupOTelSdk() {
    testInMemorySpanExporter = InMemorySpanExporter.create();
    SdkTracerProvider testTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(testInMemorySpanExporter))
            .build();

    testOTelSdk = OpenTelemetrySdk.builder().setTracerProvider(testTracerProvider).build();
  }

  @AfterEach
  public void cleanupOTelSdk() {
    testOTelSdk.close();
  }

  @Test
  public void testMyUseCase() {
    TraceExporterExample.myUseCase(testOTelSdk, "test");
    List<SpanData> spanItems = testInMemorySpanExporter.getFinishedSpanItems();
    assertFalse(spanItems.isEmpty());

    // assert on custom span attributes set by application
    spanItems.stream()
        .filter(spanData -> spanData.getName().equals("test"))
        .map(SpanData::getAttributes)
        .forEach(
            spanAttributes -> {
              assertEquals(true, spanAttributes.get(AttributeKey.booleanKey("test_span")));
              assertEquals(3, spanAttributes.get(AttributeKey.longKey("work_loop")));
            });
  }
}
