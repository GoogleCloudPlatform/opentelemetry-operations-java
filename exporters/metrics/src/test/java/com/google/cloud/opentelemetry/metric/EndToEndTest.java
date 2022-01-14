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
package com.google.cloud.opentelemetry.metric;

import static com.google.cloud.opentelemetry.metric.FakeData.aMetricData;
import static com.google.cloud.opentelemetry.metric.FakeData.aProjectId;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

@RunWith(JUnit4.class)
public class EndToEndTest {
  private MetricExporter exporter;
  private MockCloudMetricClient mockClient;

  /** A test-container instance that loads the Cloud-Ops-Mock server container. */
  private static class CloudOperationsMockContainer
      extends GenericContainer<CloudOperationsMockContainer> {
    CloudOperationsMockContainer() {
      super(
          new ImageFromDockerfile()
              .withDockerfileFromBuilder(
                  builder ->
                      builder
                          .from("golang:1.15")
                          .run("go get github.com/googleinterns/cloud-operations-api-mock/cmd")
                          .cmd(
                              "go run github.com/googleinterns/cloud-operations-api-mock/cmd --address=:8080")
                          .build()));
      this.withExposedPorts(8080).waitingFor(Wait.forLogMessage(".*Listening on.*\\n", 1));
    }

    public MockCloudMetricClient newCloudMetricClient() throws IOException {
      return new MockCloudMetricClient(getContainerIpAddress(), getFirstMappedPort());
    }
  }

  @Rule public CloudOperationsMockContainer mockContainer = new CloudOperationsMockContainer();

  @Before
  public void setup() throws Exception {
    mockClient = mockContainer.newCloudMetricClient();
  }

  @Test
  public void testExportMockMetricsDataList() {
    exporter = new MetricExporter(aProjectId, mockClient, MetricDescriptorStrategy.ALWAYS_SEND);
    assertTrue(exporter.export(ImmutableList.of(aMetricData)).isSuccess());
  }

  @Test
  public void testExportEmptyMetricsList() {
    exporter = new MetricExporter(aProjectId, mockClient, MetricDescriptorStrategy.ALWAYS_SEND);
    assertTrue(exporter.export(new ArrayList<>()).isSuccess());
  }
}
