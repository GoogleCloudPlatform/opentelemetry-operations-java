package com.google.cloud.opentelemetry.metric;

import static com.google.cloud.opentelemetry.metric.FakeData.aMetricData;
import static com.google.cloud.opentelemetry.metric.FakeData.aProjectId;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
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
  public void setup() throws MockServerStartupFailedPleaseReadmeException {
    try {
      // Find a free port to spin up our server at.
      ServerSocket socket = new ServerSocket(0);
      int port = socket.getLocalPort();
      String address = String.format("%s:%d", LOCALHOST, port);
      socket.close();

      // Start the mock server. This assumes the binary is present and in $PATH.
      // Typically, the CI will be the one that curls the binary and adds it to $PATH.
      String[] cmdArray = new String[] {System.getProperty("mock.server.path"), "-address", address};
      ProcessBuilder pb = new ProcessBuilder(cmdArray);
      pb.redirectErrorStream(true);
      mockServerProcess = pb.start();

      // Setup the mock metric client
      mockClient = new MockCloudMetricClient(LOCALHOST, port);

      // Block until the mock server starts (it will output the address after starting).
      BufferedReader br =
          new BufferedReader(new InputStreamReader(mockServerProcess.getInputStream()));
      br.readLine();
    } catch (Exception e) {
      System.err.print("Unable to start Google API Mock Server: ");
      System.err.println(System.getProperty("mock.server.path"));
      System.err.println("\tMake sure you're following the direction to run tests");
      System.err.println("\t$ source get_mock_server.sh");
      System.err.println("\t$ ./gradlew test -Dmock.server.path=$MOCKSERVER\n");
      System.err.println("Error Stream\n");
      throw new MockServerStartupFailedPleaseReadmeException("Failed to start mock server.", e);
    }
  }

  @After
  public void tearDown() {
    mockServerProcess.destroy();
  }

  @Test
  public void testExportMockMetricsDataList() {
    exporter = new MetricExporter(aProjectId, mockClient);
    assertTrue(exporter.export(ImmutableList.of(aMetricData)).isSuccess());
  }

  @Test
  public void testExportEmptyMetricsList() {
    exporter = new MetricExporter(aProjectId, mockClient);
    assertTrue(exporter.export(new ArrayList<>()).isSuccess());
  }
}
