package com.google.cloud.opentelemetry.trace;

import com.google.devtools.cloudtrace.v2.AttributeValue;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.test.TestSpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TraceId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class EndToEndTest {

  private static final String PROJECT_ID = "project-id";
  private static final Map<String, AttributeValue> FIXED_ATTRIBUTES = new HashMap<>();
  private static final TraceId TRACE_ID = new TraceId(321, 123);
  private static final SpanId SPAN_ID = new SpanId(12345);
  private static final SpanId PARENT_SPAN_ID = new SpanId(54321);
  private static final String SPAN_NAME = "MySpanName";
  private static final long START_EPOCH_NANOS = TimeUnit.SECONDS.toNanos(3000) + 200;
  private static final long END_EPOCH_NANOS = TimeUnit.SECONDS.toNanos(3001) + 255;
  private static final Status SPAN_DATA_STATUS = Status.OK;
  private static final String LOCALHOST = "127.0.0.1";

  private MockCloudTraceClient mockCloudTraceClient;
  private TraceExporter exporter;
  private Process mockServerProcess;


  @Before
  public void setup() throws IOException {
    // Find a free port to spin up our server at.
    ServerSocket socket = new ServerSocket(0);
    int port = socket.getLocalPort();
    String address = String.format("%s:%d", LOCALHOST, port);
    socket.close();

    // Start the mock server. This assumes the binary is present and in $PATH.
    // Typically, the CI will be the one that curls the binary and adds it to $PATH.
    String[] cmdArray = new String[]{"mock_server", "-address", address};
    ProcessBuilder pb = new ProcessBuilder(cmdArray);
    pb.redirectErrorStream(true);
    mockServerProcess = pb.start();

    // Setup the mock trace client.
    mockCloudTraceClient = new MockCloudTraceClient(LOCALHOST, port);

    // Block until the mock server starts (it will output the address after starting).
    BufferedReader br = new BufferedReader(new InputStreamReader(mockServerProcess.getInputStream()));
    br.readLine();
  }

  @After
  public void tearDown() {
    mockServerProcess.destroy();
  }

  @Test
  public void exportMockSpanDataList(){
    exporter = new TraceExporter(PROJECT_ID, mockCloudTraceClient, FIXED_ATTRIBUTES);
    Collection<SpanData> spanDataList = new ArrayList<>();

    TestSpanData spanDataOne = TestSpanData.newBuilder()
            .setParentSpanId(PARENT_SPAN_ID)
            .setSpanId(SPAN_ID)
            .setTraceId(TRACE_ID)
            .setName(SPAN_NAME)
            .setKind(io.opentelemetry.trace.Span.Kind.SERVER)
            .setEvents(Collections.emptyList())
            .setStatus(SPAN_DATA_STATUS)
            .setStartEpochNanos(START_EPOCH_NANOS)
            .setEndEpochNanos(END_EPOCH_NANOS)
            .setTotalRecordedLinks(0)
            .setHasRemoteParent(false)
            .setHasEnded(true)
            .build();

    spanDataList.add(spanDataOne);

    // Invokes export();
    assertEquals(SpanExporter.ResultCode.SUCCESS, exporter.export(spanDataList));
  }

  @Test
  public void exportEmptySpanDataList(){
    exporter = new TraceExporter(PROJECT_ID, mockCloudTraceClient, FIXED_ATTRIBUTES);
    Collection<SpanData> spanDataList = new ArrayList<>();

    // Invokes export();
    assertEquals(SpanExporter.ResultCode.SUCCESS, exporter.export(spanDataList));
  }
}
