package com.google.cloud.opentelemetry.metric;

import com.google.api.MetricDescriptor;
import com.google.api.gax.grpc.GrpcCallContext;
import com.google.api.gax.rpc.ClientContext;
import com.google.cloud.monitoring.v3.stub.GrpcMetricServiceStub;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import io.grpc.CallOptions;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.util.List;

// A simplified version of TraceServiceClient, used ONLY for testing purposes.
class MockCloudMetricClient implements CloudMetricClient {

  private final GrpcMetricServiceStub stub;

  MockCloudMetricClient(String host, int port) throws IOException {
    stub = GrpcMetricServiceStub.create(
        ClientContext.newBuilder().setDefaultCallContext(
            GrpcCallContext
                .of(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), CallOptions.DEFAULT))
            .build());
  }

  public final MetricDescriptor createMetricDescriptor(CreateMetricDescriptorRequest request) {
    return stub.createMetricDescriptorCallable().call(request);
  }

  public final void createTimeSeries(ProjectName name, List<TimeSeries> timeSeries) {
    CreateTimeSeriesRequest request = CreateTimeSeriesRequest.newBuilder().setName(name.toString())
        .addAllTimeSeries(timeSeries)
        .build();
    stub.createTimeSeriesCallable().call(request);
  }

  public final void shutdown() {
    // Empty because not being tested
  }
}
