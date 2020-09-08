package com.google.cloud.opentelemetry.metric;

import static io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type.MONOTONIC_DOUBLE;
import static io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type.MONOTONIC_LONG;
import static io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type.NON_MONOTONIC_DOUBLE;
import static io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type.NON_MONOTONIC_LONG;

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
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricTranslator {

  private static final Logger logger = LoggerFactory.getLogger(MetricTranslator.class);

  static final String DESCRIPTOR_TYPE_URL = "custom.googleapis.com/OpenTelemetry/";
  static final Set<String> KNOWN_DOMAINS = ImmutableSet
      .of("googleapis.com", "kubernetes.io", "istio.io", "knative.dev");
  static final long NANO_PER_SECOND = (long) 1e9;

  static final Set<Type> GAUGE_TYPES = ImmutableSet.of(NON_MONOTONIC_LONG, NON_MONOTONIC_DOUBLE);
  static final Set<MetricData.Descriptor.Type> CUMULATIVE_TYPES = ImmutableSet
      .of(MONOTONIC_LONG, MONOTONIC_DOUBLE);
  static final Set<MetricData.Descriptor.Type> LONG_TYPES = ImmutableSet.of(NON_MONOTONIC_LONG, MONOTONIC_LONG);
  static final Set<MetricData.Descriptor.Type> DOUBLE_TYPES = ImmutableSet
      .of(NON_MONOTONIC_DOUBLE, MONOTONIC_DOUBLE);

  static Metric mapMetric(MetricData metric, String type) {
    Metric.Builder metricBuilder = Metric.newBuilder().setType(type);
    metric.getDescriptor().getConstantLabels().forEach(metricBuilder::putLabels);
    return metricBuilder.build();
  }

  static MetricDescriptor mapMetricDescriptor(MetricData metric) {
    String instrumentName = metric.getInstrumentationLibraryInfo().getName();
    MetricDescriptor.Builder builder = MetricDescriptor.newBuilder().setDisplayName(instrumentName)
        .setType(mapMetricType(instrumentName));
    metric.getDescriptor().getConstantLabels().forEach((key, value) -> builder.addLabels(mapConstantLabel(key, value)));

    MetricData.Descriptor.Type metricType = metric.getDescriptor().getType();
    if (GAUGE_TYPES.contains(metricType)) {
      builder.setMetricKind(MetricDescriptor.MetricKind.GAUGE);
    } else if (CUMULATIVE_TYPES.contains(metricType)) {
      builder.setMetricKind(MetricDescriptor.MetricKind.CUMULATIVE);
    } else {
      logger.error("Metric type {} not supported", metricType);
      return null;
    }
    if (LONG_TYPES.contains(metricType)) {
      builder.setValueType(MetricDescriptor.ValueType.INT64);
    } else if (DOUBLE_TYPES.contains(metricType)) {
      builder.setValueType(MetricDescriptor.ValueType.DOUBLE);
    } else {
      logger.error("Metric type {} not supported", metricType);
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

  static LabelDescriptor mapConstantLabel(String key, String value) {
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

  static Point mapPoint(Map<MetricWithLabels, Long> lastUpdatedTime, MetricData metric, MetricWithLabels updateKey,
      Instant exporterStartTime, long pointCollectionTime) {
    Builder pointBuilder = Point.newBuilder();
    Type type = metric.getDescriptor().getType();
    if (LONG_TYPES.contains(type)) {
      pointBuilder.setValue(TypedValue.newBuilder().setInt64Value(
          ((MetricData.LongPoint) metric.getPoints().iterator().next()).getValue()));
    } else if (DOUBLE_TYPES.contains(type)) {
      pointBuilder.setValue(TypedValue.newBuilder().setDoubleValue(
          ((MetricData.DoublePoint) metric.getPoints().iterator().next()).getValue()));
    } else {
      logger.error("Type {} not supported", type);
      return null;
    }
    pointBuilder.setInterval(
        mapInterval(lastUpdatedTime, updateKey, type, exporterStartTime, pointCollectionTime));
    return pointBuilder.build();
  }

  static TimeInterval mapInterval(Map<MetricWithLabels, Long> lastUpdatedTime,
      MetricWithLabels updateKey, Type descriptorType, Instant exporterStartTime, long pointCollectionTime) {
    long seconds;
    int nanos;
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
    lastUpdatedTime.put(updateKey, pointCollectionTime);
    return TimeInterval.newBuilder().setStartTime(startTime).setEndTime(endTime).build();
  }
}
