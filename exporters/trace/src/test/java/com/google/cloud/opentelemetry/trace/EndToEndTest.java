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

import static org.junit.Assert.assertTrue;

import com.google.devtools.cloudtrace.v2.AttributeValue;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

@RunWith(JUnit4.class)
public class EndToEndTest {

  private static final String PROJECT_ID = "project-id";
  private static final Map<String, AttributeValue> FIXED_ATTRIBUTES = new HashMap<>();
  private static final String TRACE_ID = TraceId.fromLongs(321, 123);
  private static final String SPAN_ID = SpanId.fromLong(12345);
  private static final String PARENT_SPAN_ID = SpanId.fromLong(54321);
  private static final String SPAN_NAME = "MySpanName";
  private static final long START_EPOCH_NANOS = TimeUnit.SECONDS.toNanos(3000) + 200;
  private static final long END_EPOCH_NANOS = TimeUnit.SECONDS.toNanos(3001) + 255;
  private static final StatusData SPAN_DATA_STATUS = StatusData.ok();

  private SpanExporter exporter;

  /** A test-container instance that loads the Cloud-Ops-Mock server container. */
  private static class CloudOperationsMockContainer
      extends GenericContainer<CloudOperationsMockContainer> {
    CloudOperationsMockContainer() {
      super(
          new ImageFromDockerfile()
              .withDockerfileFromBuilder(
                  builder ->
                      builder
                          .from("golang:1.17")
                          .run(
                              "go install github.com/googleinterns/cloud-operations-api-mock/cmd@v2-alpha")
                          .cmd("cmd --address=:8080")
                          .build()));
      this.withExposedPorts(8080).waitingFor(Wait.forLogMessage(".*Listening on.*\\n", 1));
    }

    public String getTraceServiceEndpoint() {
      return getContainerIpAddress() + ":" + getFirstMappedPort();
    }
  }

  @Rule public CloudOperationsMockContainer mockContainer = new CloudOperationsMockContainer();

  @Test
  public void exportMockSpanDataList() throws IOException {
    exporter =
        TraceExporter.createWithConfiguration(
            TraceConfiguration.builder()
                .setTraceServiceEndpoint(mockContainer.getTraceServiceEndpoint())
                .setInsecureEndpoint(true)
                .setFixedAttributes(FIXED_ATTRIBUTES)
                .setProjectId(PROJECT_ID)
                .build());
    Collection<SpanData> spanDataList = new ArrayList<>();

    TestSpanData spanDataOne =
        TestSpanData.builder()
            .setParentSpanContext(
                SpanContext.createFromRemoteParent(
                    TRACE_ID, PARENT_SPAN_ID, TraceFlags.getDefault(), TraceState.getDefault()))
            .setSpanContext(
                SpanContext.create(
                    TRACE_ID, SPAN_ID, TraceFlags.getDefault(), TraceState.getDefault()))
            .setName(SPAN_NAME)
            .setKind(SpanKind.SERVER)
            .setEvents(Collections.emptyList())
            .setStatus(SPAN_DATA_STATUS)
            .setStartEpochNanos(START_EPOCH_NANOS)
            .setEndEpochNanos(END_EPOCH_NANOS)
            .setTotalRecordedLinks(0)
            .setHasEnded(true)
            .build();

    spanDataList.add(spanDataOne);

    // Invokes export();
    assertTrue(exporter.export(spanDataList).isSuccess());
  }

  @Test
  public void exportEmptySpanDataList() throws IOException {
    exporter =
        TraceExporter.createWithConfiguration(
            TraceConfiguration.builder()
                .setTraceServiceEndpoint(mockContainer.getTraceServiceEndpoint())
                .setInsecureEndpoint(true)
                .setFixedAttributes(FIXED_ATTRIBUTES)
                .setProjectId(PROJECT_ID)
                .build());
    Collection<SpanData> spanDataList = new ArrayList<>();

    // Invokes export();
    assertTrue(exporter.export(spanDataList).isSuccess());
  }
}
