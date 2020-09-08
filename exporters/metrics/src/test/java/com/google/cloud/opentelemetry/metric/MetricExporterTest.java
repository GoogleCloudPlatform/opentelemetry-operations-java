package com.google.cloud.opentelemetry.metric;

import static com.google.cloud.opentelemetry.metric.FakeData.aFakeCredential;
import static com.google.cloud.opentelemetry.metric.FakeData.aFakeProjectId;
import static com.google.cloud.opentelemetry.metric.FakeData.aGceResource;
import static com.google.cloud.opentelemetry.metric.FakeData.aLongPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aMonotonicLongDescriptor;
import static com.google.cloud.opentelemetry.metric.FakeData.anInstrumentationLibraryInfo;
import static com.google.cloud.opentelemetry.metric.FakeData.someLabels;
import static com.google.cloud.opentelemetry.metric.MetricExporter.PROJECT_NAME_PREFIX;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.DESCRIPTOR_TYPE_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.LabelDescriptor;
import com.google.api.LabelDescriptor.ValueType;
import com.google.api.MetricDescriptor;
import com.google.api.MetricDescriptor.MetricKind;
import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.CreateMetricDescriptorRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import io.opentelemetry.sdk.metrics.export.MetricExporter.ResultCode;
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

  @Mock
  private CloudMetricClientImpl mockClient;

  @Captor
  private ArgumentCaptor<ArrayList<TimeSeries>> timeSeriesArgCaptor;

  @Captor
  private ArgumentCaptor<CreateMetricDescriptorRequest> metricDescriptorCaptor;

  @Captor
  private ArgumentCaptor<ProjectName> projectNameArgCaptor;


  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(mockClient.createMetricDescriptor(any())).thenReturn(null);
  }

  @Test
  public void testCreateWithConfigurationSucceeds() throws IOException {
    MetricConfiguration configuration = MetricConfiguration.builder().setProjectId(aFakeProjectId)
        .setCredentials(aFakeCredential).build();
    MetricExporter exporter = MetricExporter.createWithConfiguration(configuration);
    assertNotNull(exporter);
  }

  @Test
  public void testExportSucceeds() {
    MetricDescriptor expectedDescriptor = MetricDescriptor.newBuilder()
        .setDisplayName(anInstrumentationLibraryInfo.getName())
        .setType(DESCRIPTOR_TYPE_URL + anInstrumentationLibraryInfo.getName())
        .addLabels(LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING).build())
        .addLabels(LabelDescriptor.newBuilder().setKey("label2").setValueType(ValueType.BOOL).build())
        .setMetricKind(MetricKind.CUMULATIVE)
        .setValueType(MetricDescriptor.ValueType.INT64)
        .build();
    CreateMetricDescriptorRequest expectedRequest = CreateMetricDescriptorRequest.newBuilder()
        .setName(PROJECT_NAME_PREFIX + aFakeProjectId)
        .setMetricDescriptor(expectedDescriptor)
        .build();
    ProjectName expectedProjectName = ProjectName.of(aFakeProjectId);

    MetricExporter exporter = MetricExporter.createWithClient(aFakeProjectId, mockClient);
    MetricData metricData = MetricData
        .create(aMonotonicLongDescriptor, aGceResource, anInstrumentationLibraryInfo, ImmutableList.of(aLongPoint));

    ResultCode result = exporter.export(ImmutableList.of(metricData));
    verify(mockClient, times(1)).createMetricDescriptor(metricDescriptorCaptor.capture());
    verify(mockClient, times(1)).createTimeSeries(projectNameArgCaptor.capture(), timeSeriesArgCaptor.capture());

    assertEquals(ResultCode.SUCCESS, result);
    assertEquals(expectedRequest, metricDescriptorCaptor.getValue());
    assertEquals(expectedProjectName, projectNameArgCaptor.getValue());
    assertEquals(1, timeSeriesArgCaptor.getValue().size());
    TimeSeries timeSeries = timeSeriesArgCaptor.getValue().get(0);
    assertEquals(DESCRIPTOR_TYPE_URL + anInstrumentationLibraryInfo.getName(), timeSeries.getMetric().getType());
    assertEquals(1, timeSeries.getPointsCount());
    assertEquals(32L, timeSeries.getPoints(0).getValue().getInt64Value());
  }

  @Test
  public void testExportWithNonSupportedMetricTypeDoesNothing() {
    MetricExporter exporter = MetricExporter.createWithClient(aFakeProjectId, mockClient);
    Descriptor summaryDescriptor = Descriptor
        .create("Descriptor Name", "Descriptor description", "Unit", Type.SUMMARY,
            someLabels);
    MetricData metricData = MetricData
        .create(summaryDescriptor, aGceResource, anInstrumentationLibraryInfo, ImmutableList.of(aLongPoint));

    ResultCode result = exporter.export(ImmutableList.of(metricData));
    verify(mockClient, times(0)).createMetricDescriptor(any());
    verify(mockClient, times(0)).createTimeSeries(any(ProjectName.class), any());

    assertEquals(ResultCode.SUCCESS, result);
  }

}
