package com.google.cloud.opentelemetry.metric;

import static com.google.cloud.opentelemetry.metric.FakeData.NANO_PER_SECOND;
import static com.google.cloud.opentelemetry.metric.FakeData.aGceResource;
import static com.google.cloud.opentelemetry.metric.FakeData.aMonotonicLongDescriptor;
import static com.google.cloud.opentelemetry.metric.FakeData.aNonMonotonicDoubleDescriptor;
import static com.google.cloud.opentelemetry.metric.FakeData.anInstrumentationLibraryInfo;
import static com.google.cloud.opentelemetry.metric.FakeData.anUniqueIdentifier;
import static com.google.cloud.opentelemetry.metric.FakeData.someDoublePoints;
import static com.google.cloud.opentelemetry.metric.FakeData.someGkeAttributes;
import static com.google.cloud.opentelemetry.metric.FakeData.someLabels;
import static com.google.cloud.opentelemetry.metric.FakeData.someLongPoints;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.DESCRIPTOR_TYPE_URL;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.UNIQUE_IDENTIFIER_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.api.LabelDescriptor;
import com.google.api.LabelDescriptor.ValueType;
import com.google.api.Metric;
import com.google.api.Metric.Builder;
import com.google.api.MetricDescriptor;
import com.google.api.MetricDescriptor.MetricKind;
import com.google.api.MonitoredResource;
import com.google.cloud.opentelemetry.metric.MetricExporter.MetricWithLabels;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.Timestamp;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetricTranslatorTest {

  @Test
  public void testMapMetricWithUniqueIdentifierSucceeds() {
    MetricData metricData = MetricData
        .create(aMonotonicLongDescriptor, aGceResource, anInstrumentationLibraryInfo, someLongPoints);
    String type = "custom.googleapis.com/OpenTelemetry/" + anInstrumentationLibraryInfo.getName();

    Builder expectedMetricBuilder = Metric.newBuilder().setType(type)
        .putLabels(UNIQUE_IDENTIFIER_KEY, anUniqueIdentifier);
    metricData.getDescriptor().getConstantLabels().forEach(expectedMetricBuilder::putLabels);

    Metric actualMetric = MetricTranslator.mapMetric(metricData, type,
        anUniqueIdentifier);
    assertEquals(expectedMetricBuilder.build(), actualMetric);
  }

  @Test
  public void testMapMetricWithoutUniqueIdentifierSucceeds() {
    MetricData metricData = MetricData
        .create(aMonotonicLongDescriptor, aGceResource, anInstrumentationLibraryInfo, someLongPoints);
    String type = "custom.googleapis.com/OpenTelemetry/" + anInstrumentationLibraryInfo.getName();

    Builder expectedMetricBuilder = Metric.newBuilder().setType(type);
    metricData.getDescriptor().getConstantLabels().forEach(expectedMetricBuilder::putLabels);

    Metric actualMetric = MetricTranslator.mapMetric(metricData, type, null);
    assertEquals(expectedMetricBuilder.build(), actualMetric);
  }

  @Test
  public void testMapMetricDescriptorWithUniqueIdentifierSucceeds() {
    MetricData metricData = MetricData
        .create(aMonotonicLongDescriptor, aGceResource, anInstrumentationLibraryInfo, someLongPoints);

    MetricDescriptor.Builder expectedDescriptor = MetricDescriptor.newBuilder()
        .setDisplayName(anInstrumentationLibraryInfo.getName())
        .setType(DESCRIPTOR_TYPE_URL + anInstrumentationLibraryInfo.getName())
        .addLabels(LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING))
        .addLabels(LabelDescriptor.newBuilder().setKey("label2").setValueType(ValueType.BOOL))
        .addLabels(LabelDescriptor.newBuilder().setKey(UNIQUE_IDENTIFIER_KEY).setValueType(ValueType.STRING))
        .setMetricKind(MetricKind.CUMULATIVE)
        .setValueType(MetricDescriptor.ValueType.INT64);

    MetricDescriptor actualDescriptor = MetricTranslator.mapMetricDescriptor(metricData, anUniqueIdentifier);
    assertEquals(expectedDescriptor.build(), actualDescriptor);
  }

  @Test
  public void testMapMetricDescriptorWithoutUniqueIdentifierSucceeds() {
    MetricData metricData = MetricData
        .create(aMonotonicLongDescriptor, aGceResource, anInstrumentationLibraryInfo, someLongPoints);

    MetricDescriptor.Builder expectedDescriptor = MetricDescriptor.newBuilder()
        .setDisplayName(anInstrumentationLibraryInfo.getName())
        .setType(DESCRIPTOR_TYPE_URL + anInstrumentationLibraryInfo.getName())
        .addLabels(LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING))
        .addLabels(LabelDescriptor.newBuilder().setKey("label2").setValueType(ValueType.BOOL))
        .setMetricKind(MetricKind.CUMULATIVE)
        .setValueType(MetricDescriptor.ValueType.INT64);

    MetricDescriptor actualDescriptor = MetricTranslator.mapMetricDescriptor(metricData, null);
    assertEquals(expectedDescriptor.build(), actualDescriptor);
  }

  @Test
  public void testMapMetricDescriptorWithInvalidMetricKindReturnsNull() {
    Descriptor summaryDescriptor = Descriptor
        .create("Descriptor Name", "Descriptor description", "Unit", Type.SUMMARY,
            someLabels);
    MetricData metricData = MetricData.create(summaryDescriptor, aGceResource, anInstrumentationLibraryInfo,
        someLongPoints);

    MetricDescriptor actualDescriptor = MetricTranslator.mapMetricDescriptor(metricData, null);
    assertNull(actualDescriptor);
  }

  @Test
  public void testMapGcpMonitoredResourceWithGceTypeSucceeds() {
    MonitoredResource expectedResource = MonitoredResource.newBuilder()
        .setType("gce_instance")
        .putLabels("instance_id", "host")
        .putLabels("project_id", "123")
        .putLabels("zone", "US")
        .build();
    MonitoredResource actualResource = MetricTranslator.mapGcpMonitoredResource(aGceResource);
    assertEquals(expectedResource, actualResource);
  }

  @Test
  public void testMapGcpMonitoredResourceWithNonGcpProviderReturnsNull() {
    Attributes someAttributes = Attributes.newBuilder()
        .setAttribute("cloud.provider", "anotherProvider")
        .setAttribute("gcp.resource_type", "gce_instance")
        .build();
    Resource aResource = Resource.create(someAttributes);

    MonitoredResource actualResource = MetricTranslator.mapGcpMonitoredResource(aResource);
    assertNull(actualResource);
  }

  @Test
  public void testMapGcpMonitoredResourceWithNonGcpResourceTypeReturnsNull() {
    Attributes someAttributes = Attributes.newBuilder()
        .setAttribute("cloud.provider", "gcp")
        .setAttribute("gcp.resource_type", "random_instance")
        .build();
    Resource aResource = Resource.create(someAttributes);

    MonitoredResource actualResource = MetricTranslator.mapGcpMonitoredResource(aResource);
    assertNull(actualResource);
  }

  @Test
  public void testMapGcpMonitoredResourceWithGkeTypeSucceeds() {
    MonitoredResource expectedResource = MonitoredResource.newBuilder()
        .setType("gke_container")
        .putLabels("cluster_name", "my_k8s_cluster")
        .putLabels("namespace_id", "my_k8s_namespace")
        .putLabels("pod_id", "my_pod_123")
        .putLabels("instance_id", "host")
        .putLabels("container_name", "otel_container_1")
        .putLabels("project_id", "123")
        .putLabels("zone", "US")
        .build();
    MonitoredResource actualResource = MetricTranslator.mapGcpMonitoredResource(Resource.create(someGkeAttributes));
    assertEquals(expectedResource, actualResource);
  }

  @Test
  public void testMapConstantLabelWithStringValueSucceeds() {
    LabelDescriptor actualLabel = MetricTranslator.mapConstantLabel("label1", "value1");
    LabelDescriptor expectedLabel = LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING)
        .build();
    assertEquals(expectedLabel, actualLabel);
  }

  @Test
  public void testMapConstantLabelWithBooleanValueSucceeds() {
    LabelDescriptor actualLabel = MetricTranslator.mapConstantLabel("label1", "True");
    LabelDescriptor expectedLabel = LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.BOOL)
        .build();
    assertEquals(expectedLabel, actualLabel);

    LabelDescriptor actualLabel2 = MetricTranslator.mapConstantLabel("label1", "false");
    assertEquals(expectedLabel, actualLabel2);
  }

  @Test
  public void testMapConstantLabelWithLongValueSucceeds() {
    LabelDescriptor actualLabel = MetricTranslator.mapConstantLabel("label1", "123928374982123");
    LabelDescriptor expectedLabel = LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.INT64)
        .build();
    assertEquals(expectedLabel, actualLabel);
  }

  @Test
  public void testMapPointWithCumulativeLongResetSucceeds() {
    Instant exporterStartTime = Instant.ofEpochSecond(1599022162);
    long pointCollectionTime = 1599132125 * NANO_PER_SECOND;
    MetricWithLabels metricWithLabels = new MetricWithLabels("custom.googleapis.com/OpenTelemetry/InstrumentName",
        someLabels);
    Map<MetricWithLabels, Long> lastUpdated = new HashMap<>();
    lastUpdated.put(metricWithLabels, 1599032114L);
    MetricData metricData = MetricData
        .create(aMonotonicLongDescriptor, aGceResource, anInstrumentationLibraryInfo, someLongPoints);

    long expectedStartNano = lastUpdated.get(metricWithLabels) + (long) 1e6;
    Timestamp expectedStartTime = Timestamp.newBuilder().setSeconds(expectedStartNano / NANO_PER_SECOND).setNanos(
        (int) (expectedStartNano % NANO_PER_SECOND)).build();
    Timestamp expectedEndTime = Timestamp.newBuilder().setSeconds(pointCollectionTime / NANO_PER_SECOND)
        .setNanos((int) (pointCollectionTime % NANO_PER_SECOND)).build();
    TimeInterval expectedInterval = TimeInterval.newBuilder()
        .setStartTime(expectedStartTime)
        .setEndTime(expectedEndTime)
        .build();
    Point expectedPoint = Point.newBuilder().setValue(TypedValue.newBuilder().setInt64Value(32L).build()).setInterval(
        expectedInterval).build();

    Point actualPoint = MetricTranslator.mapPoint(lastUpdated, metricData, metricWithLabels,
        exporterStartTime, pointCollectionTime);
    assertEquals(expectedPoint, actualPoint);
  }

  @Test
  public void testMapPointWithCumulativeLongAggregatedSucceeds() {
    Instant exporterStartTime = Instant.ofEpochSecond(1599022162);
    long pointCollectionTime = 1599132125 * NANO_PER_SECOND;
    MetricWithLabels metricWithLabels = new MetricWithLabels("custom.googleapis.com/OpenTelemetry/InstrumentName",
        someLabels);
    Map<MetricWithLabels, Long> lastUpdated = new HashMap<>();
    MetricData metricData = MetricData
        .create(aMonotonicLongDescriptor, aGceResource, anInstrumentationLibraryInfo, someLongPoints);

    Timestamp expectedStartTime = Timestamp.newBuilder().setSeconds(exporterStartTime.getEpochSecond()).setNanos(
        exporterStartTime.getNano()).build();
    Timestamp expectedEndTime = Timestamp.newBuilder().setSeconds(pointCollectionTime / NANO_PER_SECOND)
        .setNanos((int) (pointCollectionTime % NANO_PER_SECOND)).build();
    TimeInterval expectedInterval = TimeInterval.newBuilder()
        .setStartTime(expectedStartTime)
        .setEndTime(expectedEndTime)
        .build();
    Point expectedPoint = Point.newBuilder().setValue(TypedValue.newBuilder().setInt64Value(32L).build()).setInterval(
        expectedInterval).build();

    Point actualPoint = MetricTranslator.mapPoint(lastUpdated, metricData, metricWithLabels,
        exporterStartTime, pointCollectionTime);
    assertEquals(expectedPoint, actualPoint);
  }

  @Test
  public void testMapPointWithGaugeDoubleSucceeds() {
    Instant exporterStartTime = Instant.ofEpochSecond(1599022162);
    long pointCollectionTime = 1599132125 * NANO_PER_SECOND;
    MetricWithLabels metricWithLabels = new MetricWithLabels("custom.googleapis.com/OpenTelemetry/InstrumentName",
        someLabels);
    Map<MetricWithLabels, Long> lastUpdated = new HashMap<>();
    MetricData metricData = MetricData
        .create(aNonMonotonicDoubleDescriptor, aGceResource, anInstrumentationLibraryInfo, someDoublePoints);

    Timestamp expectedStartEndTime = Timestamp.newBuilder().setSeconds(pointCollectionTime / NANO_PER_SECOND)
        .setNanos((int) (pointCollectionTime % NANO_PER_SECOND)).build();
    TimeInterval expectedInterval = TimeInterval.newBuilder()
        .setStartTime(expectedStartEndTime)
        .setEndTime(expectedStartEndTime)
        .build();
    Point expectedPoint = Point.newBuilder().setValue(TypedValue.newBuilder().setDoubleValue(32.35).build())
        .setInterval(
            expectedInterval).build();

    Point actualPoint = MetricTranslator.mapPoint(lastUpdated, metricData, metricWithLabels,
        exporterStartTime, pointCollectionTime);
    assertEquals(expectedPoint, actualPoint);
  }
}
