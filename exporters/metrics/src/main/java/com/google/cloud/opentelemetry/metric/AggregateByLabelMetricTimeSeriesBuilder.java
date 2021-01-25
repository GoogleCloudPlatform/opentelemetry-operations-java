package com.google.cloud.opentelemetry.metric;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.google.api.MetricDescriptor;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;

import io.opentelemetry.api.common.Labels;
import io.opentelemetry.sdk.metrics.data.DoublePoint;
import io.opentelemetry.sdk.metrics.data.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData;

import com.google.cloud.opentelemetry.metric.MetricExporter.MetricWithLabels;

import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapMetricDescriptor;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapInterval;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapMetric;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.mapResource;

public class AggregateByLabelMetricTimeSeriesBuilder implements MetricTimeSeriesBuilder {

    private final Map<String, MetricDescriptor> descriptors = new HashMap<>();
    private final Map<MetricWithLabels, TimeSeries.Builder> pendingTimeSeries = new HashMap<>();
    private final String projectId;


    public AggregateByLabelMetricTimeSeriesBuilder(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void recordPoint(MetricData metric, LongPoint point) {
        MetricDescriptor descriptor = mapMetricDescriptor(metric, point);
        if (descriptor == null) {
            return;
        }
        // TODO: Use actual unique key for descriptors, and deal with conflicts (or log)
        descriptors.putIfAbsent(descriptor.getName(), descriptor);
        MetricWithLabels key = new MetricWithLabels(descriptor.getType(), point.getLabels());
        // TODO: Check lastExportTime and ensure we don't send too often...
        pendingTimeSeries
          .computeIfAbsent(key, k -> makeTimeSeriesHeader(metric, point.getLabels(), descriptor))
          .addPoints(
            com.google.monitoring.v3.Point.newBuilder()
            .setValue(
                TypedValue.newBuilder().setInt64Value(point.getValue())
            ).setInterval(mapInterval(point, metric.getType())).build());
    }

    @Override
    public void recordPoint(MetricData metric, DoublePoint point) {
        MetricDescriptor descriptor = mapMetricDescriptor(metric, point);
        if (descriptor == null) {
            return;
        }
        // TODO: Use actual unique key for descriptors, and deal with conflicts (or log)
        descriptors.putIfAbsent(descriptor.getName(), descriptor);
        MetricWithLabels key = new MetricWithLabels(descriptor.getType(), point.getLabels());
        // TODO: Check lastExportTime and ensure we don't send too often...
        pendingTimeSeries
          .computeIfAbsent(key, k -> makeTimeSeriesHeader(metric, point.getLabels(), descriptor))
          .addPoints(
            com.google.monitoring.v3.Point.newBuilder()
            .setValue(
                TypedValue.newBuilder().setDoubleValue(point.getValue())
            ).setInterval(mapInterval(point, metric.getType())));
    }

    private TimeSeries.Builder makeTimeSeriesHeader(MetricData metric, Labels labels, MetricDescriptor descriptor) {
        return TimeSeries.newBuilder()
           .setMetric(mapMetric(labels, descriptor.getType()))
           .setMetricKind(descriptor.getMetricKind())
           .setResource(mapResource(projectId));
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
