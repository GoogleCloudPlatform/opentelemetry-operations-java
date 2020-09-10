package com.google.cloud.opentelemetry.metric;

import static com.google.cloud.opentelemetry.metric.FakeData.aGceResource;
import static com.google.cloud.opentelemetry.metric.FakeData.aMonotonicLongDescriptor;
import static com.google.cloud.opentelemetry.metric.FakeData.aProjectId;
import static com.google.cloud.opentelemetry.metric.FakeData.anInstrumentationLibraryInfo;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.NANO_PER_SECOND;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import io.opentelemetry.common.Labels;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.export.MetricExporter.ResultCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class EndToEndTest {

  private static final String LOCALHOST = "127.0.0.1";

  private Process mockServerProcess;
  private MetricExporter exporter;
  private MockCloudMetricClient mockClient;

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

    // Setup the mock metric client
    mockClient = new MockCloudMetricClient(LOCALHOST, port);

    // Block until the mock server starts (it will output the address after starting).
    BufferedReader br = new BufferedReader(new InputStreamReader(mockServerProcess.getInputStream()));
    br.readLine();
  }

  @After
  public void tearDown() {
    mockServerProcess.destroy();
  }

  @Test
  public void testExportMockMetricsDataList() {
    exporter = new MetricExporter(aProjectId, mockClient);

    LongPoint longPoint = LongPoint
        .create(1599032114 * NANO_PER_SECOND, Instant.now().plus(10, SECONDS).getEpochSecond() * NANO_PER_SECOND,
            Labels.empty(), 32L);
    MetricData metricData = MetricData
        .create(aMonotonicLongDescriptor, aGceResource, anInstrumentationLibraryInfo, ImmutableList.of(longPoint));
    assertEquals(ResultCode.SUCCESS, exporter.export(ImmutableList.of(metricData)));
  }

  @Test
  public void testExportEmptyMetricsList() {
    exporter = new MetricExporter(aProjectId, mockClient);

    assertEquals(ResultCode.SUCCESS, exporter.export(new ArrayList<>()));
  }
}
