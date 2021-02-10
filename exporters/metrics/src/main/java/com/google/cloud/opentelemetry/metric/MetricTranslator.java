/*
 * Copyright 2021 Google
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

import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_GAUGE;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_SUM;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_GAUGE;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_SUM;

import com.google.api.LabelDescriptor;
import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.MonitoredResource;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Longs;
import com.google.monitoring.v3.TimeInterval;
import com.google.protobuf.Timestamp;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Labels;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
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

  static final Set<MetricDataType> GAUGE_TYPES = ImmutableSet.of(LONG_GAUGE, DOUBLE_GAUGE);
  static final Set<MetricDataType> CUMULATIVE_TYPES = ImmutableSet.of(LONG_SUM, DOUBLE_SUM);
  static final Set<MetricDataType> LONG_TYPES = ImmutableSet.of(LONG_GAUGE, LONG_SUM);
  static final Set<MetricDataType> DOUBLE_TYPES = ImmutableSet.of(DOUBLE_GAUGE, DOUBLE_SUM);
  private static final int MIN_TIMESTAMP_INTERVAL_NANOS = 1000000;

  // Mapping outlined at https://cloud.google.com/monitoring/api/resources#tag_gce_instance
  private static final Map<String, AttributeKey<String>> gceMap =
      Stream.of(
              new Object[][] {
                {"project_id", SemanticAttributes.CLOUD_ACCOUNT_ID},
                {"instance_id", SemanticAttributes.HOST_ID},
                {"zone", SemanticAttributes.CLOUD_ZONE}
              })
          .collect(
              Collectors.toMap(data -> (String) data[0], data -> (AttributeKey<String>) data[1]));

  // Mapping outlined at https://cloud.google.com/monitoring/api/resources#tag_gke_container
  private static final Map<String, AttributeKey<String>> gkeMap =
      Stream.of(
              new Object[][] {
                {"project_id", SemanticAttributes.CLOUD_ACCOUNT_ID},
                {"cluster_name", SemanticAttributes.K8S_CLUSTER_NAME},
                {"namespace_id", SemanticAttributes.K8S_NAMESPACE_NAME},
                {"instance_id", SemanticAttributes.HOST_ID},
                {"pod_id", SemanticAttributes.K8S_POD_NAME},
                {"container_name", SemanticAttributes.K8S_CONTAINER_NAME},
                {"zone", SemanticAttributes.CLOUD_ZONE}
              })
          .collect(
              Collectors.toMap(data -> (String) data[0], data -> (AttributeKey<String>) data[1]));

  static Metric mapMetric(Labels labels, String type) {
    Metric.Builder metricBuilder = Metric.newBuilder().setType(type);
    labels.forEach(metricBuilder::putLabels);
    return metricBuilder.build();
  }

  static MetricDescriptor mapMetricDescriptor(
      MetricData metric, io.opentelemetry.sdk.metrics.data.Point metricPoint) {
    MetricDescriptor.Builder builder =
        MetricDescriptor.newBuilder()
            .setDisplayName(metric.getName())
            .setDescription(metric.getDescription())
            .setType(mapMetricType(metric.getName()))
            .setUnit(metric.getUnit());
    metricPoint.getLabels().forEach((key, value) -> builder.addLabels(mapLabel(key, value)));

    MetricDataType metricType = metric.getType();
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

  static TimeInterval mapInterval(
      io.opentelemetry.sdk.metrics.data.Point point, MetricDataType metricType) {
    Timestamp startTime = mapTimestamp(point.getStartEpochNanos());
    Timestamp endTime = mapTimestamp(point.getEpochNanos());
    if (GAUGE_TYPES.contains(metricType)) {
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

  static MonitoredResource mapResource(Resource resource, String projectId) {
    // First, we try to map to known GCP resources
    Attributes attributes = resource.getAttributes();

    // GCE: https://cloud.google.com/monitoring/api/resources#tag_gce_instance
    String provider = attributes.get(SemanticAttributes.CLOUD_PROVIDER);
    if (provider != null && provider.equals(SemanticAttributes.CloudProviderValues.GCP)) {
      String namespace = attributes.get(SemanticAttributes.K8S_NAMESPACE_NAME);
      if (namespace != null) {
        return MonitoredResource.newBuilder()
            .setType("gke_container")
            .putAllLabels(
                gkeMap.entrySet().stream()
                    .collect(
                        Collectors.toMap(
                            e -> (String) e.getKey(), e -> attributes.get(e.getValue()))))
            .build();
      }
      return MonitoredResource.newBuilder()
          .setType("gce_instance")
          .putAllLabels(
              gceMap.entrySet().stream()
                  .collect(
                      Collectors.toMap(
                          e -> (String) e.getKey(), e -> attributes.get(e.getValue()))))
          .build();
    }

    // If none of the standard resource types apply, use the "global" resource:
    // https://cloud.google.com/monitoring/api/resources#tag_global
    return MonitoredResource.newBuilder()
        .setType(DEFAULT_RESOURCE_TYPE)
        .putLabels(RESOURCE_PROJECT_ID_LABEL, projectId)
        .build();
  }

  private static Timestamp mapTimestamp(long epochNanos) {
    return Timestamp.newBuilder()
        .setSeconds(epochNanos / NANO_PER_SECOND)
        .setNanos((int) (epochNanos % NANO_PER_SECOND))
        .build();
  }
}
