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

import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapDistribution;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapInterval;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapMetric;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapMetricDescriptor;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapResource;

import com.google.api.MetricDescriptor;
import com.google.cloud.opentelemetry.metric.MetricExporter.MetricWithLabels;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.DoubleHistogramPointData;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AggregateByLabelMetricTimeSeriesBuilder implements MetricTimeSeriesBuilder {

  private final Map<String, MetricDescriptor> descriptors = new HashMap<>();
  private final Map<MetricWithLabels, TimeSeries.Builder> pendingTimeSeries = new HashMap<>();
  private final String projectId;

  public AggregateByLabelMetricTimeSeriesBuilder(String projectId) {
    this.projectId = projectId;
  }

  @Override
  public void recordPoint(MetricData metric, LongPointData point) {
    MetricDescriptor descriptor = mapMetricDescriptor(metric, point);
    if (descriptor == null) {
      return;
    }
    // TODO: Use actual unique key for descriptors, and deal with conflicts (or log)
    descriptors.putIfAbsent(descriptor.getName(), descriptor);
    MetricWithLabels key = new MetricWithLabels(descriptor.getType(), point.getAttributes());
    // TODO: Check lastExportTime and ensure we don't send too often...
    pendingTimeSeries
        .computeIfAbsent(key, k -> makeTimeSeriesHeader(metric, point.getAttributes(), descriptor))
        .addPoints(
            com.google.monitoring.v3.Point.newBuilder()
                .setValue(TypedValue.newBuilder().setInt64Value(point.getValue()))
                .setInterval(mapInterval(point, metric.getType()))
                .build());
  }

  @Override
  public void recordPoint(MetricData metric, DoublePointData point) {
    MetricDescriptor descriptor = mapMetricDescriptor(metric, point);
    if (descriptor == null) {
      return;
    }
    // TODO: Use actual unique key for descriptors, and deal with conflicts (or log)
    descriptors.putIfAbsent(descriptor.getName(), descriptor);
    MetricWithLabels key = new MetricWithLabels(descriptor.getType(), point.getAttributes());
    // TODO: Check lastExportTime and ensure we don't send too often...
    pendingTimeSeries
        .computeIfAbsent(key, k -> makeTimeSeriesHeader(metric, point.getAttributes(), descriptor))
        .addPoints(
            com.google.monitoring.v3.Point.newBuilder()
                .setValue(TypedValue.newBuilder().setDoubleValue(point.getValue()))
                .setInterval(mapInterval(point, metric.getType())));
  }

  @Override
  public void recordPoint(MetricData metric, DoubleHistogramPointData point) {
    MetricDescriptor descriptor = mapMetricDescriptor(metric, point);
    if (descriptor == null) {
      return;
    }
    // TODO: Use actual unique key for descriptors, and deal with conflicts (or log)
    descriptors.putIfAbsent(descriptor.getName(), descriptor);
    MetricWithLabels key = new MetricWithLabels(descriptor.getType(), point.getAttributes());
    pendingTimeSeries
        .computeIfAbsent(key, k -> makeTimeSeriesHeader(metric, point.getAttributes(), descriptor))
        .addPoints(
            com.google.monitoring.v3.Point.newBuilder()
                .setValue(
                    TypedValue.newBuilder().setDistributionValue(mapDistribution(point, projectId)))
                .setInterval(mapInterval(point, metric.getType())));
  }

  private TimeSeries.Builder makeTimeSeriesHeader(
      MetricData metric, Attributes attributes, MetricDescriptor descriptor) {
    return TimeSeries.newBuilder()
        .setMetric(mapMetric(attributes, descriptor.getType()))
        .setMetricKind(descriptor.getMetricKind())
        .setResource(mapResource(metric.getResource(), projectId));
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
