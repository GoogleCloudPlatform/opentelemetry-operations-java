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

import com.google.api.Distribution;
import com.google.api.Distribution.BucketOptions;
import com.google.api.Distribution.BucketOptions.Explicit;
import com.google.api.LabelDescriptor;
import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.common.collect.ImmutableSet;
import com.google.monitoring.v3.DroppedLabels;
import com.google.monitoring.v3.SpanContext;
import com.google.monitoring.v3.TimeInterval;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.ExemplarData;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricTranslator {

  private static final Logger logger = LoggerFactory.getLogger(MetricTranslator.class);

  static final String DESCRIPTOR_TYPE_URL = "custom.googleapis.com/OpenTelemetry/";
  static final Set<String> KNOWN_DOMAINS =
      ImmutableSet.of("googleapis.com", "kubernetes.io", "istio.io", "knative.dev");
  private static final String DEFAULT_RESOURCE_TYPE = "global";
  private static final String RESOURCE_PROJECT_ID_LABEL = "project_id";
  static final long NANO_PER_SECOND = (long) 1e9;
  static final String METRIC_DESCRIPTOR_TIME_UNIT = "ns";
  private static final int MIN_TIMESTAMP_INTERVAL_NANOS = 1000000;

  // Mapping outlined at https://cloud.google.com/monitoring/api/resources#tag_gce_instance
  private static final Map<String, AttributeKey<String>> gceMap =
      Stream.of(
              new Object[][] {
                {"project_id", ResourceAttributes.CLOUD_ACCOUNT_ID},
                {"instance_id", ResourceAttributes.HOST_ID},
                {"zone", ResourceAttributes.CLOUD_AVAILABILITY_ZONE}
              })
          .collect(
              Collectors.toMap(data -> (String) data[0], data -> (AttributeKey<String>) data[1]));

  // Mapping outlined at https://cloud.google.com/monitoring/api/resources#tag_gke_container
  private static final Map<String, AttributeKey<String>> gkeMap =
      Stream.of(
              new Object[][] {
                {"project_id", ResourceAttributes.CLOUD_ACCOUNT_ID},
                {"cluster_name", ResourceAttributes.K8S_CLUSTER_NAME},
                {"namespace_id", ResourceAttributes.K8S_NAMESPACE_NAME},
                {"instance_id", ResourceAttributes.HOST_ID},
                {"pod_id", ResourceAttributes.K8S_POD_NAME},
                {"container_name", ResourceAttributes.K8S_CONTAINER_NAME},
                {"zone", ResourceAttributes.CLOUD_AVAILABILITY_ZONE}
              })
          .collect(
              Collectors.toMap(data -> (String) data[0], data -> (AttributeKey<String>) data[1]));

  static Metric mapMetric(Attributes attributes, String type) {
    Metric.Builder metricBuilder = Metric.newBuilder().setType(type);
    attributes.forEach(
        (key, value) -> metricBuilder.putLabels(cleanAttributeKey(key.getKey()), value.toString()));
    return metricBuilder.build();
  }

  static String cleanAttributeKey(String key) {
    // . is commonly used in OTel but disallowed in GCM label names,
    // https://cloud.google.com/monitoring/api/ref_v3/rest/v3/LabelDescriptor#:~:text=Matches%20the%20following%20regular%20expression%3A
    return key.replace('.', '_');
  }

  static MetricDescriptor mapMetricDescriptor(
      MetricData metric, io.opentelemetry.sdk.metrics.data.PointData metricPoint) {
    MetricDescriptor.Builder builder =
        MetricDescriptor.newBuilder()
            .setDisplayName(metric.getName())
            .setDescription(metric.getDescription())
            .setType(mapMetricType(metric.getName()))
            .setUnit(metric.getUnit());
    metricPoint
        .getAttributes()
        .forEach((key, value) -> builder.addLabels(mapAttribute(key, value)));

    MetricDataType metricType = metric.getType();
    switch (metricType) {
      case LONG_GAUGE:
        builder.setMetricKind(MetricDescriptor.MetricKind.GAUGE);
        builder.setValueType(MetricDescriptor.ValueType.INT64);
        return builder.build();
      case DOUBLE_GAUGE:
        builder.setMetricKind(MetricDescriptor.MetricKind.GAUGE);
        builder.setValueType(MetricDescriptor.ValueType.DOUBLE);
        return builder.build();
      case LONG_SUM:
        builder.setValueType(MetricDescriptor.ValueType.INT64);
        return fillSumType(metric.getLongSumData(), builder);
      case DOUBLE_SUM:
        builder.setValueType(MetricDescriptor.ValueType.DOUBLE);
        return fillSumType(metric.getDoubleSumData(), builder);
      case HISTOGRAM:
        return fillHistogramType(metric.getHistogramData(), builder);
      default:
        logger.error(
            "Metric type {} not supported. Only gauge and cumulative types are supported.",
            metricType);
    }
    return null;
  }

  private static MetricDescriptor fillHistogramType(
      HistogramData histogram, MetricDescriptor.Builder builder) {
    builder.setValueType(MetricDescriptor.ValueType.DISTRIBUTION);
    switch (histogram.getAggregationTemporality()) {
      case CUMULATIVE:
        builder.setMetricKind(MetricDescriptor.MetricKind.CUMULATIVE);
        return builder.build();
      default:
        logger.error(
            "Histogram type {} not supported. Only cumulative types are supported.", histogram);
        return null;
    }
  }

  private static MetricDescriptor fillSumType(SumData<?> sum, MetricDescriptor.Builder builder) {
    // Treat non-monotonic sums as gauges.
    if (!sum.isMonotonic()) {
      builder.setMetricKind(MetricDescriptor.MetricKind.GAUGE);
      return builder.build();
    }
    switch (sum.getAggregationTemporality()) {
      case CUMULATIVE:
        builder.setMetricKind(MetricDescriptor.MetricKind.CUMULATIVE);
        return builder.build();
      default:
        logger.error("Sum type {} not supported. Only cumulative types are supported.", sum);
        return null;
    }
  }

  private static String mapMetricType(String instrumentName) {
    for (String domain : KNOWN_DOMAINS) {
      if (instrumentName.contains(domain)) {
        return instrumentName;
      }
    }
    return DESCRIPTOR_TYPE_URL + instrumentName;
  }

  static <T> LabelDescriptor mapAttribute(AttributeKey<T> key, Object value) {
    LabelDescriptor.Builder builder =
        LabelDescriptor.newBuilder().setKey(cleanAttributeKey(key.getKey()));
    switch (key.getType()) {
      case BOOLEAN:
        builder.setValueType(LabelDescriptor.ValueType.BOOL);
        break;
      case LONG:
        builder.setValueType(LabelDescriptor.ValueType.INT64);
        break;
      default:
        // All other attribute types will be toString'd
        builder.setValueType(LabelDescriptor.ValueType.STRING);
        break;
    }
    return builder.build();
  }

  /** Returns true if the metric should be treated as a Gauge by cloud monitoring. */
  static boolean isGauge(MetricData metric) {
    switch (metric.getType()) {
      case LONG_GAUGE:
      case DOUBLE_GAUGE:
        return true;
      case LONG_SUM:
        return !metric.getLongSumData().isMonotonic();
      case DOUBLE_SUM:
        return !metric.getDoubleSumData().isMonotonic();
      default:
        return false;
    }
  }

  static TimeInterval mapInterval(
      io.opentelemetry.sdk.metrics.data.PointData point, MetricData metric) {
    Timestamp startTime = mapTimestamp(point.getStartEpochNanos());
    Timestamp endTime = mapTimestamp(point.getEpochNanos());
    if (isGauge(metric)) {
      // The start time must be equal to the end time for the gauge metric
      startTime = endTime;
    } else if (TimeUnit.SECONDS.toNanos(startTime.getSeconds()) + startTime.getNanos()
        == TimeUnit.SECONDS.toNanos(endTime.getSeconds()) + endTime.getNanos()) {
      // The end time of a new interval must be at least a millisecond after the end time of the
      // previous interval, for all non-gauge types.
      // https://cloud.google.com/monitoring/api/ref_v3/rpc/google.monitoring.v3#timeinterval
      endTime =
          Timestamp.newBuilder()
              .setSeconds(endTime.getSeconds())
              .setNanos(endTime.getNanos() + MIN_TIMESTAMP_INTERVAL_NANOS)
              .build();
    }
    return TimeInterval.newBuilder().setStartTime(startTime).setEndTime(endTime).build();
  }

  private static Timestamp mapTimestamp(long epochNanos) {
    return Timestamp.newBuilder()
        .setSeconds(epochNanos / NANO_PER_SECOND)
        .setNanos((int) (epochNanos % NANO_PER_SECOND))
        .build();
  }

  static Distribution.Builder mapDistribution(HistogramPointData point, String projectId) {
    return Distribution.newBuilder()
        .setCount(point.getCount())
        .setMean(point.getSum() / point.getCount())
        .setBucketOptions(
            BucketOptions.newBuilder()
                .setExplicitBuckets(Explicit.newBuilder().addAllBounds(point.getBoundaries())))
        .addAllBucketCounts(point.getCounts())
        .addAllExemplars(
            point.getExemplars().stream()
                .map(e -> mapExemplar(e, projectId))
                .collect(Collectors.toList()));
  }

  private static Distribution.Exemplar mapExemplar(ExemplarData exemplar, String projectId) {
    Distribution.Exemplar.Builder exemplarBuilder =
        Distribution.Exemplar.newBuilder()
            .setValue(exemplar.getValueAsDouble())
            .setTimestamp(mapTimestamp(exemplar.getEpochNanos()));
    if (exemplar.getSpanId() != null && exemplar.getTraceId() != null) {
      exemplarBuilder.addAttachments(
          Any.pack(
              SpanContext.newBuilder()
                  .setSpanName(makeSpanName(projectId, exemplar.getTraceId(), exemplar.getSpanId()))
                  .build()));
    }
    if (!exemplar.getFilteredAttributes().isEmpty()) {
      exemplarBuilder.addAttachments(
          Any.pack(mapFilteredAttributes(exemplar.getFilteredAttributes())));
    }
    return exemplarBuilder.build();
  }

  private static String makeSpanName(String projectId, String traceId, String spanId) {
    return String.format("projects/%s/traces/%s/spans/%s", projectId, traceId, spanId);
  }

  private static DroppedLabels mapFilteredAttributes(Attributes attributes) {
    DroppedLabels.Builder labels = DroppedLabels.newBuilder();
    attributes.forEach((k, v) -> labels.putLabel(cleanAttributeKey(k.getKey()), v.toString()));
    return labels.build();
  }
}
