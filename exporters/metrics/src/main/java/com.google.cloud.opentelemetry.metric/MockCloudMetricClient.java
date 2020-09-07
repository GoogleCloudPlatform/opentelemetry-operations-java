package com.google.cloud.opentelemetry.metric;

import com.google.api.MetricDescriptor;
import com.google.cloud.monitoring.v3.stub.GrpcMetricServiceStub;
import com.google.cloud.monitoring.v3.stub.MetricServiceStubSettings;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import java.io.IOException;
import java.util.List;

// A simplified version of TraceServiceClient, used ONLY for testing purposes.
class MockCloudMetricClient implements CloudMetricClient {

  private final GrpcMetricServiceStub stub;

  MockCloudMetricClient(String address) throws IOException {
    stub = GrpcMetricServiceStub.create(
        MetricServiceStubSettings.newBuilder().setEndpoint(address).build());
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
