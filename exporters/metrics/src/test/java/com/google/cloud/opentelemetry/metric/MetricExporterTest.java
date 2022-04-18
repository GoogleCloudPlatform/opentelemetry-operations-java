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

import static com.google.cloud.opentelemetry.metric.FakeData.aCloudZone;
import static com.google.cloud.opentelemetry.metric.FakeData.aDoubleSummaryPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aFakeCredential;
import static com.google.cloud.opentelemetry.metric.FakeData.aGceResource;
import static com.google.cloud.opentelemetry.metric.FakeData.aHistogram;
import static com.google.cloud.opentelemetry.metric.FakeData.aHistogramPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aHostId;
import static com.google.cloud.opentelemetry.metric.FakeData.aLongPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aMetricData;
import static com.google.cloud.opentelemetry.metric.FakeData.aProjectId;
import static com.google.cloud.opentelemetry.metric.FakeData.aSpanId;
import static com.google.cloud.opentelemetry.metric.FakeData.aTraceId;
import static com.google.cloud.opentelemetry.metric.FakeData.anInstrumentationLibraryInfo;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.DESCRIPTOR_TYPE_URL;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.METRIC_DESCRIPTOR_TIME_UNIT;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.NANO_PER_SECOND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.Distribution;
import com.google.api.Distribution.BucketOptions;
import com.google.api.Distribution.BucketOptions.Explicit;
import com.google.api.Distribution.Exemplar;
import com.google.api.LabelDescriptor;
import com.google.api.LabelDescriptor.ValueType;
import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.MetricDescriptor.MetricKind;
import com.google.api.MonitoredResource;
import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.DroppedLabels;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.SpanContext;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSummaryData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class MetricExporterTest {

  @Mock private CloudMetricClientImpl mockClient;

  @Captor private ArgumentCaptor<ArrayList<TimeSeries>> timeSeriesArgCaptor;

  @Captor private ArgumentCaptor<CreateMetricDescriptorRequest> metricDescriptorCaptor;

  @Captor private ArgumentCaptor<ProjectName> projectNameArgCaptor;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(mockClient.createMetricDescriptor(any())).thenReturn(null);
  }

  @Test
  public void testCreateWithConfigurationSucceeds() throws IOException {
    MetricConfiguration configuration =
        MetricConfiguration.builder()
            .setProjectId(aProjectId)
            .setCredentials(aFakeCredential)
            .build();
    MetricExporter exporter = MetricExporter.createWithConfiguration(configuration);
    assertNotNull(exporter);
  }

  @Test
  public void testExportSendsAllDescriptorsOnce() {
    MetricExporter exporter =
        MetricExporter.createWithClient(aProjectId, mockClient, MetricDescriptorStrategy.SEND_ONCE);
    CompletableResultCode result = exporter.export(ImmutableList.of(aMetricData, aHistogram));
    assertTrue(result.isSuccess());
    CompletableResultCode result2 = exporter.export(ImmutableList.of(aMetricData, aHistogram));
    assertTrue(result2.isSuccess());
    CompletableResultCode result3 = exporter.export(ImmutableList.of(aMetricData, aHistogram));
    assertTrue(result3.isSuccess());
    verify(mockClient, times(2)).createMetricDescriptor(metricDescriptorCaptor.capture());
    verify(mockClient, times(3))
        .createTimeSeries(projectNameArgCaptor.capture(), timeSeriesArgCaptor.capture());

    // We know two metrics were created, let's verify we got both we sent.
    Set<String> metricDescriptorTypes =
        metricDescriptorCaptor.getAllValues().stream()
            .map(d -> d.getMetricDescriptor().getType())
            .collect(Collectors.toSet());
    assertTrue(metricDescriptorTypes.contains(DESCRIPTOR_TYPE_URL + aMetricData.getName()));
    assertTrue(metricDescriptorTypes.contains(DESCRIPTOR_TYPE_URL + aHistogram.getName()));
  }

  @Test
  public void testExportSucceeds() {
    MetricDescriptor expectedDescriptor =
        MetricDescriptor.newBuilder()
            .setDisplayName(aMetricData.getName())
            .setType(DESCRIPTOR_TYPE_URL + aMetricData.getName())
            .addLabels(
                LabelDescriptor.newBuilder()
                    .setKey("label1")
                    .setValueType(ValueType.STRING)
                    .build())
            .addLabels(
                LabelDescriptor.newBuilder().setKey("label2").setValueType(ValueType.BOOL).build())
            .setMetricKind(MetricKind.CUMULATIVE)
            .setValueType(MetricDescriptor.ValueType.INT64)
            .setUnit(METRIC_DESCRIPTOR_TIME_UNIT)
            .setDescription(aMetricData.getDescription())
            .build();
    TimeInterval expectedTimeInterval =
        TimeInterval.newBuilder()
            .setStartTime(
                Timestamp.newBuilder()
                    .setSeconds(aLongPoint.getStartEpochNanos() / NANO_PER_SECOND)
                    .setNanos(0)
                    .build())
            .setEndTime(
                Timestamp.newBuilder()
                    .setSeconds(aLongPoint.getEpochNanos() / NANO_PER_SECOND)
                    .setNanos(0)
                    .build())
            .build();
    Point expectedPoint =
        Point.newBuilder()
            .setValue(TypedValue.newBuilder().setInt64Value(aLongPoint.getValue()))
            .setInterval(expectedTimeInterval)
            .build();
    TimeSeries expectedTimeSeries =
        TimeSeries.newBuilder()
            .setMetric(
                Metric.newBuilder()
                    .setType(expectedDescriptor.getType())
                    .putLabels("label1", "value1")
                    .putLabels("label2", "false")
                    .build())
            .addPoints(expectedPoint)
            .setMetricKind(expectedDescriptor.getMetricKind())
            .setResource(
                MonitoredResource.newBuilder()
                    .setType("gce_instance")
                    .putLabels("instance_id", aHostId)
                    .putLabels("zone", aCloudZone)
                    .build())
            .build();
    CreateMetricDescriptorRequest expectedRequest =
        CreateMetricDescriptorRequest.newBuilder()
            .setName("projects/" + aProjectId)
            .setMetricDescriptor(expectedDescriptor)
            .build();
    ProjectName expectedProjectName = ProjectName.of(aProjectId);

    MetricExporter exporter =
        MetricExporter.createWithClient(
            aProjectId, mockClient, MetricDescriptorStrategy.ALWAYS_SEND);

    CompletableResultCode result = exporter.export(ImmutableList.of(aMetricData));
    verify(mockClient, times(1)).createMetricDescriptor(metricDescriptorCaptor.capture());
    verify(mockClient, times(1))
        .createTimeSeries(projectNameArgCaptor.capture(), timeSeriesArgCaptor.capture());

    assertTrue(result.isSuccess());
    assertEquals(expectedRequest, metricDescriptorCaptor.getValue());
    assertEquals(expectedProjectName, projectNameArgCaptor.getValue());
    assertEquals(1, timeSeriesArgCaptor.getValue().size());
    assertEquals(expectedTimeSeries, timeSeriesArgCaptor.getValue().get(0));
  }

  @Test
  public void testExportWithHistogram_Succeeds() {
    MetricDescriptor expectedDescriptor =
        MetricDescriptor.newBuilder()
            .setDisplayName(aHistogram.getName())
            .setType(DESCRIPTOR_TYPE_URL + aHistogram.getName())
            .addLabels(
                LabelDescriptor.newBuilder().setKey("test").setValueType(ValueType.STRING).build())
            .setMetricKind(MetricKind.CUMULATIVE)
            .setValueType(MetricDescriptor.ValueType.DISTRIBUTION)
            .setUnit(aHistogram.getUnit())
            .setDescription(aHistogram.getDescription())
            .build();
    ProjectName expectedProjectName = ProjectName.of(aProjectId);
    TimeInterval expectedTimeInterval =
        TimeInterval.newBuilder()
            .setStartTime(
                Timestamp.newBuilder()
                    .setSeconds(aHistogramPoint.getStartEpochNanos() / NANO_PER_SECOND)
                    .setNanos(0)
                    .build())
            .setEndTime(
                Timestamp.newBuilder()
                    .setSeconds(aHistogramPoint.getEpochNanos() / NANO_PER_SECOND)
                    .setNanos(1)
                    .build())
            .build();
    Point expectedPoint =
        Point.newBuilder()
            .setValue(
                TypedValue.newBuilder()
                    .setDistributionValue(
                        Distribution.newBuilder()
                            .setCount(3)
                            .setMean(1)
                            .addAllBucketCounts(ImmutableList.of(1L, 2L))
                            .setBucketOptions(
                                BucketOptions.newBuilder()
                                    .setExplicitBuckets(Explicit.newBuilder().addBounds(1).build())
                                    .build())
                            .addExemplars(
                                Exemplar.newBuilder()
                                    .setValue(3)
                                    .setTimestamp(
                                        Timestamp.newBuilder().setSeconds(0).setNanos(2).build())
                                    .addAttachments(
                                        Any.pack(
                                            SpanContext.newBuilder()
                                                .setSpanName(
                                                    "projects/"
                                                        + aProjectId
                                                        + "/traces/"
                                                        + aTraceId
                                                        + "/spans/"
                                                        + aSpanId)
                                                .build()))
                                    .addAttachments(
                                        Any.pack(
                                            DroppedLabels.newBuilder()
                                                .putLabel("test2", "two")
                                                .build()))
                                    .build())
                            .build()))
            .setInterval(expectedTimeInterval)
            .build();
    TimeSeries expectedTimeSeries =
        TimeSeries.newBuilder()
            .setMetric(
                Metric.newBuilder()
                    .setType(expectedDescriptor.getType())
                    .putLabels("test", "one")
                    .build())
            .addPoints(expectedPoint)
            .setMetricKind(expectedDescriptor.getMetricKind())
            .setResource(
                MonitoredResource.newBuilder()
                    .setType("gce_instance")
                    .putLabels("instance_id", aHostId)
                    .putLabels("zone", aCloudZone)
                    .build())
            .build();
    MetricExporter exporter =
        MetricExporter.createWithClient(
            aProjectId, mockClient, MetricDescriptorStrategy.ALWAYS_SEND);
    CompletableResultCode result = exporter.export(ImmutableList.of(aHistogram));
    verify(mockClient, times(1)).createMetricDescriptor(metricDescriptorCaptor.capture());
    verify(mockClient, times(1))
        .createTimeSeries(projectNameArgCaptor.capture(), timeSeriesArgCaptor.capture());
    assertTrue(result.isSuccess());
    assertEquals(expectedDescriptor, metricDescriptorCaptor.getValue().getMetricDescriptor());
    assertEquals(expectedProjectName, projectNameArgCaptor.getValue());
    assertEquals(1, timeSeriesArgCaptor.getValue().size());
    assertEquals(expectedTimeSeries, timeSeriesArgCaptor.getValue().get(0));
  }

  @Test
  public void testExportWithNonSupportedMetricTypeReturnsFailure() {
    MetricExporter exporter =
        MetricExporter.createWithClient(
            aProjectId, mockClient, MetricDescriptorStrategy.ALWAYS_SEND);

    MetricData metricData =
        MetricData.createDoubleSummary(
            aGceResource,
            anInstrumentationLibraryInfo,
            "Metric Name",
            "description",
            "ns",
            ImmutableSummaryData.create(ImmutableList.of(aDoubleSummaryPoint)));

    CompletableResultCode result = exporter.export(ImmutableList.of(metricData));
    verify(mockClient, times(0)).createMetricDescriptor(any());
    verify(mockClient, times(0)).createTimeSeries(any(ProjectName.class), any());

    assertFalse(result.isSuccess());
  }
}
