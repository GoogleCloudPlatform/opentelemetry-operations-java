package com.google.cloud.opentelemetry.metric;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapGcpMonitoredResource;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapMetric;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapMetricDescriptor;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapPoint;
import static java.util.logging.Level.WARNING;

import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.cloud.monitoring.v3.stub.MetricServiceStub;
import com.google.common.collect.Lists;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import io.opentelemetry.common.Labels;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class MetricExporter implements io.opentelemetry.sdk.metrics.export.MetricExporter {

  private static final String PROJECT_NAME_PREFIX = "projects/";
  private static final long WRITE_INTERVAL_SECOND = 10;
  private static final int MAX_BATCH_SIZE = 200;
  private static final long NANO_PER_SECOND = (long) 1e9;

  private static final Logger logger = Logger.getLogger(MetricExporter.class.getName());

  private final MetricServiceClient metricServiceClient;
  private final String projectId;
  private final Instant exporterStartTime;
  private final Map<MetricWithLabels, Long> lastUpdatedTime = new HashMap<>();
  private String uniqueIdentifier;

  MetricExporter(
      String projectId,
      MetricServiceClient client,
      boolean addUniqueIdentifier
  ) {
    this.projectId = projectId;
    this.metricServiceClient = client;
    if (addUniqueIdentifier) {
      this.uniqueIdentifier = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
    }
    this.exporterStartTime = Instant.now();
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
          projectId, credentials);
    }
    return MetricExporter.createWithClient(
        projectId, MetricServiceClient.create(stub),
        configuration.getAddUniqueIdentifier());
  }

  private static MetricExporter createWithClient(
      String projectId,
      MetricServiceClient metricServiceClient,
      boolean addUniqueIdentifier) {
    return new MetricExporter(projectId, metricServiceClient, addUniqueIdentifier);
  }

  private static MetricExporter createWithCredentials(
      String projectId,
      Credentials credentials) throws IOException {
    MetricServiceSettings.Builder builder =
        MetricServiceSettings.newBuilder().setCredentialsProvider(
            FixedCredentialsProvider.create(checkNotNull(credentials, "Credentials not provided.")));
    return new MetricExporter(projectId, MetricServiceClient.create(builder.build()), false);
  }

  @Override
  public ResultCode export(Collection<MetricData> metrics) {
    List<TimeSeries> allTimesSeries = new ArrayList<>();

    for (MetricData metricData : metrics) {
      MetricDescriptor descriptor = mapMetricDescriptor(metricData, uniqueIdentifier);
      if (descriptor == null) {
        continue;
      }
      metricServiceClient.createMetricDescriptor(
          CreateMetricDescriptorRequest.newBuilder().setName(PROJECT_NAME_PREFIX + projectId)
              .setMetricDescriptor(descriptor)
              .build());

      MetricWithLabels updateKey = new MetricWithLabels(descriptor.getType(),
          metricData.getDescriptor().getConstantLabels());

      // We are expecting one point per MetricData
      Optional<MetricData.Point> metricPoint = metricData.getPoints().stream()
          .reduce((first, second) -> second);
      if (!metricPoint.isPresent()) {
        logger.log(WARNING, "No point found in metric {}", metricData);
        continue;
      }

      // Cloud Monitoring API allows, for any combination of labels and
      // metric name, one update per WRITE_INTERVAL seconds
      long pointCollectionTime = metricPoint.get().getEpochNanos();
      if (lastUpdatedTime.containsKey(updateKey)
          && pointCollectionTime <= lastUpdatedTime.get(updateKey) / NANO_PER_SECOND + WRITE_INTERVAL_SECOND) {
        continue;
      }

      Metric metric = mapMetric(metricData, descriptor, uniqueIdentifier);
      Point point = mapPoint(lastUpdatedTime, metricData, updateKey, exporterStartTime, pointCollectionTime);
      if (point == null) {
        continue;
      }

      allTimesSeries.add(TimeSeries.newBuilder()
          .setMetric(metric)
          .addPoints(point)
          .setResource(mapGcpMonitoredResource(metricData.getResource()))
          .build());
    }
    createTimeSeriesBatch(metricServiceClient, ProjectName.of(projectId), allTimesSeries);
    return ResultCode.SUCCESS;
  }

  private static void createTimeSeriesBatch(MetricServiceClient metricServiceClient, ProjectName projectName,
      List<TimeSeries> allTimesSeries) {
    List<List<TimeSeries>> batches = Lists.partition(allTimesSeries, MAX_BATCH_SIZE);
    for (List<TimeSeries> timeSeries : batches) {
      metricServiceClient.createTimeSeries(projectName, timeSeries);
    }
  }

  @Override
  public ResultCode flush() {
    // TODO (zoe): add support for flush
    return ResultCode.FAILURE;
  }

  @Override
  public void shutdown() {
    metricServiceClient.shutdown();
  }

  static class MetricWithLabels {

    private final String metricType;
    private final Labels labels;

    private MetricWithLabels(String metricType, Labels labels) {
      this.metricType = metricType;
      this.labels = labels;
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
      return Objects.equals(metricType, that.metricType) && Objects.equals(labels, that.labels);
    }

    @Override
    public int hashCode() {
      return Objects.hash(metricType, labels);
    }
  }
}
