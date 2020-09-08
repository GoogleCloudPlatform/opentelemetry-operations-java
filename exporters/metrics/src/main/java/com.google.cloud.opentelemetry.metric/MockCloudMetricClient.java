package com.google.cloud.opentelemetry.metric;

import com.google.api.MetricDescriptor;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.auth.Credentials;
import com.google.cloud.monitoring.v3.stub.GrpcMetricServiceStub;
import com.google.cloud.monitoring.v3.stub.MetricServiceStubSettings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

// A simplified version of TraceServiceClient, used ONLY for testing purposes.
class MockCloudMetricClient implements CloudMetricClient {

  private final GrpcMetricServiceStub stub;

  MockCloudMetricClient(String host, int port, Credentials credentials) throws IOException {
    stub = GrpcMetricServiceStub.create(
        MetricServiceStubSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(new FakeCreds()))
            .setTransportChannelProvider(FixedTransportChannelProvider.create(GrpcTransportChannel.create(
                ManagedChannelBuilder.forAddress(host, port).usePlaintext().build())))
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

  private static class FakeCreds extends Credentials {

    @Override
    public String getAuthenticationType() {
      return null;
    }

    @Override
    public Map<String, List<String>> getRequestMetadata(URI uri) {
      return ImmutableMap.of("Authorization", ImmutableList.of("Bearer owner"));
    }

    @Override
    public boolean hasRequestMetadata() {
      return true;
    }

    @Override
    public boolean hasRequestMetadataOnly() {
      return true;
    }

    @Override
    public void refresh() {
    }
  }
}
