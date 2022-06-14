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

import com.google.api.MetricDescriptor;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.monitoring.v3.stub.GrpcMetricServiceStub;
import com.google.cloud.monitoring.v3.stub.MetricServiceStubSettings;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.util.List;

// A simplified version of TraceServiceClient, used ONLY for testing purposes.
class MockCloudMetricClient implements CloudMetricClient {

  private final GrpcMetricServiceStub stub;

  MockCloudMetricClient(String host, int port) throws IOException {
    stub =
        GrpcMetricServiceStub.create(
            MetricServiceStubSettings.newBuilder()
                .setCredentialsProvider(NoCredentialsProvider.create())
                .setTransportChannelProvider(
                    FixedTransportChannelProvider.create(
                        GrpcTransportChannel.create(
                            ManagedChannelBuilder.forAddress(host, port).usePlaintext().build())))
                .build());
  }

  public final MetricDescriptor createMetricDescriptor(CreateMetricDescriptorRequest request) {
    return stub.createMetricDescriptorCallable().call(request);
  }

  public final void createTimeSeries(ProjectName name, List<TimeSeries> timeSeries) {
    CreateTimeSeriesRequest request =
        CreateTimeSeriesRequest.newBuilder()
            .setName(name.toString())
            .addAllTimeSeries(timeSeries)
            .build();
    stub.createTimeSeriesCallable().call(request);
  }

  public final void shutdown() {
    stub.shutdown();
  }
}
