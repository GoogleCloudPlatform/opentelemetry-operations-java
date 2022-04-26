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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.devtools.cloudtrace.v2.TruncatableString;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraceTranslatorTest {

  private TraceTranslator translator = new TraceTranslator();

  @Test
  public void testToDisplayName() {
    String serverPrefixSpanName = "Recv. mySpanName";
    String clientPrefixSpanName = "Sent. mySpanName";
    String regularSpanName = "regularSpanName";
    SpanKind serverSpanKind = SpanKind.SERVER;
    SpanKind clientSpanKind = SpanKind.CLIENT;

    assertEquals(
        serverPrefixSpanName, TraceTranslator.toDisplayName(serverPrefixSpanName, serverSpanKind));
    assertEquals(
        clientPrefixSpanName, TraceTranslator.toDisplayName(clientPrefixSpanName, clientSpanKind));
    assertEquals(
        "Recv.regularSpanName", TraceTranslator.toDisplayName(regularSpanName, serverSpanKind));
    assertEquals(
        "Sent.regularSpanName", TraceTranslator.toDisplayName(regularSpanName, clientSpanKind));
  }

  @Test
  public void testInsertResourceAttributes() {
    Map<String, AttributeValue> resourceAttributes = new HashMap<>();
    Resource resource =
        Resource.create(
            Attributes.builder()
                .put(ResourceAttributes.SERVICE_NAME, "my-service-name")
                .put(ResourceAttributes.SERVICE_NAMESPACE, "qa")
                .put(ResourceAttributes.SERVICE_INSTANCE_ID, "23")
                .build());
    TraceTranslator.insertResourceAttributes(resource, resourceAttributes);
    assertTrue(resourceAttributes.containsKey("g.co/r/generic_task/job"));
    assertEquals(
        "my-service-name",
        resourceAttributes.get("g.co/r/generic_task/job").getStringValue().getValue());
    assertTrue(resourceAttributes.containsKey("g.co/r/generic_task/namespace"));
    assertEquals(
        "qa", resourceAttributes.get("g.co/r/generic_task/namespace").getStringValue().getValue());
    assertTrue(resourceAttributes.containsKey("g.co/r/generic_task/task_id"));
    assertEquals(
        "23", resourceAttributes.get("g.co/r/generic_task/task_id").getStringValue().getValue());
    assertTrue(resourceAttributes.containsKey("g.co/r/generic_task/location"));
    assertEquals(
        "global",
        resourceAttributes.get("g.co/r/generic_task/location").getStringValue().getValue());
  }

  @Test
  public void testNullTruncatableStringProto() {
    assertThrows(NullPointerException.class, () -> TraceTranslator.toTruncatableStringProto(null));
  }

  @Test
  public void testToTruncatableStringProto() {
    String truncatableString = "myTruncatableString";
    TruncatableString testTruncatable = TraceTranslator.toTruncatableStringProto(truncatableString);

    assertEquals("myTruncatableString", testTruncatable.getValue());
    assertEquals(0, testTruncatable.getTruncatedByteCount());
  }

  @Test
  public void testToTimestampProto() {
    long epochNanos = TimeUnit.SECONDS.toNanos(3001) + 255;
    com.google.protobuf.Timestamp timestamp = TraceTranslator.toTimestampProto(epochNanos);

    assertEquals(3001, timestamp.getSeconds());
    assertEquals(255, timestamp.getNanos());
  }

  @Test
  public void testToAttributesProtoWithLists() {
    Attributes attributes =
        Attributes.builder()
            .put(AttributeKey.stringArrayKey("names"), Arrays.asList("test"))
            .put(AttributeKey.booleanArrayKey("statuses"), Arrays.asList(true))
            .put(AttributeKey.longArrayKey("counts"), Arrays.asList(1L, 2L))
            .put(AttributeKey.doubleArrayKey("values"), Arrays.asList(1d, 2.5d))
            .build();
    Span.Attributes translatedAttributes =
        translator.toAttributesProto(attributes, Collections.emptyMap());
    assertTrue(translatedAttributes.containsAttributeMap("names"));
    assertEquals(
        translatedAttributes.getAttributeMapMap().get("names").getStringValue().getValue(),
        "[test]");
    assertTrue(translatedAttributes.containsAttributeMap("statuses"));
    assertEquals(
        translatedAttributes.getAttributeMapMap().get("statuses").getStringValue().getValue(),
        "[true]");
    assertTrue(translatedAttributes.containsAttributeMap("counts"));
    assertEquals(
        translatedAttributes.getAttributeMapMap().get("counts").getStringValue().getValue(),
        "[1,2]");
    assertTrue(translatedAttributes.containsAttributeMap("values"));
    assertEquals(
        translatedAttributes.getAttributeMapMap().get("values").getStringValue().getValue(),
        "[1.0,2.5]");
  }

  @Test
  public void testToAttributesProto() {
    String stringkey = "myValue";
    boolean boolKey = true;
    long longKey = 100;
    double doubleKey = 3.14;

    Attributes attributes =
        Attributes.builder()
            .put("myKey", stringkey)
            .put("http.status_code", boolKey)
            .put("anotherKey", longKey)
            .put("http.host", doubleKey)
            .build();
    Map<String, AttributeValue> fixedAttributes = new LinkedHashMap<>();
    fixedAttributes.put(
        "fixed",
        AttributeValue.newBuilder()
            .setStringValue(
                TruncatableString.newBuilder()
                    .setValue("attributes")
                    .setTruncatedByteCount(0)
                    .build())
            .build());
    fixedAttributes.put(
        "another",
        AttributeValue.newBuilder()
            .setStringValue(
                TruncatableString.newBuilder().setValue("entry").setTruncatedByteCount(0).build())
            .build());

    TraceTranslator withFixedAttributes =
        new TraceTranslator(TraceConfiguration.DEFAULT_ATTRIBUTE_MAPPING, fixedAttributes);

    Span.Attributes translatedAttributes =
        withFixedAttributes.toAttributesProto(attributes, fixedAttributes);

    // Because order in a hash map cannot be guaranteed, the test manually checks each entry

    assertTrue(translatedAttributes.containsAttributeMap("myKey"));
    assertEquals(
        translatedAttributes.getAttributeMapMap().get("myKey").getStringValue().getValue(),
        "myValue");

    assertTrue(translatedAttributes.containsAttributeMap("/http/host"));
    assertEquals(
        translatedAttributes.getAttributeMapMap().get("/http/host").getStringValue().getValue(),
        "3.14");

    assertTrue(translatedAttributes.containsAttributeMap("/http/status_code"));
    assertTrue(translatedAttributes.getAttributeMapMap().get("/http/status_code").getBoolValue());

    assertTrue(translatedAttributes.containsAttributeMap("anotherKey"));
    assertEquals(translatedAttributes.getAttributeMapMap().get("anotherKey").getIntValue(), 100);

    assertTrue(translatedAttributes.containsAttributeMap("fixed"));
    assertEquals(
        translatedAttributes.getAttributeMapMap().get("fixed").getStringValue().getValue(),
        "attributes");

    assertTrue(translatedAttributes.containsAttributeMap("another"));
    assertEquals(
        translatedAttributes.getAttributeMapMap().get("another").getStringValue().getValue(),
        "entry");
  }

  @Test
  public void testGenerateSpan() {
    TraceTranslator withDefaultMapping =
        new TraceTranslator(TraceConfiguration.DEFAULT_ATTRIBUTE_MAPPING, Collections.emptyMap());
    String traceId = "00000000000000000000000000000001";
    String spanId = "0000000000000002";
    String projectId = "test-project";
    TestSpanData spanData =
        TestSpanData.builder()
            .setName("test-span")
            .setSpanContext(
                SpanContext.create(
                    traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault()))
            .setStartEpochNanos(1L)
            .setEndEpochNanos(2L)
            .setHasEnded(true)
            .setStatus(StatusData.ok())
            .setKind(SpanKind.SERVER)
            .setAttributes(Attributes.builder().put("conflict", "kept").build())
            .setResource(
                Resource.create(
                    Attributes.builder()
                        .put("test-resource-key", "test-resource-value")
                        .put("conflict", "ignored")
                        .build()))
            .build();
    Span resultSpan = withDefaultMapping.generateSpan(spanData, "test-project");

    // Ensure name is UUID of span
    assertEquals(
        "projects/" + projectId + "/traces/" + traceId + "/spans/" + spanId, resultSpan.getName());
    assertEquals(spanId, resultSpan.getSpanId());

    // Ensure display name is a "server" name.
    assertEquals("Recv.test-span", resultSpan.getDisplayName().getValue());

    // Ensure status is "ok"
    assertEquals(Code.OK.getNumber(), resultSpan.getStatus().getCode());

    // Ensure start/stop times
    assertEquals(1, resultSpan.getStartTime().getNanos());
    assertEquals(2, resultSpan.getEndTime().getNanos());

    Span.Attributes translatedAttributes = resultSpan.getAttributes();
    // Make sure agent gets added to attributes.
    assertTrue(translatedAttributes.containsAttributeMap("g.co/agent"));
    assertEquals(
        translatedAttributes.getAttributeMapMap().get("g.co/agent").getStringValue().getValue(),
        String.format(
            "opentelemetry-java %s; google-cloud-trace-exporter %s",
            TraceVersions.SDK_VERSION, TraceVersions.EXPORTER_VERSION));
    // Make sure instrumentation library is added to attributes.
    assertTrue(translatedAttributes.containsAttributeMap("otel.scope.name"));
    assertEquals(
        "",
        translatedAttributes
            .getAttributeMapMap()
            .get("otel.scope.name")
            .getStringValue()
            .getValue());

    // Make sure resource attributes are copied over.
    assertTrue(translatedAttributes.containsAttributeMap("test-resource-key"));
    assertEquals(
        "test-resource-value",
        translatedAttributes
            .getAttributeMapMap()
            .get("test-resource-key")
            .getStringValue()
            .getValue());

    // Make sure resource conflicting attributes are NOT overwriting span attribtues.
    assertEquals(
        "kept",
        translatedAttributes.getAttributeMapMap().get("conflict").getStringValue().getValue());
  }

  @Test
  public void testToTimeEventsProto() {
    List<EventData> events = new ArrayList<>();
    EventData eventOne =
        new EventData() {
          // The SpanData.Event interfaces requires us to override these four methods
          @Override
          public long getEpochNanos() {
            return 0;
          }

          @Override
          public int getTotalAttributeCount() {
            return 1;
          }

          @Override
          public String getName() {
            return "eventOne";
          }

          @Override
          public Attributes getAttributes() {
            return Attributes.builder().put("key", "value").build();
          }
        };
    events.add(eventOne);

    Span.TimeEvents timeEvents = translator.toTimeEventsProto(events);
    assertEquals(1, timeEvents.getTimeEventCount());

    Span.TimeEvent timeEvent = timeEvents.getTimeEvent(0);
    Span.TimeEvent.Annotation annotation = timeEvent.getAnnotation();
    assertEquals("eventOne", annotation.getDescription().getValue());

    Span.Attributes attributes = annotation.getAttributes();
    assertEquals(1, attributes.getAttributeMapCount());

    Map<String, AttributeValue> attributeMap = attributes.getAttributeMapMap();
    assertEquals("value", attributeMap.get("key").getStringValue().getValue());
  }

  @Test
  public void testToStatusProto() {
    Status spanStatus = TraceTranslator.toStatusProto(StatusData.ok());

    // The int representation is 0 for canonical code "OK".
    assertEquals(Code.OK.getNumber(), spanStatus.getCode());

    Status failStatus = TraceTranslator.toStatusProto(StatusData.create(StatusCode.ERROR, "FAIL!"));
    assertEquals(Code.UNKNOWN.getNumber(), failStatus.getCode());
    assertEquals("FAIL!", failStatus.getMessage());

    Status unsetStatus = TraceTranslator.toStatusProto(StatusData.unset());
    assertEquals("Unset status should not create protos", null, unsetStatus);
  }

  @Test
  public void testNullResourceLabels() {
    Map<String, String> nullResources = new HashMap<>();
    assertEquals(Collections.emptyMap(), TraceTranslator.getResourceLabels(nullResources));
  }

  @Test
  public void testGetResourceLabels() {
    Map<String, String> resources = new HashMap<>();
    resources.put("testOne", "testTwo");
    resources.put("another", "entry");

    Map<String, AttributeValue> resourceLabels = new LinkedHashMap<>();
    resourceLabels.put(
        "g.co/r/testOne",
        AttributeValue.newBuilder()
            .setStringValue(
                TruncatableString.newBuilder().setValue("testTwo").setTruncatedByteCount(0).build())
            .build());
    resourceLabels.put(
        "g.co/r/another",
        AttributeValue.newBuilder()
            .setStringValue(
                TruncatableString.newBuilder().setValue("entry").setTruncatedByteCount(0).build())
            .build());

    assertEquals(
        Collections.unmodifiableMap(resourceLabels), TraceTranslator.getResourceLabels(resources));
  }
}
