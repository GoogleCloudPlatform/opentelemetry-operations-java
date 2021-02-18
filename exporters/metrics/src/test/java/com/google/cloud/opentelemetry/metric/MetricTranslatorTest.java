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

import static com.google.cloud.opentelemetry.metric.FakeData.aDoubleSummaryPoint;
import static com.google.cloud.opentelemetry.metric.FakeData.aGceResource;
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
import com.google.api.MonitoredResource;
import com.google.common.collect.ImmutableList;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.metrics.data.DoubleSummaryData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
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

  @Test
  public void testMapResourcesWithGCEResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {SemanticAttributes.CLOUD_PROVIDER, SemanticAttributes.CloudProviderValues.GCP},
                  {SemanticAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"},
                  {SemanticAttributes.CLOUD_ZONE, "country-region-zone"},
                  {SemanticAttributes.CLOUD_REGION, "country-region"},
                  {SemanticAttributes.HOST_ID, "GCE-instance-id"},
                  {SemanticAttributes.HOST_NAME, "GCE-instance-name"},
                  {SemanticAttributes.HOST_TYPE, "GCE-instance-type"}
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
                  {SemanticAttributes.CLOUD_PROVIDER, SemanticAttributes.CloudProviderValues.GCP},
                  {SemanticAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"},
                  {SemanticAttributes.CLOUD_ZONE, "country-region-zone"},
                  {SemanticAttributes.CLOUD_REGION, "country-region"},
                  {SemanticAttributes.HOST_ID, "GCE-instance-id"},
                  {SemanticAttributes.HOST_NAME, "GCE-instance-name"},
                  {SemanticAttributes.HOST_TYPE, "GCE-instance-type"},
                  {SemanticAttributes.K8S_CLUSTER_NAME, "GKE-cluster-name"},
                  {SemanticAttributes.K8S_NAMESPACE_NAME, "GKE-testNameSpace"},
                  {SemanticAttributes.K8S_POD_NAME, "GKE-testHostName"},
                  {SemanticAttributes.K8S_CONTAINER_NAME, "GKE-testContainerName"}
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
}
