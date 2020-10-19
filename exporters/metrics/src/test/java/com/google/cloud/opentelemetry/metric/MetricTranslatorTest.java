package com.google.cloud.opentelemetry.metric;

import static com.google.cloud.opentelemetry.metric.FakeData.aGceResource;
import static com.google.cloud.opentelemetry.metric.FakeData.aLongPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aMetricData;
import static com.google.cloud.opentelemetry.metric.FakeData.anInstrumentationLibraryInfo;
import static com.google.cloud.opentelemetry.metric.FakeData.someLabels;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.DESCRIPTOR_TYPE_URL;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.METRIC_DESCRIPTOR_TIME_UNIT;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.NANO_PER_SECOND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.api.LabelDescriptor;
import com.google.api.LabelDescriptor.ValueType;
import com.google.api.Metric;
import com.google.api.Metric.Builder;
import com.google.api.MetricDescriptor;
import com.google.api.MetricDescriptor.MetricKind;
import com.google.cloud.opentelemetry.metric.MetricExporter.MetricWithLabels;
import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.Timestamp;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.Type;
import java.util.HashMap;
import java.util.Map;
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

    Metric actualMetric = MetricTranslator.mapMetric(aLongPoint, type);
    assertEquals(expectedMetricBuilder.build(), actualMetric);
  }

  @Test
  public void testMapMetricDescriptorSucceeds() {
    MetricDescriptor.Builder expectedDescriptor = MetricDescriptor.newBuilder()
        .setDisplayName(aMetricData.getName())
        .setType(DESCRIPTOR_TYPE_URL + anInstrumentationLibraryInfo.getName())
        .addLabels(LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING))
        .addLabels(LabelDescriptor.newBuilder().setKey("label2").setValueType(ValueType.BOOL))
        .setUnit(METRIC_DESCRIPTOR_TIME_UNIT)
        .setDescription(aMetricData.getDescription())
        .setMetricKind(MetricKind.CUMULATIVE)
        .setValueType(MetricDescriptor.ValueType.INT64);

    MetricDescriptor actualDescriptor = MetricTranslator.mapMetricDescriptor(aMetricData, aLongPoint);
    assertEquals(expectedDescriptor.build(), actualDescriptor);
  }

  @Test
  public void testMapMetricDescriptorWithInvalidMetricKindReturnsNull() {
    String name = "Metric Name";
    String description = "Metric Description";
    String unit = "ns";
    MetricData metricData = MetricData.create(aGceResource, anInstrumentationLibraryInfo, name, description, unit,
        Type.SUMMARY, ImmutableList.of(aLongPoint));

    MetricDescriptor actualDescriptor = MetricTranslator.mapMetricDescriptor(metricData, aLongPoint);
    assertNull(actualDescriptor);
  }

  @Test
  public void testMapConstantLabelWithStringValueSucceeds() {
    LabelDescriptor actualLabel = MetricTranslator.mapLabel("label1", "value1");
    LabelDescriptor expectedLabel = LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING)
        .build();
    assertEquals(expectedLabel, actualLabel);
  }

  @Test
  public void testMapConstantLabelWithBooleanValueSucceeds() {
    LabelDescriptor actualLabel = MetricTranslator.mapLabel("label1", "True");
    LabelDescriptor expectedLabel = LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.BOOL)
        .build();
    assertEquals(expectedLabel, actualLabel);

    LabelDescriptor actualLabel2 = MetricTranslator.mapLabel("label1", "false");
    assertEquals(expectedLabel, actualLabel2);
  }

  @Test
  public void testMapConstantLabelWithLongValueSucceeds() {
    LabelDescriptor actualLabel = MetricTranslator.mapLabel("label1", "123928374982123");
    LabelDescriptor expectedLabel = LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.INT64)
        .build();
    assertEquals(expectedLabel, actualLabel);
  }

  @Test
  public void testMapPointSucceeds() {
    MetricWithLabels metricWithLabels = new MetricWithLabels("custom.googleapis.com/OpenTelemetry/DescriptorName",
        someLabels);
    Map<MetricWithLabels, Long> lastUpdated = new HashMap<>();
    lastUpdated.put(metricWithLabels, 1599032114L);

    Timestamp expectedStartTime = Timestamp.newBuilder().setSeconds(aLongPoint.getStartEpochNanos() / NANO_PER_SECOND)
        .setNanos(
            (int) (aLongPoint.getStartEpochNanos() % NANO_PER_SECOND)).build();
    Timestamp expectedEndTime = Timestamp.newBuilder().setSeconds(aLongPoint.getEpochNanos() / NANO_PER_SECOND)
        .setNanos((int) (aLongPoint.getEpochNanos() % NANO_PER_SECOND)).build();
    TimeInterval expectedInterval = TimeInterval.newBuilder()
        .setStartTime(expectedStartTime)
        .setEndTime(expectedEndTime)
        .build();
    Point expectedPoint = Point.newBuilder().setValue(TypedValue.newBuilder().setInt64Value(32L).build()).setInterval(
        expectedInterval).build();

    Point actualPoint = MetricTranslator.mapPoint(aMetricData, aLongPoint, metricWithLabels, lastUpdated);
    assertEquals(expectedPoint, actualPoint);
  }
}
