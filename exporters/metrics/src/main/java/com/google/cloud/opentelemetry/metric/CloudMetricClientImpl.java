/*
 * Copyright 2023 Google LLC
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
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import java.util.List;

/** Directly talks to Cloud Monitoring. */
public final class CloudMetricClientImpl implements CloudMetricClient {
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
