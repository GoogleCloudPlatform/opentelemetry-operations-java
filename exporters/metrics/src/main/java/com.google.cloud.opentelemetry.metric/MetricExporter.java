package com.google.cloud.opentelemetry.metric;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type.MONOTONIC_DOUBLE;
import static io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type.MONOTONIC_LONG;
import static io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type.NON_MONOTONIC_DOUBLE;
import static io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type.NON_MONOTONIC_LONG;
import static java.util.logging.Level.WARNING;

import com.google.api.LabelDescriptor;
import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.MonitoredResource;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.cloud.monitoring.v3.stub.MetricServiceStub;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.Point.Builder;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.Timestamp;
import io.opentelemetry.common.Labels;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import io.opentelemetry.sdk.resources.Resource;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class MetricExporter implements io.opentelemetry.sdk.metrics.export.MetricExporter {

  private static final String UNIQUE_IDENTIFIER_KEY = "opentelemetry_id";
  private static final String DESCRIPTOR_TYPE_URL = "custom.googleapis.com/OpenTelemetry/";
  private static final String PROJECT_NAME_PREFIX = "projects/";
  private static final long WRITE_INTERVAL_SECOND = 10;
  private static final int MAX_BATCH_SIZE = 200;
  private static final long NANO_PER_SECOND = (long) 1e9;

  private static final Set<Type> GAUGE_TYPES = ImmutableSet.of(NON_MONOTONIC_LONG, NON_MONOTONIC_DOUBLE);
  private static final Set<MetricData.Descriptor.Type> CUMULATIVE_TYPES = ImmutableSet
      .of(MONOTONIC_LONG, MONOTONIC_DOUBLE);
  private static final Set<MetricData.Descriptor.Type> LONG_TYPES = ImmutableSet.of(NON_MONOTONIC_LONG, MONOTONIC_LONG);
  private static final Set<MetricData.Descriptor.Type> DOUBLE_TYPES = ImmutableSet
      .of(NON_MONOTONIC_DOUBLE, MONOTONIC_DOUBLE);

  private static final Map<String, Map<String, String>> OTEL_TO_GCP_LABELS = ImmutableMap.<String, Map<String, String>>builder()
      .put("gce_instance", ImmutableMap.<String, String>builder()
          .put("host.id", "instance_id")
          .put("cloud.account.id", "project_id")
          .put("cloud.zone", "zone")
          .build())
      .put("gke_container", ImmutableMap.<String, String>builder()
          .put("k8s.cluster.name", "cluster_name")
          .put("k8s.namespace.name", "namespace_id")
          .put("k8s.pod.name", "pod_id")
          .put("host.id", "instance_id")
          .put("container.name", "container_name")
          .put("cloud.account.id", "project_id")
          .put("cloud.zone", "zone")
          .build())
      .build();

  private static final Logger logger = Logger.getLogger(MetricExporter.class.getName());

  private final MetricServiceClient metricServiceClient;
  private final String projectId;
  private final Instant exporterStartTime;
  private final Map<MetricWithLabels, Long> lastUpdatedTime = new HashMap<>();
  private String uniqueIdentifier = null;

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

    for (MetricData metric : metrics) {
      MetricDescriptor descriptor = createMetricDescriptor(metricServiceClient, metric, projectId, uniqueIdentifier);
      if (descriptor == null) {
        continue;
      }

      MetricWithLabels updateKey = new MetricWithLabels(descriptor.getType(),
          metric.getDescriptor().getConstantLabels());

      // We are expecting one point per MetricData
      Optional<MetricData.Point> point = metric.getPoints().stream()
          .reduce((first, second) -> second);
      if (!point.isPresent()) {
        logger.log(WARNING, "No point found in metric {}", metric);
        continue;
      }

      // Cloud Monitoring API allows, for any combination of labels and
      // metric name, one update per WRITE_INTERVAL seconds
      long pointCollectionTime = point.get().getEpochNanos();
      if (lastUpdatedTime.containsKey(updateKey)
          && pointCollectionTime <= lastUpdatedTime.get(updateKey) / NANO_PER_SECOND + WRITE_INTERVAL_SECOND) {
        continue;
      }

      Metric.Builder metricBuilder = Metric.newBuilder().setType(descriptor.getType());

      // Add labels to the metric
      metric.getDescriptor().getConstantLabels().forEach(metricBuilder::putLabels);
      if (uniqueIdentifier != null) {
        metricBuilder.putLabels(UNIQUE_IDENTIFIER_KEY, uniqueIdentifier);
      }

      // Add point value to the metric
      Point.Builder pointBuilder = Point.newBuilder();
      Type type = metric.getDescriptor().getType();
      if (LONG_TYPES.contains(type)) {
        pointBuilder.setValue(TypedValue.newBuilder().setInt64Value(
            ((MetricData.LongPoint) metric.getPoints().iterator().next()).getValue()));
      } else if (DOUBLE_TYPES.contains(type)) {
        pointBuilder.setValue(TypedValue.newBuilder().setDoubleValue(
            ((MetricData.DoublePoint) metric.getPoints().iterator().next()).getValue()));
      } else {
        logger.log(WARNING, "Type {} not supported", type);
        continue;
      }

      setStartEndTimes(lastUpdatedTime, pointBuilder, updateKey, type, exporterStartTime, pointCollectionTime);
      allTimesSeries.add(
          TimeSeries.newBuilder().setMetric(metricBuilder.build()).addPoints(pointBuilder.build())
              .setResource(mapToGcpMonitoredResource(metric.getResource()))
              .build());
    }

    createTimeSeriesBatch(metricServiceClient, ProjectName.of(projectId), allTimesSeries);
    return ResultCode.SUCCESS;
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

  private static MetricDescriptor createMetricDescriptor(
      MetricServiceClient metricServiceClient, MetricData metric,
      String projectId, String uniqueIdentifier) {
    String instrumentName = metric.getInstrumentationLibraryInfo().getName();
    MetricDescriptor.Builder builder = MetricDescriptor.newBuilder().setDisplayName(instrumentName)
        .setType(DESCRIPTOR_TYPE_URL + instrumentName);
    metric.getDescriptor().getConstantLabels().forEach((key, value) -> builder.addLabels(mapConstantLabel(key, value)));
    if (uniqueIdentifier != null) {
      builder.addLabels(
          LabelDescriptor.newBuilder().setKey(UNIQUE_IDENTIFIER_KEY).setValueType(LabelDescriptor.ValueType.STRING)
              .build());
    }

    MetricData.Descriptor.Type metricType = metric.getDescriptor().getType();
    if (GAUGE_TYPES.contains(metricType)) {
      builder.setMetricKind(MetricDescriptor.MetricKind.GAUGE);
    } else if (CUMULATIVE_TYPES.contains(metricType)) {
      builder.setMetricKind(MetricDescriptor.MetricKind.CUMULATIVE);
    } else {
      logger.log(WARNING, "Metric type {} not supported", metricType);
      return null;
    }
    if (LONG_TYPES.contains(metricType)) {
      builder.setValueType(MetricDescriptor.ValueType.INT64);
    } else if (DOUBLE_TYPES.contains(metricType)) {
      builder.setValueType(MetricDescriptor.ValueType.DOUBLE);
    } else {
      logger.log(WARNING, "Metric type {} not supported", metricType);
      return null;
    }
    return metricServiceClient.createMetricDescriptor(
        CreateMetricDescriptorRequest.newBuilder().setName(PROJECT_NAME_PREFIX + projectId)
            .setMetricDescriptor(builder.build())
            .build());
  }

  private void setStartEndTimes(Map<MetricWithLabels, Long> lastUpdatedTime, Builder pointBuilder,
      MetricWithLabels updateKey, Type descriptorType, Instant exporterStartTime, long pointCollectionTime) {
    long seconds = 0;
    int nanos = 0;
    if (CUMULATIVE_TYPES.contains(descriptorType)) {
      if (!lastUpdatedTime.containsKey(updateKey)) {
        // The aggregation has not reset since the exporter
        // has started up, so that is the start time
        seconds = exporterStartTime.getEpochSecond();
        nanos = exporterStartTime.getNano();
      } else {
        // The aggregation reset the last time it was exported
        // Add 1ms to guarantee there is no overlap from the previous export
        // (see https://cloud.google.com/monitoring/api/ref_v3/rpc/google.monitoring.v3#timeinterval)
        long lastUpdatedNanos = lastUpdatedTime.get(updateKey) + (long) 1e6;
        seconds = lastUpdatedNanos / NANO_PER_SECOND;
        nanos = (int) (lastUpdatedNanos % NANO_PER_SECOND);
      }
    } else {
      seconds = pointCollectionTime / NANO_PER_SECOND;
      nanos = (int) (pointCollectionTime % NANO_PER_SECOND);
    }
    Timestamp startTime = Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos).build();
    Timestamp endTime = Timestamp.newBuilder().setSeconds(pointCollectionTime / NANO_PER_SECOND)
        .setNanos((int) (pointCollectionTime % NANO_PER_SECOND)).build();
    pointBuilder.setInterval(TimeInterval.newBuilder().setStartTime(startTime).setEndTime(endTime).build());
    lastUpdatedTime.put(updateKey, pointCollectionTime);
  }

  private static LabelDescriptor mapConstantLabel(String key, String value) {
    LabelDescriptor.Builder builder = LabelDescriptor.newBuilder().setKey(key);
    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
      builder.setValueType(LabelDescriptor.ValueType.BOOL);
    } else if (Ints.tryParse(value) != null) {
      builder.setValueType(LabelDescriptor.ValueType.INT64);
    } else {
      builder.setValueType(LabelDescriptor.ValueType.STRING);
    }
    return builder.build();
  }

  private MonitoredResource mapToGcpMonitoredResource(Resource resource) {
    ReadableAttributes attributes = resource.getAttributes();
    if (attributes.get("cloud.provider") != null &&
        !attributes.get("cloud.provider").getStringValue().equals("gcp")) {
      return null;
    }
    String resourceType = attributes.get("gcp.resource_type").getStringValue();
    if (!(resourceType.equalsIgnoreCase("gce_instance") || resourceType.equalsIgnoreCase("gke_container"))) {
      return null;
    }

    MonitoredResource.Builder builder = MonitoredResource.newBuilder().setType(resourceType);
    for (Map.Entry<String, String> labels : OTEL_TO_GCP_LABELS.get(resourceType).entrySet()) {
      if (attributes.get(labels.getKey()) == null) {
        logger.log(WARNING, "Missing monitored resource value for {}", labels.getKey());
        continue;
      }
      builder.putLabels(labels.getValue(), attributes.get(labels.getKey()).getStringValue());
    }
    return builder.build();
  }

  private static void createTimeSeriesBatch(MetricServiceClient metricServiceClient, ProjectName projectName,
      List<TimeSeries> allTimesSeries) {
    List<List<TimeSeries>> batches = Lists.partition(allTimesSeries, MAX_BATCH_SIZE);
    for (List<TimeSeries> timeSeries : batches) {
      metricServiceClient.createTimeSeries(projectName, timeSeries);
    }
  }

  private static class MetricWithLabels {

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
