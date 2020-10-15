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
import com.google.rpc.Status;
import io.opentelemetry.common.AttributeConsumer;
import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.SpanData.Event;
import io.opentelemetry.trace.Span.Kind;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class TraceTranslator {

  // TODO(nilebox): Extract the constant
  private static final String OPEN_TELEMETRY_LIBRARY_VERSION = "0.6.0";
  private static final String EXPORTER_VERSION = "0.1.0";
  private static final String AGENT_LABEL_KEY = "g.co/agent";
  private static final String AGENT_LABEL_VALUE_STRING =
      "opentelemetry-java " + OPEN_TELEMETRY_LIBRARY_VERSION +
              "; google-cloud-trace-exporter " + EXPORTER_VERSION;
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
    SpanData.Status status = spanData.getStatus();
    if (status != null) {
      spanBuilder.setStatus(toStatusProto(status));
    }
    long end = spanData.getEndEpochNanos();
    if (end != 0) {
      spanBuilder.setEndTime(toTimestampProto(end));
    }
    spanBuilder.setLinks(toLinksProto(spanData.getLinks(), spanData.getTotalRecordedLinks()));
    if (spanData.getParentSpanId() != null) {
      spanBuilder.setParentSpanId(spanData.getParentSpanId());
    }
    boolean hasRemoteParent = spanData.getHasRemoteParent();
    spanBuilder.setSameProcessAsParentSpan(BoolValue.of(!hasRemoteParent));
    return spanBuilder.build();
  }

  @VisibleForTesting
  static String toDisplayName(String spanName, @javax.annotation.Nullable Kind spanKind) {
    if (spanKind == Kind.SERVER && !spanName.startsWith(SERVER_PREFIX)) {
      return SERVER_PREFIX + spanName;
    }

    if (spanKind == Kind.CLIENT && !spanName.startsWith(CLIENT_PREFIX)) {
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
      ReadableAttributes attributes, Map<String, AttributeValue> fixedAttributes) {
    Attributes.Builder attributesBuilder = toAttributesBuilderProto(attributes);
    attributesBuilder.putAttributeMap(AGENT_LABEL_KEY, AGENT_LABEL_VALUE);
    for (Map.Entry<String, AttributeValue> entry : fixedAttributes.entrySet()) {
      attributesBuilder.putAttributeMap(entry.getKey(), entry.getValue());
    }
    return attributesBuilder.build();
  }

  private static Attributes toAttributesProto(ReadableAttributes attributes) {
    return toAttributesProto(attributes, ImmutableMap.<String, AttributeValue>of());
  }

  private static Attributes.Builder toAttributesBuilderProto(ReadableAttributes attributes) {
    Attributes.Builder attributesBuilder =
        // TODO (nilebox): Does OpenTelemetry support droppedAttributesCount?
        Attributes.newBuilder().setDroppedAttributesCount(0);
    attributes.forEach(new AttributeConsumer() {
      @Override
      public <T> void consume(AttributeKey<T> key, T value) {
        attributesBuilder.putAttributeMap(mapKey(key), toAttributeValueProto(key, value));
      }
    });
    return attributesBuilder;
  }

  private static <T> AttributeValue toAttributeValueProto(
      io.opentelemetry.common.AttributeKey<T> key, T value) {
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
        builder.setStringValue(
            toTruncatableStringProto(String.valueOf((value))));
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
  static Span.TimeEvents toTimeEventsProto(List<Event> events) {
    Span.TimeEvents.Builder timeEventsBuilder = Span.TimeEvents.newBuilder();

    for (Event event : events) {
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
  static Status toStatusProto(SpanData.Status status) {
    Status.Builder statusBuilder = Status.newBuilder().setCode(status.getCanonicalCode().value());
    if (status.getDescription() != null) {
      statusBuilder.setMessage(status.getDescription());
    }
    return statusBuilder.build();
  }

  @VisibleForTesting
  static Links toLinksProto(
      List<io.opentelemetry.sdk.trace.data.SpanData.Link> links, int totalRecordedLinks) {
    final Links.Builder linksBuilder =
        Links.newBuilder().setDroppedLinksCount(Math.max(0, totalRecordedLinks - links.size()));
    for (io.opentelemetry.sdk.trace.data.SpanData.Link link : links) {
      linksBuilder.addLink(toLinkProto(link));
    }
    return linksBuilder.build();
  }

  private static Link toLinkProto(io.opentelemetry.sdk.trace.data.SpanData.Link link) {
    checkNotNull(link);
    return Link.newBuilder()
        .setTraceId(link.getContext().getTraceIdAsHexString())
        .setSpanId(link.getContext().getSpanIdAsHexString())
        .setType(Link.Type.TYPE_UNSPECIFIED)
        .setAttributes(toAttributesBuilderProto(link.getAttributes()))
        .build();
  }

  @VisibleForTesting
  static Map<String, AttributeValue> getResourceLabels(Map<String, String> resource) {
    if (resource == null) {
      return Collections.emptyMap();
    }
    Map<String, AttributeValue> resourceLabels = new LinkedHashMap<String, AttributeValue>();
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
