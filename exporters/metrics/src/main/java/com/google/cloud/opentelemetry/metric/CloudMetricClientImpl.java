package com.google.cloud.opentelemetry.metric;

import com.google.api.MetricDescriptor;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import java.util.List;

public class CloudMetricClientImpl implements CloudMetricClient {
  private final MetricServiceClient metricServiceClient;

  public CloudMetricClientImpl(MetricServiceClient metricServiceClient) {
    this.metricServiceClient = metricServiceClient;
  }

  @Override
  public MetricDescriptor createMetricDescriptor(CreateMetricDescriptorRequest request) {
    return this.metricServiceClient.createMetricDescriptor(request);
  }

  @Override
  public void createTimeSeries(ProjectName name, List<TimeSeries> timeSeries) {
    this.metricServiceClient.createTimeSeries(name, timeSeries);
  }

  @Override
  public void shutdown() {
    this.metricServiceClient.shutdown();
  }
}
