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
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapDistribution;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapInterval;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapMetric;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapMetricDescriptor;
import static com.google.cloud.opentelemetry.metric.ResourceTranslator.mapResource;

/**
 * Builds GCM TimeSeries from each OTEL metric point, creating metric descriptors based on the
 * "first" seen point for any given metric.
 */
public final class AggregateByLabelMetricTimeSeriesBuilder implements MetricTimeSeriesBuilder {

  private static final String INSTRUMENTATION_LIBRARY_NAME = "instrumentation_source";
  private static final String INSTRUMENTATION_LIBRARY_VERSION = "instrumentation_version";

  private final Map<String, MetricDescriptor> descriptors = new HashMap<>();
  private final Map<MetricWithLabels, TimeSeries.Builder> pendingTimeSeries = new HashMap<>();
  private final String projectId;
  private final String prefix;

  public AggregateByLabelMetricTimeSeriesBuilder(String projectId, String prefix) {
    this.projectId = projectId;
    this.prefix = prefix;
  }

  @Override
  public void recordPoint(MetricData metric, LongPointData point) {
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
    // TODO: Check lastExportTime and ensure we don't send too often...
    pendingTimeSeries
        .computeIfAbsent(key, k -> makeTimeSeriesHeader(metric, metricAttributes, descriptor))
        .addPoints(
            com.google.monitoring.v3.Point.newBuilder()
                .setValue(TypedValue.newBuilder().setInt64Value(point.getValue()))
                .setInterval(mapInterval(point, metric))
                .build());
  }

  @Override
  public void recordPoint(MetricData metric, DoublePointData point) {
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
    // TODO: Check lastExportTime and ensure we don't send too often...
    pendingTimeSeries
        .computeIfAbsent(key, k -> makeTimeSeriesHeader(metric, metricAttributes, descriptor))
        .addPoints(
            com.google.monitoring.v3.Point.newBuilder()
                .setValue(TypedValue.newBuilder().setDoubleValue(point.getValue()))
                .setInterval(mapInterval(point, metric)));
  }

  @Override
  public void recordPoint(MetricData metric, HistogramPointData point) {
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
        .addPoints(
            com.google.monitoring.v3.Point.newBuilder()
                .setValue(
                    TypedValue.newBuilder().setDistributionValue(mapDistribution(point, projectId)))
                .setInterval(mapInterval(point, metric)));
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
            AttributeKey.stringKey(INSTRUMENTATION_LIBRARY_NAME),
            instrumentationScopeInfo.getName())
        .put(
            AttributeKey.stringKey(INSTRUMENTATION_LIBRARY_VERSION),
            Objects.requireNonNullElse(instrumentationScopeInfo.getVersion(), ""))
        .build();
  }

  @Override
  public Collection<MetricDescriptor> getDescriptors() {
    return descriptors.values();
  }

  @Override
  public List<TimeSeries> getTimeSeries() {
    return pendingTimeSeries.values().stream().map(b -> b.build()).collect(Collectors.toList());
  }
}
