package com.google.cloud.opentelemetry.trace;

import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.devtools.cloudtrace.v2.TruncatableString;
import com.google.rpc.Status;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TraceTranslatorTest {

  @Test
  public void toDisplayNameTest(){
    String serverPrefixSpanName = "Recv. mySpanName";
    String clientPrefixSpanName = "Sent. mySpanName";
    String regularSpanName = "regularSpanName";
    Kind serverSpanKind = Kind.SERVER;
    Kind clientSpanKind = Kind.CLIENT;

    assertEquals("Recv. mySpanName", TraceTranslator.toDisplayName(serverPrefixSpanName, serverSpanKind));
    assertEquals("Sent. mySpanName", TraceTranslator.toDisplayName(clientPrefixSpanName, clientSpanKind));
    assertEquals("Recv.regularSpanName", TraceTranslator.toDisplayName(regularSpanName, serverSpanKind));
    assertEquals("Sent.regularSpanName", TraceTranslator.toDisplayName(regularSpanName, clientSpanKind));
  }

  @Test(expected = NullPointerException.class)
  public void nullTruncatableStringProtoTest(){
    TruncatableString nullTruncatable = TraceTranslator.toTruncatableStringProto(null);

    assertNull(nullTruncatable.getValue());
    assertEquals(0, nullTruncatable.getTruncatedByteCount());
  }

  @Test
  public void toTruncatableStringProtoTest(){
    String truncatableString = "myTruncatableString";
    TruncatableString testTruncatable = TraceTranslator.toTruncatableStringProto(truncatableString);

    assertEquals("myTruncatableString", testTruncatable.getValue());
    assertEquals(0, testTruncatable.getTruncatedByteCount());
  }

  @Test
  public void toTimestampProtoTest(){
    long epochNanos = TimeUnit.SECONDS.toNanos(3001) + 255;
    com.google.protobuf.Timestamp timestamp = TraceTranslator.toTimestampProto(epochNanos);

    assertEquals(3001, timestamp.getSeconds());
    assertEquals(255, timestamp.getNanos());
  }

  @Test
  public void toAttributesProtoTest(){
    ReadableAttributes attributes = Attributes.newBuilder()
            .setAttribute("myKey", "myValue")
            .setAttribute("http.status_code", true)
            .setAttribute("anotherKey", 100)
            .setAttribute("http.host", 3.14)
            .build();
    Map<String, AttributeValue> fixedAttributes = new LinkedHashMap<>();
    fixedAttributes.put("fixed", AttributeValue.newBuilder().setStringValue
        (TruncatableString.newBuilder().setValue("attributes").setTruncatedByteCount(0).build()).build());
    fixedAttributes.put("another", AttributeValue.newBuilder().setStringValue
        (TruncatableString.newBuilder().setValue("entry").setTruncatedByteCount(0).build()).build());

    Span.Attributes.Builder attributesBuilder = Span.Attributes.newBuilder().setDroppedAttributesCount(0)
        .putAttributeMap("myKey", AttributeValue.newBuilder().setStringValue
            (TruncatableString.newBuilder().setValue("myValue").setTruncatedByteCount(0)).build())
        .putAttributeMap("/http/host", AttributeValue.newBuilder().setStringValue
            (TruncatableString.newBuilder().setValue(String.valueOf(3.14)).setTruncatedByteCount(0)).build())
        .putAttributeMap("/http/status_code", AttributeValue.newBuilder().setBoolValue
            (true).build())
        .putAttributeMap("anotherKey", AttributeValue.newBuilder().setIntValue
            (100).build())
        .putAttributeMap("g.co/agent", AttributeValue.newBuilder().setStringValue
            (TruncatableString.newBuilder().setValue("opentelemetry-java [0.6.0]").setTruncatedByteCount(0)).build());
    for (Map.Entry<String, AttributeValue> entry : fixedAttributes.entrySet()) {
      attributesBuilder.putAttributeMap(entry.getKey(), entry.getValue());
    }

    Span.Attributes allAtributes = attributesBuilder.build();

    assertEquals(allAtributes, TraceTranslator.toAttributesProto(attributes, fixedAttributes));

  }

  @Test
  public void toTimeEventsProtoTest(){
    List<SpanData.Event> events = new ArrayList<>();
    SpanData.Event eventOne = new SpanData.Event() {
      //The SpanData.Event interfaces requires us to override these four methods
      @Override
      public long getEpochNanos() {
        return 0;
      }

      @Override
      public int getTotalAttributeCount() {
        return 0;
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
    //TODO: Figure out how to use timeEvents to call SpanData.Event methods to ensure those are working properly
  }

  @Test
  public void toStatusProtoTest(){
    io.opentelemetry.trace.Status myStatus = io.opentelemetry.trace.Status.OK.withDescription("Status description");
    Status spanStatus = TraceTranslator.toStatusProto(myStatus);

    //The int representation is 0 for canonical code "OK".
    assertEquals(0, spanStatus.getCode());
    assertEquals("Status description", spanStatus.getMessage());
  }
  
  @Test
  public void toLinksProtoTest(){
    List<io.opentelemetry.sdk.trace.data.SpanData.Link> links = new ArrayList<>();

    TraceId traceIdOne = new TraceId(321, 123);
    SpanId spanIdOne = new SpanId(12345);
    TraceFlags traceFlagsOne = TraceFlags.builder().build();
    TraceState traceStateOne = TraceState.builder().build();
    SpanContext spanContextOne = SpanContext.create(traceIdOne, spanIdOne, traceFlagsOne, traceStateOne);
    Attributes attributesOne = Attributes.newBuilder().setAttribute("key", "value").build();
    io.opentelemetry.sdk.trace.data.SpanData.Link linkOne =  io.opentelemetry.sdk.trace.data.SpanData.Link.create(spanContextOne, attributesOne);

    TraceId traceIdTwo = new TraceId(32473, 24893);
    SpanId spanIdTwo = new SpanId(54321);
    TraceFlags traceFlagsTwo = TraceFlags.builder().build();
    TraceState traceStateTwo = TraceState.builder().build();
    SpanContext spanContextTwo = SpanContext.create(traceIdTwo, spanIdTwo, traceFlagsTwo, traceStateTwo);
    Attributes attributesTwo = Attributes.newBuilder().setAttribute("random string", "another random string").build();
    io.opentelemetry.sdk.trace.data.SpanData.Link linkTwo =  io.opentelemetry.sdk.trace.data.SpanData.Link.create(spanContextTwo, attributesTwo);

    links.add(linkOne);
    links.add(linkTwo);
    Span.Links finalLinks = TraceTranslator.toLinksProto(links, 2);

    assertEquals(2, finalLinks.getLinkCount());
    assertEquals(0, finalLinks.getDroppedLinksCount());

    assertEquals(traceIdOne.toLowerBase16(), finalLinks.getLink(0).getTraceId());
    assertEquals(spanIdOne.toLowerBase16(), finalLinks.getLink(0).getSpanId());
    //the int representation is 0 for (Link.Type.TYPE_UNSPECIFIED), which is given in TraceTranslator.
    assertEquals(0, finalLinks.getLink(0).getTypeValue());
    assertTrue(finalLinks.getLink(0).getAttributes().containsAttributeMap("key"));

    assertEquals(traceIdTwo.toLowerBase16(), finalLinks.getLink(1).getTraceId());
    assertEquals(spanIdTwo.toLowerBase16(), finalLinks.getLink(1).getSpanId());
    assertEquals(0, finalLinks.getLink(1).getTypeValue());
    assertTrue(finalLinks.getLink(1).getAttributes().containsAttributeMap("random string"));
    assertFalse(finalLinks.getLink(1).getAttributes().containsAttributeMap("key"));
  }

  @Test
  public void getResourceLabelsTest(){
    Map<String, String> nullResources = new HashMap<>();
    Map<String, String> resources = new HashMap<>();
    resources.put("testOne", "testTwo");
    resources.put("another", "entry");

    Map<String, AttributeValue> resourceLabels = new LinkedHashMap<>();
    resourceLabels.put("g.co/r/testOne", AttributeValue.newBuilder().setStringValue
            (TruncatableString.newBuilder().setValue("testTwo").setTruncatedByteCount(0).build()).build());
    resourceLabels.put("g.co/r/another", AttributeValue.newBuilder().setStringValue
            (TruncatableString.newBuilder().setValue("entry").setTruncatedByteCount(0).build()).build());

    assertEquals(Collections.emptyMap(), TraceTranslator.getResourceLabels(nullResources));
    assertEquals(Collections.unmodifiableMap(resourceLabels), TraceTranslator.getResourceLabels(resources));
  }

}
