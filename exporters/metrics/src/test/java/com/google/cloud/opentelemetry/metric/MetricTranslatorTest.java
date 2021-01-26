package com.google.cloud.opentelemetry.metric;

import static com.google.cloud.opentelemetry.metric.FakeData.aGceResource;
import static com.google.cloud.opentelemetry.metric.FakeData.aDoubleSummaryPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aLongPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aMetricData;
import static com.google.cloud.opentelemetry.metric.FakeData.anInstrumentationLibraryInfo;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.DESCRIPTOR_TYPE_URL;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.METRIC_DESCRIPTOR_TIME_UNIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.api.LabelDescriptor;
import com.google.api.LabelDescriptor.ValueType;
import com.google.api.Metric;
import com.google.api.Metric.Builder;
import com.google.api.MetricDescriptor;
import com.google.api.MetricDescriptor.MetricKind;
import com.google.common.collect.ImmutableList;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.DoubleSummaryData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetricTranslatorTest {

  @Test
  public void testMapMetricSucceeds() {
    String type = "custom.googleapis.com/OpenTelemetry/" + anInstrumentationLibraryInfo.getName();

    Builder expectedMetricBuilder = Metric.newBuilder().setType(type);
    aLongPoint.getLabels().forEach(expectedMetricBuilder::putLabels);

    Metric actualMetric = MetricTranslator.mapMetric(aLongPoint.getLabels(), type);
    assertEquals(expectedMetricBuilder.build(), actualMetric);
  }

  @Test
  public void testMapMetricDescriptorSucceeds() {
    MetricDescriptor.Builder expectedDescriptor =
        MetricDescriptor.newBuilder()
            .setDisplayName(aMetricData.getName())
            .setType(DESCRIPTOR_TYPE_URL + aMetricData.getName())
            .addLabels(LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING))
            .addLabels(LabelDescriptor.newBuilder().setKey("label2").setValueType(ValueType.BOOL))
            .setUnit(METRIC_DESCRIPTOR_TIME_UNIT)
            .setDescription(aMetricData.getDescription())
            .setMetricKind(MetricKind.CUMULATIVE)
            .setValueType(MetricDescriptor.ValueType.INT64);

    MetricDescriptor actualDescriptor =
        MetricTranslator.mapMetricDescriptor(aMetricData, aLongPoint);
    assertEquals(expectedDescriptor.build(), actualDescriptor);
  }

  @Test
  public void testMapMetricDescriptorWithInvalidMetricKindReturnsNull() {
    String name = "Metric Name";
    String description = "Metric Description";
    String unit = "ns";
    MetricData metricData =
        MetricData.createDoubleSummary(
            aGceResource,
            anInstrumentationLibraryInfo,
            name,
            description,
            unit,
            DoubleSummaryData.create(ImmutableList.of(aDoubleSummaryPoint)));

    MetricDescriptor actualDescriptor =
        MetricTranslator.mapMetricDescriptor(metricData, aLongPoint);
    assertNull(actualDescriptor);
  }

  @Test
  public void testMapConstantLabelWithStringValueSucceeds() {
    LabelDescriptor actualLabel = MetricTranslator.mapLabel("label1", "value1");
    LabelDescriptor expectedLabel =
        LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING).build();
    assertEquals(expectedLabel, actualLabel);
  }

  @Test
  public void testMapConstantLabelWithBooleanValueSucceeds() {
    LabelDescriptor actualLabel = MetricTranslator.mapLabel("label1", "True");
    LabelDescriptor expectedLabel =
        LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.BOOL).build();
    assertEquals(expectedLabel, actualLabel);

    LabelDescriptor actualLabel2 = MetricTranslator.mapLabel("label1", "false");
    assertEquals(expectedLabel, actualLabel2);
  }

  @Test
  public void testMapConstantLabelWithLongValueSucceeds() {
    LabelDescriptor actualLabel = MetricTranslator.mapLabel("label1", "123928374982123");
    LabelDescriptor expectedLabel =
        LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.INT64).build();
    assertEquals(expectedLabel, actualLabel);
  }
}
