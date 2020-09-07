package com.google.cloud.opentelemetry.metric;

import com.google.api.MetricDescriptor;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import java.util.List;

public interface CloudMetricClient {
  MetricDescriptor createMetricDescriptor(CreateMetricDescriptorRequest request);

  void createTimeSeries(ProjectName name, List<TimeSeries> timeSeries);

  void shutdown();
}
