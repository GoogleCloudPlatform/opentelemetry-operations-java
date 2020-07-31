package com.google.cloud.opentelemetry.trace;

import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.devtools.cloudtrace.v2.TruncatableString;
import com.google.rpc.Status;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;
import io.opentelemetry.trace.SpanContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(JUnit4.class)
public class TraceTranslatorTest {

  @Test
  public void testToDisplayName(){
    String serverPrefixSpanName = "Recv. mySpanName";
    String clientPrefixSpanName = "Sent. mySpanName";
    String regularSpanName = "regularSpanName";
    Kind serverSpanKind = Kind.SERVER;
    Kind clientSpanKind = Kind.CLIENT;

    assertEquals(serverPrefixSpanName, TraceTranslator.toDisplayName(serverPrefixSpanName, serverSpanKind));
    assertEquals(clientPrefixSpanName, TraceTranslator.toDisplayName(clientPrefixSpanName, clientSpanKind));
    assertEquals("Recv.regularSpanName", TraceTranslator.toDisplayName(regularSpanName, serverSpanKind));
    assertEquals("Sent.regularSpanName", TraceTranslator.toDisplayName(regularSpanName, clientSpanKind));
  }

  @Test
  public void testNullTruncatableStringProto(){
    assertThrows(NullPointerException.class, () -> TraceTranslator.toTruncatableStringProto(null));
  }

  @Test
  public void testToTruncatableStringProto(){
    String truncatableString = "myTruncatableString";
    TruncatableString testTruncatable = TraceTranslator.toTruncatableStringProto(truncatableString);

    assertEquals("myTruncatableString", testTruncatable.getValue());
    assertEquals(0, testTruncatable.getTruncatedByteCount());
  }

  @Test
  public void testToTimestampProto(){
    long epochNanos = TimeUnit.SECONDS.toNanos(3001) + 255;
    com.google.protobuf.Timestamp timestamp = TraceTranslator.toTimestampProto(epochNanos);

    assertEquals(3001, timestamp.getSeconds());
    assertEquals(255, timestamp.getNanos());
  }

  @Test
  public void testToAttributesProto(){
    String stringkey = "myValue";
    boolean boolKey = true;
    long longKey = 100;
    double doubleKey = 3.14;

    ReadableAttributes attributes = Attributes.newBuilder()
            .setAttribute("myKey", stringkey)
            .setAttribute("http.status_code", boolKey)
            .setAttribute("anotherKey", longKey)
            .setAttribute("http.host", doubleKey)
            .build();
    Map<String, AttributeValue> fixedAttributes = new LinkedHashMap<>();
    fixedAttributes.put("fixed", AttributeValue.newBuilder().setStringValue
        (TruncatableString.newBuilder().setValue("attributes").setTruncatedByteCount(0).build()).build());
    fixedAttributes.put("another", AttributeValue.newBuilder().setStringValue
        (TruncatableString.newBuilder().setValue("entry").setTruncatedByteCount(0).build()).build());

    Span.Attributes translatedAttributes = TraceTranslator.toAttributesProto(attributes, fixedAttributes);

    // Because order in a hash map cannot be guaranteed, the test manually checks each entry

    assertTrue(translatedAttributes.containsAttributeMap("myKey"));
    assertEquals(translatedAttributes.getAttributeMapMap().get("myKey").getStringValue().getValue(), "myValue");

    assertTrue(translatedAttributes.containsAttributeMap("/http/host"));
    assertEquals(translatedAttributes.getAttributeMapMap().get("/http/host").getStringValue().getValue(), "3.14");

    assertTrue(translatedAttributes.containsAttributeMap("/http/status_code"));
    assertTrue(translatedAttributes.getAttributeMapMap().get("/http/status_code").getBoolValue());

    assertTrue(translatedAttributes.containsAttributeMap("anotherKey"));
    assertEquals(translatedAttributes.getAttributeMapMap().get("anotherKey").getIntValue(), 100);

    assertTrue(translatedAttributes.containsAttributeMap("fixed"));
    assertEquals(translatedAttributes.getAttributeMapMap().get("fixed").getStringValue().getValue(), "attributes");

    assertTrue(translatedAttributes.containsAttributeMap("another"));
    assertEquals(translatedAttributes.getAttributeMapMap().get("another").getStringValue().getValue(), "entry");

    assertTrue(translatedAttributes.containsAttributeMap("g.co/agent"));
    assertEquals(translatedAttributes.getAttributeMapMap().get("g.co/agent").getStringValue().getValue(),
            "opentelemetry-java 0.6.0; google-cloud-trace-exporter 0.1.0");
  }

  @Test
  public void testToTimeEventsProto(){
    List<SpanData.Event> events = new ArrayList<>();
    SpanData.Event eventOne = new SpanData.Event() {
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
        return Attributes.newBuilder().setAttribute("key", "value").build();
      }
    };
    events.add(eventOne);

    Span.TimeEvents timeEvents = TraceTranslator.toTimeEventsProto(events);
    assertEquals(1, timeEvents.getTimeEventCount());

    Span.TimeEvent timeEvent = timeEvents.getTimeEvent(0);
    Span.TimeEvent.Annotation annotation = timeEvent.getAnnotation();
    assertEquals("eventOne", annotation.getDescription().getValue());

    Span.Attributes attributes = annotation.getAttributes();
    assertEquals(2, attributes.getAttributeMapCount());

    Map<String, AttributeValue> attributeMap = attributes.getAttributeMapMap();
    assertEquals("value", attributeMap.get("key").getStringValue().getValue());
    assertEquals("opentelemetry-java 0.6.0; google-cloud-trace-exporter 0.1.0",
            attributeMap.get("g.co/agent").getStringValue().getValue());
  }

  @Test
  public void testToStatusProto(){
    io.opentelemetry.trace.Status myStatus = io.opentelemetry.trace.Status.OK.withDescription("Status description");
    Status spanStatus = TraceTranslator.toStatusProto(myStatus);

    // The int representation is 0 for canonical code "OK".
    assertEquals(0,spanStatus.getCode());
    assertEquals("Status description", spanStatus.getMessage());
  }
  
  @Test
  public void testToLinksProto(){
    List<SpanData.Link> links = new ArrayList<>();

    TraceId traceIdOne = new TraceId(321, 123);
    SpanId spanIdOne = new SpanId(12345);
    TraceId traceIdTwo = new TraceId(32473, 24893);
    SpanId spanIdTwo = new SpanId(54321);

    SpanData.Link linkOne = createLink(traceIdOne, spanIdOne, TraceFlags.builder().build(),
            TraceState.builder().build(), Attributes.newBuilder().setAttribute("key", "value").build());
    SpanData.Link linkTwo = createLink(traceIdTwo, spanIdTwo, TraceFlags.builder().build(),
            TraceState.builder().build(), Attributes.newBuilder().setAttribute("random string", "another string").build());

    links.add(linkOne);
    links.add(linkTwo);
    Span.Links finalLinks = TraceTranslator.toLinksProto(links, 2);

    assertEquals(2, finalLinks.getLinkCount());
    assertEquals(0, finalLinks.getDroppedLinksCount());

    assertEquals(traceIdOne.toLowerBase16(), finalLinks.getLink(0).getTraceId());
    assertEquals(spanIdOne.toLowerBase16(), finalLinks.getLink(0).getSpanId());
    // The int representation is 0 for (Link.Type.TYPE_UNSPECIFIED), which is given in TraceTranslator.
    assertEquals(0, finalLinks.getLink(0).getTypeValue());
    assertTrue(finalLinks.getLink(0).getAttributes().containsAttributeMap("key"));

    assertEquals(traceIdTwo.toLowerBase16(), finalLinks.getLink(1).getTraceId());
    assertEquals(spanIdTwo.toLowerBase16(), finalLinks.getLink(1).getSpanId());
    assertEquals(0, finalLinks.getLink(1).getTypeValue());
    assertTrue(finalLinks.getLink(1).getAttributes().containsAttributeMap("random string"));
    assertFalse(finalLinks.getLink(1).getAttributes().containsAttributeMap("key"));
  }

  public SpanData.Link createLink(TraceId traceId, SpanId spanId, TraceFlags traceFlags, TraceState traceState, Attributes attributes){
    SpanContext spanContext = SpanContext.create(traceId, spanId, traceFlags, traceState);
    return SpanData.Link.create(spanContext, attributes);
  }

  @Test
  public void testNullResourceLabels(){
    Map<String, String> nullResources = new HashMap<>();
    assertEquals(Collections.emptyMap(), TraceTranslator.getResourceLabels(nullResources));
  }

  @Test
  public void testGetResourceLabels(){
    Map<String, String> resources = new HashMap<>();
    resources.put("testOne", "testTwo");
    resources.put("another", "entry");

    Map<String, AttributeValue> resourceLabels = new LinkedHashMap<>();
    resourceLabels.put("g.co/r/testOne", AttributeValue.newBuilder().setStringValue
            (TruncatableString.newBuilder().setValue("testTwo").setTruncatedByteCount(0).build()).build());
    resourceLabels.put("g.co/r/another", AttributeValue.newBuilder().setStringValue
            (TruncatableString.newBuilder().setValue("entry").setTruncatedByteCount(0).build()).build());

    assertEquals(Collections.unmodifiableMap(resourceLabels), TraceTranslator.getResourceLabels(resources));
  }

}
