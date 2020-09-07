package com.google.cloud.opentelemetry.metric;

import com.google.api.MetricDescriptor;
import com.google.api.gax.rpc.ClientContext;
import com.google.cloud.monitoring.v3.stub.GrpcMetricServiceStub;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import java.io.IOException;
import java.util.List;

// A simplified version of TraceServiceClient, used ONLY for testing purposes.
class MockCloudMetricClient implements CloudMetricClient {

  private final GrpcMetricServiceStub stub;

  MockCloudMetricClient(String endpoint) throws IOException {
    stub = GrpcMetricServiceStub.create(ClientContext.newBuilder().setEndpoint(endpoint).build());
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

  // Empty because not being tested
  public final void shutdown() {
  }
}
