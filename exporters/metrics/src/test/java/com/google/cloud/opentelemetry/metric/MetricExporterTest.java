package com.google.cloud.opentelemetry.metric;

import static com.google.cloud.opentelemetry.metric.FakeData.aFakeCredential;
import static com.google.cloud.opentelemetry.metric.FakeData.aGceResource;
import static com.google.cloud.opentelemetry.metric.FakeData.aLongPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aMetricData;
import static com.google.cloud.opentelemetry.metric.FakeData.aProjectId;
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

import com.google.api.LabelDescriptor;
import com.google.api.LabelDescriptor.ValueType;
import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.MetricDescriptor.MetricKind;
import com.google.api.MonitoredResource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.Timestamp;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.Type;
import java.io.IOException;
import java.util.ArrayList;
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
  public void testExportSucceeds() {
    MetricDescriptor expectedDescriptor =
        MetricDescriptor.newBuilder()
            .setDisplayName(aMetricData.getName())
            .setType(DESCRIPTOR_TYPE_URL + aMetricData.getInstrumentationLibraryInfo().getName())
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
            .setValue(TypedValue.newBuilder().setInt64Value(((LongPoint) aLongPoint).getValue()))
            .setInterval(expectedTimeInterval)
            .build();
    TimeSeries expectedTimeSeries =
        TimeSeries.newBuilder()
            .setMetric(
                Metric.newBuilder()
                    .setType(expectedDescriptor.getType())
                    .putLabels("label1", "value1")
                    .putLabels("label2", "False")
                    .build())
            .addPoints(expectedPoint)
            .setResource(
                MonitoredResource.newBuilder()
                    .putAllLabels(
                        ImmutableMap.of(
                            "cloud.account.id",
                            "123",
                            "cloud.provider",
                            "gcp",
                            "cloud.zone",
                            "US",
                            "extra_info",
                            "extra",
                            "host.id",
                            "host"))
                    .putLabels("not_gcp_resource", "value")
                    .build())
            .setMetricKind(expectedDescriptor.getMetricKind())
            .build();
    CreateMetricDescriptorRequest expectedRequest =
        CreateMetricDescriptorRequest.newBuilder()
            .setName("projects/" + aProjectId)
            .setMetricDescriptor(expectedDescriptor)
            .build();
    ProjectName expectedProjectName = ProjectName.of(aProjectId);

    MetricExporter exporter = MetricExporter.createWithClient(aProjectId, mockClient);

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
  public void testExportWithNonSupportedMetricTypeReturnsFailure() {
    MetricExporter exporter = MetricExporter.createWithClient(aProjectId, mockClient);

    MetricData metricData =
        MetricData.create(
            aGceResource,
            anInstrumentationLibraryInfo,
            "Metric Name",
            "description",
            "ns",
            Type.SUMMARY,
            ImmutableList.of(aLongPoint));

    CompletableResultCode result = exporter.export(ImmutableList.of(metricData));
    verify(mockClient, times(0)).createMetricDescriptor(any());
    verify(mockClient, times(0)).createTimeSeries(any(ProjectName.class), any());

    assertFalse(result.isSuccess());
  }
}
