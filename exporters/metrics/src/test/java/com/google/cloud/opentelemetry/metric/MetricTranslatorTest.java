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

import static com.google.cloud.opentelemetry.metric.FakeData.aDoublePoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aDoubleSummaryPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aGceResource;
import static com.google.cloud.opentelemetry.metric.FakeData.aHistogramPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aLongPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aMetricData;
import static com.google.cloud.opentelemetry.metric.FakeData.anInstrumentationLibraryInfo;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.DESCRIPTOR_TYPE_URL;
import static com.google.cloud.opentelemetry.metric.MetricTranslator.METRIC_DESCRIPTOR_TIME_UNIT;
import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.api.Distribution;
import com.google.api.LabelDescriptor;
import com.google.api.LabelDescriptor.ValueType;
import com.google.api.Metric;
import com.google.api.Metric.Builder;
import com.google.api.MetricDescriptor;
import com.google.api.MetricDescriptor.MetricKind;
import com.google.api.MonitoredResource;
import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.DroppedLabels;
import com.google.monitoring.v3.SpanContext;
import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoubleHistogramData;
import io.opentelemetry.sdk.metrics.data.DoubleSumData;
import io.opentelemetry.sdk.metrics.data.DoubleSummaryData;
import io.opentelemetry.sdk.metrics.data.LongSumData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetricTranslatorTest {

  @Test
  public void testMapMetricSucceeds() {
    String type = "custom.googleapis.com/OpenTelemetry/" + anInstrumentationLibraryInfo.getName();

    Builder expectedMetricBuilder = Metric.newBuilder().setType(type);
    aLongPoint
        .getAttributes()
        .forEach((k, v) -> expectedMetricBuilder.putLabels(k.getKey(), v.toString()));

    Metric actualMetric = MetricTranslator.mapMetric(aLongPoint.getAttributes(), type);
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
  public void testMapMetricDescriptorNonMonotonicSumIsGauage() {
    String name = "Metric Name";
    String description = "Metric Description";
    String unit = "ns";
    MetricData metricData =
        MetricData.createLongSum(
            aGceResource,
            anInstrumentationLibraryInfo,
            name,
            description,
            unit,
            LongSumData.create(
                false, AggregationTemporality.CUMULATIVE, ImmutableList.of(aLongPoint)));
    MetricDescriptor.Builder expectedDescriptor =
        MetricDescriptor.newBuilder()
            .setDisplayName(metricData.getName())
            .setType(DESCRIPTOR_TYPE_URL + metricData.getName())
            .addLabels(LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING))
            .addLabels(LabelDescriptor.newBuilder().setKey("label2").setValueType(ValueType.BOOL))
            .setUnit(METRIC_DESCRIPTOR_TIME_UNIT)
            .setDescription(metricData.getDescription())
            .setMetricKind(MetricKind.GAUGE)
            .setValueType(MetricDescriptor.ValueType.INT64);

    MetricDescriptor actualDescriptor =
        MetricTranslator.mapMetricDescriptor(metricData, aLongPoint);
    assertEquals(expectedDescriptor.build(), actualDescriptor);
  }

  @Test
  public void testMapMetricDescriptorHistogramIsDistribution() {
    String name = "Metric Name";
    String description = "Metric Description";
    String unit = "ns";
    MetricData metricData =
        MetricData.createDoubleHistogram(
            aGceResource,
            anInstrumentationLibraryInfo,
            name,
            description,
            unit,
            DoubleHistogramData.create(
                AggregationTemporality.CUMULATIVE, ImmutableList.of(aHistogramPoint)));
    MetricDescriptor.Builder expectedDescriptor =
        MetricDescriptor.newBuilder()
            .setDisplayName(metricData.getName())
            .setType(DESCRIPTOR_TYPE_URL + metricData.getName())
            .addLabels(LabelDescriptor.newBuilder().setKey("test").setValueType(ValueType.STRING))
            .setUnit(METRIC_DESCRIPTOR_TIME_UNIT)
            .setDescription(metricData.getDescription())
            .setMetricKind(MetricKind.CUMULATIVE)
            .setValueType(MetricDescriptor.ValueType.DISTRIBUTION);

    MetricDescriptor actualDescriptor =
        MetricTranslator.mapMetricDescriptor(metricData, aHistogramPoint);
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
  public void testMapMetricDescriptorWithDeltaSumReturnsNull() {
    String name = "Metric Name";
    String description = "Metric Description";
    String unit = "ns";
    MetricData metricData =
        MetricData.createDoubleSum(
            aGceResource,
            anInstrumentationLibraryInfo,
            name,
            description,
            unit,
            DoubleSumData.create(
                true, AggregationTemporality.DELTA, ImmutableList.of(aDoublePoint)));

    MetricDescriptor actualDescriptor =
        MetricTranslator.mapMetricDescriptor(metricData, aLongPoint);
    assertNull(actualDescriptor);
  }

  @Test
  public void testMapConstantLabelWithStringValueSucceeds() {
    LabelDescriptor actualLabel = MetricTranslator.mapAttribute(stringKey("label1"), "value1");
    LabelDescriptor expectedLabel =
        LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING).build();
    assertEquals(expectedLabel, actualLabel);
  }

  @Test
  public void testMapConstantLabelWithBooleanValueSucceeds() {
    LabelDescriptor actualLabel = MetricTranslator.mapAttribute(booleanKey("label1"), true);
    LabelDescriptor expectedLabel =
        LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.BOOL).build();
    assertEquals(expectedLabel, actualLabel);

    LabelDescriptor actualLabel2 = MetricTranslator.mapAttribute(booleanKey("label1"), false);
    assertEquals(expectedLabel, actualLabel2);
  }

  @Test
  public void testMapConstantLabelWithLongValueSucceeds() {
    LabelDescriptor actualLabel =
        MetricTranslator.mapAttribute(longKey("label1"), 123928374982123L);
    LabelDescriptor expectedLabel =
        LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.INT64).build();
    assertEquals(expectedLabel, actualLabel);
  }

  @Test
  public void testMapResourcesWithGCEResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {ResourceAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"},
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.CLOUD_REGION, "country-region"},
                  {ResourceAttributes.HOST_ID, "GCE-instance-id"},
                  {ResourceAttributes.HOST_NAME, "GCE-instance-name"},
                  {ResourceAttributes.HOST_TYPE, "GCE-instance-type"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(
        (key, value) -> {
          attrBuilder.put(key, value);
        });
    Attributes attributes = attrBuilder.build();

    MonitoredResource monitoredResource =
        MetricTranslator.mapResource(Resource.create(attributes), "GCE_pid");

    assertEquals("gce_instance", monitoredResource.getType());

    Map<String, String> monitoredResourceMap = monitoredResource.getLabelsMap();
    assertEquals(3, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"instance_id", "GCE-instance-id"},
                  {"zone", "country-region-zone"},
                  {"project_id", "GCE-pid"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithGKEResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {ResourceAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"},
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.CLOUD_REGION, "country-region"},
                  {ResourceAttributes.HOST_ID, "GCE-instance-id"},
                  {ResourceAttributes.HOST_NAME, "GCE-instance-name"},
                  {ResourceAttributes.HOST_TYPE, "GCE-instance-type"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "GKE-cluster-name"},
                  {ResourceAttributes.K8S_NAMESPACE_NAME, "GKE-testNameSpace"},
                  {ResourceAttributes.K8S_POD_NAME, "GKE-testHostName"},
                  {ResourceAttributes.K8S_CONTAINER_NAME, "GKE-testContainerName"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    MonitoredResource monitoredResource =
        MetricTranslator.mapResource(Resource.create(attributes), "GCE_pid");

    assertEquals("gke_container", monitoredResource.getType());
    Map<String, String> monitoredResourceMap = monitoredResource.getLabelsMap();
    assertEquals(7, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"instance_id", "GCE-instance-id"},
                  {"zone", "country-region-zone"},
                  {"project_id", "GCE-pid"},
                  {"cluster_name", "GKE-cluster-name"},
                  {"pod_id", "GKE-testHostName"},
                  {"container_name", "GKE-testContainerName"},
                  {"namespace_id", "GKE-testNameSpace"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapDistribution() {
    Distribution result =
        MetricTranslator.mapDistribution(FakeData.aHistogramPoint, "projectId").build();
    assertEquals("Distirbution.count", 3, result.getCount());
    assertEquals("Distribution.bucketCounts[0]", 1, result.getBucketCounts(0));
    assertEquals("Distribution.bucketCounts[1]", 2, result.getBucketCounts(1));
    assertEquals("Distribution.mean", 1d, result.getMean(), 0.001);
    assertEquals(
        "Distribution.bucketOptions.explicitBucketBounds[0]",
        1.0d,
        result.getBucketOptions().getExplicitBuckets().getBounds(0),
        0.001);
    assertEquals("Distribution.exemplars.length", 1, result.getExemplarsCount());
    assertEquals("Distribution.exemplars[0].value", 3, result.getExemplars(0).getValue(), 0.01);
    assertEquals(
        "Distribution.exemplars[0].attachments.length",
        2,
        result.getExemplars(0).getAttachmentsCount());
    result
        .getExemplars(0)
        .getAttachmentsList()
        .forEach(
            any -> {
              try {
                if (any.is(SpanContext.class)) {
                  assertEquals(
                      "projects/projectId/traces/traceId/spans/spanId",
                      any.unpack(SpanContext.class).getSpanName());
                } else if (any.is(DroppedLabels.class)) {
                  DroppedLabels labels = any.unpack(DroppedLabels.class);
                  assertEquals("two", labels.getLabelMap().get("test2"));
                }
              } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
              }
            });
  }
}
