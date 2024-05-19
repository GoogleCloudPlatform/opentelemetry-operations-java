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

import static com.google.api.client.util.Preconditions.checkNotNull;

import com.google.api.MetricDescriptor;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates internal implementation details for exporting metrics to Google Cloud
 * Platform. This class should not be exposed to the end user for direct usage.
 */
class InternalMetricExporter implements MetricExporter {

  private static final Logger logger = LoggerFactory.getLogger(InternalMetricExporter.class);

  private static final String PROJECT_NAME_PREFIX = "projects/";
  private static final int MAX_BATCH_SIZE = 200;

  private final CloudMetricClient metricServiceClient;
  private final String projectId;
  private final String prefix;
  private final MetricDescriptorStrategy metricDescriptorStrategy;
  private final Predicate<AttributeKey<?>> resourceAttributesFilter;
  private final boolean useCreateServiceTimeSeries;
  private final MonitoredResourceMapping monitoredResourceMapping;

  InternalMetricExporter(
      String projectId,
      String prefix,
      CloudMetricClient client,
      MetricDescriptorStrategy descriptorStrategy,
      Predicate<AttributeKey<?>> resourceAttributesFilter,
      boolean useCreateServiceTimeSeries,
      MonitoredResourceMapping monitoredResourceMapping) {
    this.projectId = projectId;
    this.prefix = prefix;
    this.metricServiceClient = client;
    this.metricDescriptorStrategy = descriptorStrategy;
    this.resourceAttributesFilter = resourceAttributesFilter;
    this.useCreateServiceTimeSeries = useCreateServiceTimeSeries;
    this.monitoredResourceMapping = monitoredResourceMapping;
  }

  static InternalMetricExporter createWithConfiguration(MetricConfiguration configuration)
      throws IOException {
    String projectId = configuration.getProjectId();
    String prefix = configuration.getPrefix();
    MetricServiceSettings.Builder builder = MetricServiceSettings.newBuilder();
    // For testing, we need to hack around our gRPC config.
    if (configuration.getInsecureEndpoint()) {
      builder.setCredentialsProvider(NoCredentialsProvider.create());
      builder.setTransportChannelProvider(
          FixedTransportChannelProvider.create(
              GrpcTransportChannel.create(
                  ManagedChannelBuilder.forTarget(configuration.getMetricServiceEndpoint())
                      .usePlaintext()
                      .build())));
    } else {
      // For any other endpoint, we force credentials to exist.
      Credentials credentials =
          configuration.getCredentials() == null
              ? GoogleCredentials.getApplicationDefault()
              : configuration.getCredentials();

      builder.setCredentialsProvider(
          FixedCredentialsProvider.create(checkNotNull(credentials, "Credentials not provided.")));
      builder.setEndpoint(configuration.getMetricServiceEndpoint());
    }
    builder
        .createMetricDescriptorSettings()
        .setSimpleTimeoutNoRetries(
            org.threeten.bp.Duration.ofMillis(configuration.getDeadline().toMillis()));

    return new InternalMetricExporter(
        projectId,
        prefix,
        new CloudMetricClientImpl(MetricServiceClient.create(builder.build())),
        configuration.getDescriptorStrategy(),
        configuration.getResourceAttributesFilter(),
        configuration.getUseServiceTimeSeries(),
        configuration.getMonitoredResourceMapping());
  }

  @VisibleForTesting
  static InternalMetricExporter createWithClient(
      String projectId,
      String prefix,
      CloudMetricClient metricServiceClient,
      MetricDescriptorStrategy descriptorStrategy,
      Predicate<AttributeKey<?>> resourceAttributesFilter,
      boolean useCreateServiceTimeSeries,
      MonitoredResourceMapping monitoredResourceMapping) {
    return new InternalMetricExporter(
        projectId,
        prefix,
        metricServiceClient,
        descriptorStrategy,
        resourceAttributesFilter,
        useCreateServiceTimeSeries,
        monitoredResourceMapping);
  }

  private void exportDescriptor(MetricDescriptor descriptor) {
    logger.trace("Creating metric descriptor: {}", descriptor);
    metricServiceClient.createMetricDescriptor(
        CreateMetricDescriptorRequest.newBuilder()
            .setName(PROJECT_NAME_PREFIX + projectId)
            .setMetricDescriptor(descriptor)
            .build());
  }

  @Override
  public AggregationTemporality getAggregationTemporality(@Nonnull InstrumentType instrumentType) {
    return AggregationTemporality.CUMULATIVE;
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    // General Algorithm for export:
    // 1. Iterate over all points in the set of metrics to export
    // 2. Attempt to register MetricDescriptors (using configured strategy)
    // 3. Fire the set of time series off.
    MetricTimeSeriesBuilder builder =
        new AggregateByLabelMetricTimeSeriesBuilder(
            projectId, prefix, resourceAttributesFilter, monitoredResourceMapping);
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
    exportDescriptors(builder);

    List<TimeSeries> series = builder.getTimeSeries();
    Consumer<List<TimeSeries>> timeSeriesGenerator =
        timeSeries -> {
          if (useCreateServiceTimeSeries) {
            metricServiceClient.createServiceTimeSeries(ProjectName.of(projectId), timeSeries);
          } else {
            metricServiceClient.createTimeSeries(ProjectName.of(projectId), timeSeries);
          }
        };
    createTimeSeriesBatch(series, timeSeriesGenerator);
    // TODO: better error reporting.
    if (series.size() < metrics.size()) {
      return CompletableResultCode.ofFailure();
    }
    return CompletableResultCode.ofSuccess();
  }

  private void exportDescriptors(MetricTimeSeriesBuilder timeSeriesBuilder) {
    if (useCreateServiceTimeSeries) {
      // do not export metric descriptors when using createServiceTimeSeries
      return;
    }
    try {
      Collection<MetricDescriptor> descriptors = timeSeriesBuilder.getDescriptors();
      if (!descriptors.isEmpty()) {
        metricDescriptorStrategy.exportDescriptors(descriptors, this::exportDescriptor);
      }
    } catch (Exception e) {
      logger.warn("Failed to create metric descriptors", e);
    }
  }

  // Fragment metrics into batches and send to GCM.
  private void createTimeSeriesBatch(
      List<TimeSeries> allTimesSeries, Consumer<List<TimeSeries>> timeSeriesGenerator) {
    List<List<TimeSeries>> batches = Lists.partition(allTimesSeries, MAX_BATCH_SIZE);
    for (List<TimeSeries> timeSeries : batches) {
      timeSeriesGenerator.accept(new ArrayList<>(timeSeries));
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
}
