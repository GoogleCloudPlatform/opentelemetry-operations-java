package com.google.cloud.opentelemetry.trace;

import com.google.devtools.cloudtrace.v2.AttributeValue;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;;
import io.opentelemetry.trace.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

class TestSpanData implements SpanData {
  @Override
  public TraceId getTraceId() {
    return null;
  }

  @Override
  public SpanId getSpanId() {
    return null;
  }

  @Override
  public TraceFlags getTraceFlags() {
    return null;
  }

  @Override
  public TraceState getTraceState() {
    return null;
  }

  @Override
  public SpanId getParentSpanId() {
    return null;
  }

  @Override
  public Resource getResource() {
    return null;
  }

  @Override
  public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Span.Kind getKind() {
    return null;
  }

  @Override
  public long getStartEpochNanos() {
    return 0;
  }

  @Override
  public ReadableAttributes getAttributes() {
    return null;
  }

  @Override
  public List<Event> getEvents() {
    return null;
  }

  @Override
  public List<Link> getLinks() {
    return null;
  }

  @Override
  public Status getStatus() {
    return null;
  }

  @Override
  public long getEndEpochNanos() {
    return 0;
  }

  @Override
  public boolean getHasRemoteParent() {
    return false;
  }

  @Override
  public boolean getHasEnded() {
    return false;
  }

  @Override
  public int getTotalRecordedEvents() {
    return 0;
  }

  @Override
  public int getTotalRecordedLinks() {
    return 0;
  }

  @Override
  public int getTotalAttributeCount() {
    return 0;
  }
}

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
    String[] cmdArray = new String[]{System.getProperty("mock.server.path"), "-address", address};
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

    TestSpanData spanDataOne = new TestSpanData();

    spanDataList.add(spanDataOne);

    // Invokes export();
    assertTrue(exporter.export(spanDataList).isSuccess());
  }

  @Test
  public void exportEmptySpanDataList(){
    exporter = new TraceExporter(PROJECT_ID, mockCloudTraceClient, FIXED_ATTRIBUTES);
    Collection<SpanData> spanDataList = new ArrayList<>();

    // Invokes export();
    assertTrue(exporter.export(spanDataList).isSuccess());
  }
}
