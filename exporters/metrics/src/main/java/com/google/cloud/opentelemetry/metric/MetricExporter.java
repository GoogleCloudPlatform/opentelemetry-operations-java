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

import static com.google.api.client.util.Preconditions.checkNotNull;

import com.google.api.MetricDescriptor;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.cloud.monitoring.v3.stub.MetricServiceStub;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricExporter implements io.opentelemetry.sdk.metrics.export.MetricExporter {

  private static final Logger logger = LoggerFactory.getLogger(MetricExporter.class);

  private static final String PROJECT_NAME_PREFIX = "projects/";
  private static final int MAX_BATCH_SIZE = 200;

  private final CloudMetricClient metricServiceClient;
  private final String projectId;
  private final MetricDescriptorStrategy metricDescriptorStrategy;

  MetricExporter(
      String projectId, CloudMetricClient client, MetricDescriptorStrategy descriptorStrategy) {
    this.projectId = projectId;
    this.metricServiceClient = client;
    this.metricDescriptorStrategy = descriptorStrategy;
  }

  public static MetricExporter createWithDefaultConfiguration() throws IOException {
    MetricConfiguration configuration = MetricConfiguration.builder().build();
    return MetricExporter.createWithConfiguration(configuration);
  }

  public static MetricExporter createWithConfiguration(MetricConfiguration configuration)
      throws IOException {
    String projectId = configuration.getProjectId();
    MetricServiceStub stub = configuration.getMetricServiceStub();

    if (stub == null) {
      Credentials credentials =
          configuration.getCredentials() == null
              ? GoogleCredentials.getApplicationDefault()
              : configuration.getCredentials();

      return MetricExporter.createWithCredentials(
          projectId,
          credentials,
          configuration.getDeadline(),
          configuration.getDescriptorStrategy());
    }
    return MetricExporter.createWithClient(
        projectId,
        new CloudMetricClientImpl(MetricServiceClient.create(stub)),
        configuration.getDescriptorStrategy());
  }

  @VisibleForTesting
  static MetricExporter createWithClient(
      String projectId,
      CloudMetricClient metricServiceClient,
      MetricDescriptorStrategy descriptorStrategy) {
    return new MetricExporter(projectId, metricServiceClient, descriptorStrategy);
  }

  private static MetricExporter createWithCredentials(
      String projectId,
      Credentials credentials,
      Duration deadline,
      MetricDescriptorStrategy descriptorStrategy)
      throws IOException {
    MetricServiceSettings.Builder builder =
        MetricServiceSettings.newBuilder()
            .setCredentialsProvider(
                FixedCredentialsProvider.create(
                    checkNotNull(credentials, "Credentials not provided.")));
    builder
        .createMetricDescriptorSettings()
        .setSimpleTimeoutNoRetries(org.threeten.bp.Duration.ofMillis(deadline.toMillis()));
    return new MetricExporter(
        projectId,
        new CloudMetricClientImpl(MetricServiceClient.create(builder.build())),
        descriptorStrategy);
  }

  private void exportDescriptor(MetricDescriptor descriptor) {
    logger.trace("Creating metric descriptor: %s", descriptor);
    metricServiceClient.createMetricDescriptor(
        CreateMetricDescriptorRequest.newBuilder()
            .setName(PROJECT_NAME_PREFIX + projectId)
            .setMetricDescriptor(descriptor)
            .build());
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    // General Algorithm for export:
    // 1. Iterate over all points in the set of metrics to export
    // 2. Attempt to register MetricDescriptors (using configured strategy)
    // 3. Fire the set of time series off.
    MetricTimeSeriesBuilder builder = new AggregateByLabelMetricTimeSeriesBuilder(projectId);
    for (final MetricData metricData : metrics) {
      // Extract all the underlying points.
      switch (metricData.getType()) {
        case LONG_GAUGE:
          for (LongPointData point : metricData.getLongGaugeData().getPoints()) {
            builder.recordPoint(metricData, point);
          }
          break;
        case LONG_SUM:
          for (LongPointData point : metricData.getLongSumData().getPoints()) {
            builder.recordPoint(metricData, point);
          }
          break;
        case DOUBLE_GAUGE:
          for (DoublePointData point : metricData.getDoubleGaugeData().getPoints()) {
            builder.recordPoint(metricData, point);
          }
          break;
        case DOUBLE_SUM:
          for (DoublePointData point : metricData.getDoubleSumData().getPoints()) {
            builder.recordPoint(metricData, point);
          }
          break;
        case HISTOGRAM:
          for (HistogramPointData point : metricData.getHistogramData().getPoints()) {
            builder.recordPoint(metricData, point);
          }
          break;
        default:
          logger.error("OpenTelemetry Metric type {} not supported.", metricData.getType());
          continue;
      }
      // TODO: Filter metrics by last updated time....
      // MetricWithLabels updateKey =
      // new MetricWithLabels(descriptor.getType(), metricPoint.getLabels());

      // // Cloud Monitoring API allows, for any combination of labels and
      // // metric name, one update per WRITE_INTERVAL seconds
      // long pointCollectionTime = metricPoint.getEpochNanos();
      // if (lastUpdatedTime.containsKey(updateKey)
      // && pointCollectionTime
      // <= lastUpdatedTime.get(updateKey) / NANO_PER_SECOND + WRITE_INTERVAL_SECOND)
      // {
      // continue;
      // }
    }
    // Update metric descriptors based on configured strategy.
    try {
      Collection<MetricDescriptor> descriptors = builder.getDescriptors();
      if (!descriptors.isEmpty()) {
        metricDescriptorStrategy.exportDescriptors(descriptors, this::exportDescriptor);
      }
    } catch (Exception e) {
      logger.warn("Failed to create metric descriptors", e);
    }

    List<TimeSeries> series = builder.getTimeSeries();
    createTimeSeriesBatch(metricServiceClient, ProjectName.of(projectId), series);
    // TODO: better error reporting.
    if (series.size() < metrics.size()) {
      return CompletableResultCode.ofFailure();
    }
    return CompletableResultCode.ofSuccess();
  }

  // Fragment metrics into batches and send to GCM.
  private static void createTimeSeriesBatch(
      CloudMetricClient metricServiceClient,
      ProjectName projectName,
      List<TimeSeries> allTimesSeries) {
    List<List<TimeSeries>> batches = Lists.partition(allTimesSeries, MAX_BATCH_SIZE);
    for (List<TimeSeries> timeSeries : batches) {
      metricServiceClient.createTimeSeries(projectName, new ArrayList<>(timeSeries));
    }
  }

  /**
   * The exporter does not batch metrics, so this method will immediately return with success.
   *
   * @return always Success
   */
  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    metricServiceClient.shutdown();

    return CompletableResultCode.ofSuccess();
  }

  @Override
  public AggregationTemporality getPreferredTemporality() {
    return AggregationTemporality.CUMULATIVE;
  }

  // TODO: Move this to its own class.
  static class MetricWithLabels {

    private final String metricType;
    private final Attributes attributes;

    MetricWithLabels(String metricType, Attributes attributes) {
      this.metricType = metricType;
      this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MetricWithLabels that = (MetricWithLabels) o;
      return Objects.equals(metricType, that.metricType)
          && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(metricType, attributes);
    }
  }
}
