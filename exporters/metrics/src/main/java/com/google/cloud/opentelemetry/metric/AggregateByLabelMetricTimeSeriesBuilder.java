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

import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapDistribution;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapInterval;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapMetric;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapMetricDescriptor;
import static com.google.cloud.opentelemetry.metric.ResourceTranslator.mapResource;

import com.google.api.MetricDescriptor;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds GCM TimeSeries from each OTEL metric point, creating metric descriptors based on the
 * "first" seen point for any given metric.
 */
public final class AggregateByLabelMetricTimeSeriesBuilder implements MetricTimeSeriesBuilder {

  public static final String LABEL_INSTRUMENTATION_SOURCE =
      "instrumentation_source"; // name of the instrumentation source
  public static final String LABEL_INSTRUMENTATION_VERSION =
      "instrumentation_version"; // version of the instrumentation source

  private final Map<String, MetricDescriptor> descriptors = new HashMap<>();
  private final Map<MetricWithLabels, TimeSeries.Builder> pendingTimeSeries = new HashMap<>();
  private final String projectId;
  private final String prefix;

  public AggregateByLabelMetricTimeSeriesBuilder(String projectId, String prefix) {
    this.projectId = projectId;
    this.prefix = prefix;
  }

  @Override
  public void recordPoint(MetricData metricData, LongPointData pointData) {
    recordPointInTimeSeries(
        metricData,
        pointData,
        Point.newBuilder()
            .setValue(TypedValue.newBuilder().setInt64Value(pointData.getValue()))
            .setInterval(mapInterval(pointData, metricData))
            .build());
  }

  @Override
  public void recordPoint(MetricData metricData, DoublePointData pointData) {
    recordPointInTimeSeries(
        metricData,
        pointData,
        Point.newBuilder()
            .setValue(TypedValue.newBuilder().setDoubleValue(pointData.getValue()))
            .setInterval(mapInterval(pointData, metricData))
            .build());
  }

  @Override
  public void recordPoint(MetricData metricData, HistogramPointData pointData) {
    recordPointInTimeSeries(
        metricData,
        pointData,
        Point.newBuilder()
            .setValue(
                TypedValue.newBuilder().setDistributionValue(mapDistribution(pointData, projectId)))
            .setInterval(mapInterval(pointData, metricData))
            .build());
  }

  private void recordPointInTimeSeries(MetricData metric, PointData point, Point builtPoint) {
    MetricDescriptor descriptor = mapMetricDescriptor(this.prefix, metric, point);
    if (descriptor == null) {
      // Unsupported type.
      return;
    }
    descriptors.putIfAbsent(descriptor.getType(), descriptor);
    Attributes metricAttributes =
        attachInstrumentationLibraryLabels(
            point.getAttributes(), metric.getInstrumentationScopeInfo());
    MetricWithLabels key = new MetricWithLabels(descriptor.getType(), metricAttributes);
    pendingTimeSeries
        .computeIfAbsent(key, k -> makeTimeSeriesHeader(metric, metricAttributes, descriptor))
        .addPoints(builtPoint);
  }

  private TimeSeries.Builder makeTimeSeriesHeader(
      MetricData metric, Attributes attributes, MetricDescriptor descriptor) {
    return TimeSeries.newBuilder()
        .setMetric(mapMetric(attributes, descriptor.getType()))
        .setMetricKind(descriptor.getMetricKind())
        .setResource(mapResource(metric.getResource()));
  }

  private Attributes attachInstrumentationLibraryLabels(
      Attributes attributes, InstrumentationScopeInfo instrumentationScopeInfo) {
    return attributes.toBuilder()
        .put(
            AttributeKey.stringKey(LABEL_INSTRUMENTATION_SOURCE),
            instrumentationScopeInfo.getName())
        .put(
            AttributeKey.stringKey(LABEL_INSTRUMENTATION_VERSION),
            Objects.requireNonNullElse(instrumentationScopeInfo.getVersion(), ""))
        .build();
  }

  @Override
  public Collection<MetricDescriptor> getDescriptors() {
    return descriptors.values();
  }

  @Override
  public List<TimeSeries> getTimeSeries() {
    return pendingTimeSeries.values().stream()
        .map(TimeSeries.Builder::build)
        .collect(Collectors.toList());
  }
}
