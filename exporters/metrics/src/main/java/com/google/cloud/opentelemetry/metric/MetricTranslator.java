package com.google.cloud.opentelemetry.metric;

import static io.opentelemetry.sdk.metrics.data.MetricData.Type.MONOTONIC_DOUBLE;
import static io.opentelemetry.sdk.metrics.data.MetricData.Type.MONOTONIC_LONG;
import static io.opentelemetry.sdk.metrics.data.MetricData.Type.NON_MONOTONIC_DOUBLE;
import static io.opentelemetry.sdk.metrics.data.MetricData.Type.NON_MONOTONIC_LONG;

import com.google.api.LabelDescriptor;
import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.cloud.opentelemetry.metric.MetricExporter.MetricWithLabels;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Longs;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.Point.Builder;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.Timestamp;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.Type;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricTranslator {

  private static final Logger logger = LoggerFactory.getLogger(MetricTranslator.class);

  static final String DESCRIPTOR_TYPE_URL = "custom.googleapis.com/OpenTelemetry/";
  static final Set<String> KNOWN_DOMAINS =
      ImmutableSet.of("googleapis.com", "kubernetes.io", "istio.io", "knative.dev");
  static final long NANO_PER_SECOND = (long) 1e9;
  static final String METRIC_DESCRIPTOR_TIME_UNIT = "ns";

  static final Set<Type> GAUGE_TYPES = ImmutableSet.of(NON_MONOTONIC_LONG, NON_MONOTONIC_DOUBLE);
  static final Set<Type> CUMULATIVE_TYPES = ImmutableSet.of(MONOTONIC_LONG, MONOTONIC_DOUBLE);
  static final Set<Type> LONG_TYPES = ImmutableSet.of(NON_MONOTONIC_LONG, MONOTONIC_LONG);
  static final Set<Type> DOUBLE_TYPES = ImmutableSet.of(NON_MONOTONIC_DOUBLE, MONOTONIC_DOUBLE);

  static Metric mapMetric(MetricData.Point metricPoint, String type) {
    Metric.Builder metricBuilder = Metric.newBuilder().setType(type);
    metricPoint.getLabels().forEach(metricBuilder::putLabels);
    return metricBuilder.build();
  }

  static MetricDescriptor mapMetricDescriptor(MetricData metric, MetricData.Point metricPoint) {
    MetricDescriptor.Builder builder =
        MetricDescriptor.newBuilder()
            .setDisplayName(metric.getName())
            .setDescription(metric.getDescription())
            .setType(mapMetricType(metric.getName()))
            .setUnit(metric.getUnit());
    metricPoint.getLabels().forEach((key, value) -> builder.addLabels(mapLabel(key, value)));

    Type metricType = metric.getType();
    if (GAUGE_TYPES.contains(metricType)) {
      builder.setMetricKind(MetricDescriptor.MetricKind.GAUGE);
    } else if (CUMULATIVE_TYPES.contains(metricType)) {
      builder.setMetricKind(MetricDescriptor.MetricKind.CUMULATIVE);
    } else {
      logger.error(
          "Metric type {} not supported. Only gauge and cumulative types are supported.",
          metricType);
      return null;
    }
    if (LONG_TYPES.contains(metricType)) {
      builder.setValueType(MetricDescriptor.ValueType.INT64);
    } else if (DOUBLE_TYPES.contains(metricType)) {
      builder.setValueType(MetricDescriptor.ValueType.DOUBLE);
    } else {
      logger.error(
          "Metric type {} not supported. Only long and double types are supported.", metricType);
      return null;
    }
    return builder.build();
  }

  private static String mapMetricType(String instrumentName) {
    for (String domain : KNOWN_DOMAINS) {
      if (instrumentName.contains(domain)) {
        return instrumentName;
      }
    }
    return DESCRIPTOR_TYPE_URL + instrumentName;
  }

  static LabelDescriptor mapLabel(String key, String value) {
    LabelDescriptor.Builder builder = LabelDescriptor.newBuilder().setKey(key);
    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
      builder.setValueType(LabelDescriptor.ValueType.BOOL);
    } else if (Longs.tryParse(value) != null) {
      builder.setValueType(LabelDescriptor.ValueType.INT64);
    } else {
      builder.setValueType(LabelDescriptor.ValueType.STRING);
    }
    return builder.build();
  }

  static Point mapPoint(
      MetricData metric,
      MetricData.Point point,
      MetricWithLabels updateKey,
      Map<MetricWithLabels, Long> lastUpdatedTime) {
    Builder pointBuilder = Point.newBuilder();
    Type type = metric.getType();
    if (LONG_TYPES.contains(type)) {
      pointBuilder.setValue(
          TypedValue.newBuilder().setInt64Value(((MetricData.LongPoint) point).getValue()));
    } else if (DOUBLE_TYPES.contains(type)) {
      pointBuilder.setValue(
          TypedValue.newBuilder().setDoubleValue(((MetricData.DoublePoint) point).getValue()));
    } else {
      logger.error("Type {} not supported", type);
      return null;
    }
    pointBuilder.setInterval(mapInterval(point, type));
    lastUpdatedTime.put(updateKey, point.getEpochNanos());
    return pointBuilder.build();
  }

  static TimeInterval mapInterval(MetricData.Point point, Type metricType) {
    Timestamp startTime = mapTimestamp(point.getStartEpochNanos());
    Timestamp endTime = mapTimestamp(point.getEpochNanos());
    if (GAUGE_TYPES.contains(metricType)) {
      // The start time must be equal to the end time for the gauge metric
      startTime = endTime;
    }
    return TimeInterval.newBuilder().setStartTime(startTime).setEndTime(endTime).build();
  }

  private static Timestamp mapTimestamp(long epochNanos) {
    return Timestamp.newBuilder()
        .setSeconds(epochNanos / NANO_PER_SECOND)
        .setNanos((int) (epochNanos % NANO_PER_SECOND))
        .build();
  }
}
