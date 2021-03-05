/*
 * Copyright 2021 Google
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.devtools.cloudtrace.v2.Span.Attributes;
import com.google.devtools.cloudtrace.v2.Span.Link;
import com.google.devtools.cloudtrace.v2.Span.Links;
import com.google.devtools.cloudtrace.v2.SpanName;
import com.google.devtools.cloudtrace.v2.TruncatableString;
import com.google.protobuf.BoolValue;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

class TraceTranslator {
  private static final String AGENT_LABEL_KEY = "g.co/agent";
  private static final String AGENT_LABEL_VALUE_STRING =
      "opentelemetry-java "
          + TraceVersions.SDK_VERSION
          + "; google-cloud-trace-exporter "
          + TraceVersions.EXPORTER_VERSION;
  private static final AttributeValue AGENT_LABEL_VALUE =
      AttributeValue.newBuilder()
          .setStringValue(toTruncatableStringProto(AGENT_LABEL_VALUE_STRING))
          .build();
  private static final String SERVER_PREFIX = "Recv.";
  private static final String CLIENT_PREFIX = "Sent.";

  private static final ImmutableMap<String, String> HTTP_ATTRIBUTE_MAPPING =
      ImmutableMap.<String, String>builder()
          .put("http.host", "/http/host")
          .put("http.method", "/http/method")
          .put("http.path", "/http/path")
          .put("http.route", "/http/route")
          .put("http.user_agent", "/http/user_agent")
          .put("http.status_code", "/http/status_code")
          .build();

  @VisibleForTesting
  static Span generateSpan(
      SpanData spanData, String projectId, Map<String, AttributeValue> constAttributes) {
    final String traceId = spanData.getTraceId();
    final String spanId = spanData.getSpanId();
    SpanName spanName =
        SpanName.newBuilder().setProject(projectId).setTrace(traceId).setSpan(spanId).build();
    Span.Builder spanBuilder =
        Span.newBuilder()
            .setName(spanName.toString())
            .setSpanId(spanId)
            .setDisplayName(
                toTruncatableStringProto(toDisplayName(spanData.getName(), spanData.getKind())))
            .setStartTime(toTimestampProto(spanData.getStartEpochNanos()))
            .setAttributes(toAttributesProto(spanData.getAttributes(), constAttributes))
            .setTimeEvents(toTimeEventsProto(spanData.getEvents()));
    StatusData status = spanData.getStatus();
    if (status != null) {
      Status statusProto = toStatusProto(status);
      if (statusProto != null) {
        spanBuilder.setStatus(statusProto);
      }
    }
    long end = spanData.getEndEpochNanos();
    if (end != 0) {
      spanBuilder.setEndTime(toTimestampProto(end));
    }
    spanBuilder.setLinks(toLinksProto(spanData.getLinks(), spanData.getTotalRecordedLinks()));
    if (spanData.getParentSpanId() != null) {
      spanBuilder.setParentSpanId(spanData.getParentSpanId());
    }
    boolean hasRemoteParent = spanData.getParentSpanContext().isRemote();
    spanBuilder.setSameProcessAsParentSpan(BoolValue.of(!hasRemoteParent));
    return spanBuilder.build();
  }

  @VisibleForTesting
  static String toDisplayName(String spanName, @javax.annotation.Nullable SpanKind spanKind) {
    if (spanKind == SpanKind.SERVER && !spanName.startsWith(SERVER_PREFIX)) {
      return SERVER_PREFIX + spanName;
    }

    if (spanKind == SpanKind.CLIENT && !spanName.startsWith(CLIENT_PREFIX)) {
      return CLIENT_PREFIX + spanName;
    }

    return spanName;
  }

  @VisibleForTesting
  static TruncatableString toTruncatableStringProto(String string) {
    return TruncatableString.newBuilder().setValue(string).setTruncatedByteCount(0).build();
  }

  @VisibleForTesting
  static com.google.protobuf.Timestamp toTimestampProto(long epochNanos) {
    long seconds = TimeUnit.NANOSECONDS.toSeconds(epochNanos);
    int nanos = (int) (epochNanos - TimeUnit.SECONDS.toNanos(seconds));

    return com.google.protobuf.Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos).build();
  }

  // These are the attributes of the Span, where usually we may add more
  // attributes like the agent.
  @VisibleForTesting
  static Attributes toAttributesProto(
      io.opentelemetry.api.common.Attributes attributes,
      Map<String, AttributeValue> fixedAttributes) {
    Attributes.Builder attributesBuilder = toAttributesBuilderProto(attributes);
    // TODO(jsuereth): pull instrumentation library/version from SpanData and add as attribute.
    attributesBuilder.putAttributeMap(AGENT_LABEL_KEY, AGENT_LABEL_VALUE);
    for (Map.Entry<String, AttributeValue> entry : fixedAttributes.entrySet()) {
      attributesBuilder.putAttributeMap(entry.getKey(), entry.getValue());
    }
    return attributesBuilder.build();
  }

  private static Attributes toAttributesProto(io.opentelemetry.api.common.Attributes attributes) {
    return toAttributesProto(attributes, ImmutableMap.of());
  }

  private static Attributes.Builder toAttributesBuilderProto(
      io.opentelemetry.api.common.Attributes attributes) {
    Attributes.Builder attributesBuilder =
        // TODO (nilebox): Does OpenTelemetry support droppedAttributesCount?
        Attributes.newBuilder().setDroppedAttributesCount(0);
    attributes.forEach(
        new BiConsumer<AttributeKey<?>, Object>() {
          @Override
          public void accept(AttributeKey<?> key, Object value) {
            attributesBuilder.putAttributeMap(mapKey(key), toAttributeValueProto(key, value));
          }
        });
    return attributesBuilder;
  }

  private static <T> AttributeValue toAttributeValueProto(AttributeKey<?> key, Object value) {
    AttributeValue.Builder builder = AttributeValue.newBuilder();
    switch (key.getType()) {
      case STRING:
        builder.setStringValue(toTruncatableStringProto((String) value));
        break;
      case BOOLEAN:
        builder.setBoolValue((Boolean) value);
        break;
      case LONG:
        builder.setIntValue((Long) value);
        break;
      case DOUBLE:
        builder.setStringValue(toTruncatableStringProto(String.valueOf((value))));
        break;
    }
    return builder.build();
  }

  private static <T> String mapKey(AttributeKey<T> key) {
    if (HTTP_ATTRIBUTE_MAPPING.containsKey(key.getKey())) {
      return HTTP_ATTRIBUTE_MAPPING.get(key.getKey());
    } else {
      return key.getKey();
    }
  }

  @VisibleForTesting
  static Span.TimeEvents toTimeEventsProto(List<EventData> events) {
    Span.TimeEvents.Builder timeEventsBuilder = Span.TimeEvents.newBuilder();

    for (EventData event : events) {
      timeEventsBuilder.addTimeEvent(
          Span.TimeEvent.newBuilder()
              .setTime(toTimestampProto(event.getEpochNanos()))
              .setAnnotation(
                  Span.TimeEvent.Annotation.newBuilder()
                      .setDescription(toTruncatableStringProto(event.getName()))
                      .setAttributes(toAttributesProto(event.getAttributes()))));
    }

    return timeEventsBuilder.build();
  }

  @VisibleForTesting
  static Status toStatusProto(StatusData status) {

    final Status.Builder statusBuilder = Status.newBuilder();

    final StatusCode statusCode = status.getStatusCode();
    switch (statusCode) {
      case OK:
        statusBuilder.setCode(Code.OK.getNumber());
        break;
      case UNSET:
        // We do not specify a code in the UNSET case.
        return null;
      case ERROR:
        statusBuilder.setCode(2);
        // Only set the status description if an error.
        if (status.getDescription() != null) {
          statusBuilder.setMessage(status.getDescription());
        }
        break;
      default:
        // Handle new/unknown codes as unknown
        statusBuilder.setCode(Code.UNKNOWN.getNumber());
        break;
    }
    return statusBuilder.build();
  }

  @VisibleForTesting
  static Links toLinksProto(List<LinkData> links, int totalRecordedLinks) {
    final Links.Builder linksBuilder =
        Links.newBuilder().setDroppedLinksCount(Math.max(0, totalRecordedLinks - links.size()));
    for (LinkData link : links) {
      linksBuilder.addLink(toLinkProto(link));
    }
    return linksBuilder.build();
  }

  private static Link toLinkProto(LinkData link) {
    checkNotNull(link);
    return Link.newBuilder()
        .setTraceId(link.getSpanContext().getTraceId())
        .setSpanId(link.getSpanContext().getSpanId())
        .setType(Link.Type.TYPE_UNSPECIFIED)
        .setAttributes(toAttributesBuilderProto(link.getAttributes()))
        .build();
  }

  @VisibleForTesting
  static Map<String, AttributeValue> getResourceLabels(Map<String, String> resource) {
    if (resource == null) {
      return Collections.emptyMap();
    }
    Map<String, AttributeValue> resourceLabels = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : resource.entrySet()) {
      putToResourceAttributeMap(resourceLabels, entry.getKey(), entry.getValue());
    }
    return Collections.unmodifiableMap(resourceLabels);
  }

  private static void putToResourceAttributeMap(
      Map<String, AttributeValue> map, String attributeName, String attributeValue) {
    map.put(createResourceLabelKey(attributeName), toStringAttributeValueProto(attributeValue));
  }

  @VisibleForTesting
  static String createResourceLabelKey(String resourceAttribute) {
    return "g.co/r/" + resourceAttribute;
  }

  @VisibleForTesting
  static AttributeValue toStringAttributeValueProto(String value) {
    return AttributeValue.newBuilder().setStringValue(toTruncatableStringProto(value)).build();
  }

  private TraceTranslator() {}
}
