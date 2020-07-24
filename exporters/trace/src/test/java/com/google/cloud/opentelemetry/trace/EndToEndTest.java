package com.google.cloud.opentelemetry.trace;

import com.google.cloud.trace.v2.TraceServiceClient;
import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.ProjectName;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.devtools.cloudtrace.v2.TruncatableString;
import com.google.protobuf.BoolValue;
import com.google.rpc.Status;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.SpanContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;

@RunWith(JUnit4.class)
public class EndToEndTest {
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  // Creating a mock TraceServiceClient so that batchWriteSpans() method won't actually be called
  TraceServiceClient mockTraceServiceClient = Mockito.mock(TraceServiceClient.class);

  private TraceExporter exporter;

  private static final String PROJECT_ID = "project-id";
  private static final Map<String, AttributeValue> FIXED_ATTRIBUTES = new HashMap<>();
  private static final TraceId TRACE_ID = new TraceId(321, 123);
  private static final SpanId SPAN_ID = new SpanId(12345);
  private static final SpanId PARENT_SPAN_ID = new SpanId(54321);
  private static final TraceFlags TRACE_FLAGS = TraceFlags.builder().build();
  private static final TraceState TRACE_STATE = TraceState.builder().build();
  private static final SpanContext spanContext = SpanContext.create(TRACE_ID, SPAN_ID, TRACE_FLAGS, TRACE_STATE);
  private static final String SPAN_NAME = "MySpanName";
  private static final long START_EPOCH_NANOS = TimeUnit.SECONDS.toNanos(3000) + 200;
  private static final long START_SECONDS = TimeUnit.NANOSECONDS.toSeconds(START_EPOCH_NANOS);
  private static final int START_NANOS = (int) (START_EPOCH_NANOS - TimeUnit.SECONDS.toNanos(START_SECONDS));
  private static final long END_EPOCH_NANOS = TimeUnit.SECONDS.toNanos(3001) + 255;
  private static final long END_SECONDS = TimeUnit.NANOSECONDS.toSeconds(END_EPOCH_NANOS);
  private static final int END_NANOS = (int) (END_EPOCH_NANOS - TimeUnit.SECONDS.toNanos(END_SECONDS));
  private static final String SERVER_PREFIX = "Recv.";
  private static final String AGENT_LABEL_KEY = "g.co/agent";
  private static final String OPEN_TELEMETRY_LIBRARY_VERSION = "0.3.0";
  private static final String AGENT_LABEL_VALUE_STRING = "opentelemetry-java [" + OPEN_TELEMETRY_LIBRARY_VERSION + "]";
  private static final AttributeValue AGENT_LABEL_VALUE = AttributeValue.newBuilder()
          .setStringValue(TruncatableString.newBuilder().setValue(AGENT_LABEL_VALUE_STRING).setTruncatedByteCount(0).build()).build();
  private static final Span.Attributes ATTRIBUTES = Span.Attributes.newBuilder()
          .setDroppedAttributesCount(0)
          .putAttributeMap(AGENT_LABEL_KEY, AGENT_LABEL_VALUE)
          .build();
  private static final Span.TimeEvents TIME_EVENTS = Span.TimeEvents.newBuilder().build();
  private static final io.opentelemetry.trace.Status SPAN_DATA_STATUS = io.opentelemetry.trace.Status.OK;
  private static final Status SPAN_STATUS = Status.newBuilder().setCode(SPAN_DATA_STATUS.getCanonicalCode().value())
          .build();
  private static final Span.Links LINKS = Span.Links.newBuilder().setDroppedLinksCount(0).build();


  // Some test cases. Since the core function of this exporter is export(), we may focus on testing that.

  @Test
  public void export(){
    // Constructs an exporter instance.
    exporter = new TraceExporter(PROJECT_ID, mockTraceServiceClient, FIXED_ATTRIBUTES);
    Collection<SpanData> spanDataList = new ArrayList<>();

    SpanData spanDataOne = SpanData.newBuilder()
            .setParentSpanId(PARENT_SPAN_ID)
            .setSpanId(SPAN_ID)
            .setTraceId(TRACE_ID)
            .setName(SPAN_NAME)
            .setKind(io.opentelemetry.trace.Span.Kind.SERVER)
            .setTimedEvents(Collections.emptyList())
            .setStatus(SPAN_DATA_STATUS)
            .setStartEpochNanos(START_EPOCH_NANOS)
            .setEndEpochNanos(END_EPOCH_NANOS)
            .setTotalRecordedLinks(0)
            .setHasRemoteParent(false)
            .setHasEnded(true)
            .build();

    spanDataList.add(spanDataOne);

    // Some hardcoding of expected data for span.
    List<Span> expectedSpans = new ArrayList<>();
    Span spanOne = Span.newBuilder()
            .setName("projects/" + PROJECT_ID + "/traces/" + TRACE_ID.toLowerBase16() + "/spans/" + SPAN_ID.toLowerBase16())
            .setSpanId(SPAN_ID.toLowerBase16())
            .setDisplayName(TruncatableString.newBuilder().setValue(SERVER_PREFIX + SPAN_NAME).setTruncatedByteCount(0).build())
            .setStartTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(START_SECONDS).setNanos(START_NANOS).build())
            .setAttributes(ATTRIBUTES)
            .setTimeEvents(TIME_EVENTS)
            .setStatus(SPAN_STATUS)
            .setEndTime(com.google.protobuf.Timestamp.newBuilder().setSeconds(END_SECONDS).setNanos(END_NANOS).build())
            .setLinks(LINKS)
            .setParentSpanId(PARENT_SPAN_ID.toLowerBase16())
            .setSameProcessAsParentSpan(BoolValue.of(true))
            .build();
    // Instead of hardcoding as above, Span.parseFrom method can potentially be used to parse from a hardcoded proto
    expectedSpans.add(spanOne);

    // My original try, but batchWriteSpans is void and cannot be stubbed with a return value
    // doReturn(expectedSpans).when(mockTraceServiceClient).batchWriteSpans(any(ProjectName.class), anyList());

    // Instead of doReturn, doAnswer is better to deal with whenever batchWriteSpans() is called.
    // Rather than actually call the BatchWriteSpans() method from the API, null is returned.
    doAnswer(invocation -> null)
            .when(mockTraceServiceClient).batchWriteSpans(eq(PROJECT_ID), anyList());

    // Invokes export();
    assertEquals(SpanExporter.ResultCode.SUCCESS, exporter.export(spanDataList));

    // Make sure that the parameters passed to batchWriteSpans() are what we expect them to be
    verify(mockTraceServiceClient, times(1)).batchWriteSpans(eq(ProjectName.of(PROJECT_ID)),
            eq(expectedSpans));
  }

  @org.junit.Test
  public void export_EmptySpanDataList(){
    // Constructs a exporter instance.
    exporter = new TraceExporter(PROJECT_ID, mockTraceServiceClient, FIXED_ATTRIBUTES);
    Collection<SpanData> spanDataList = new ArrayList<>();

    // Invokes export();
    assertEquals((SpanExporter.ResultCode.SUCCESS), (exporter.export(spanDataList)));

    // Some verification.
    List<Span> expectedSpans = new ArrayList<>();
    verify(mockTraceServiceClient, times(1)).batchWriteSpans(eq(ProjectName.of(PROJECT_ID)),
            eq(expectedSpans));
  }

}
