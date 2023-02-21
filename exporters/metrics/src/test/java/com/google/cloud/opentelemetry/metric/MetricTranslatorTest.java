/*
 * Copyright 2023 Google
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
import static com.google.cloud.opentelemetry.metric.MetricConfiguration.DEFAULT_PREFIX;
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
import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.DroppedLabels;
import com.google.monitoring.v3.SpanContext;
import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSumData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSummaryData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetricTranslatorTest {
  static final String customPrefix = "custom.googleapis.com";

  @Test
  public void testMapMetricSucceeds() {
    String type = DEFAULT_PREFIX + "/" + anInstrumentationLibraryInfo.getName();

    Builder expectedMetricBuilder = Metric.newBuilder().setType(type);
    aLongPoint
        .getAttributes()
        .forEach((k, v) -> expectedMetricBuilder.putLabels(k.getKey(), v.toString()));

    Metric actualMetric = MetricTranslator.mapMetric(aLongPoint.getAttributes(), type);
    assertEquals(expectedMetricBuilder.build(), actualMetric);
  }

  @Test
  public void testMapMetricWithWierdAttributeNameSucceeds() {
    String type = DEFAULT_PREFIX + "/" + anInstrumentationLibraryInfo.getName();
    Attributes attributes =
        io.opentelemetry.api.common.Attributes.of(stringKey("test.bad"), "value");
    Metric expectedMetric =
        Metric.newBuilder().setType(type).putLabels("test_bad", "value").build();
    Metric actualMetric = MetricTranslator.mapMetric(attributes, type);
    assertEquals(expectedMetric, actualMetric);
  }

  @Test
  public void testMapMetricDescriptorSucceeds() {
    MetricDescriptor.Builder expectedDescriptor =
        MetricDescriptor.newBuilder()
            .setDisplayName(aMetricData.getName())
            .setType(DEFAULT_PREFIX + "/" + aMetricData.getName())
            .addLabels(LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING))
            .addLabels(LabelDescriptor.newBuilder().setKey("label2").setValueType(ValueType.BOOL))
            .setUnit(METRIC_DESCRIPTOR_TIME_UNIT)
            .setDescription(aMetricData.getDescription())
            .setMetricKind(MetricKind.CUMULATIVE)
            .setValueType(MetricDescriptor.ValueType.INT64);

    MetricDescriptor actualDescriptor =
        MetricTranslator.mapMetricDescriptor(DEFAULT_PREFIX, aMetricData, aLongPoint);
    assertEquals(expectedDescriptor.build(), actualDescriptor);
  }

  @Test
  public void testMapMetricDescriptorCustomPrefixSucceeds() {
    MetricDescriptor.Builder expectedDescriptor =
        MetricDescriptor.newBuilder()
            .setDisplayName(aMetricData.getName())
            .setType(customPrefix + "/" + aMetricData.getName())
            .addLabels(LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING))
            .addLabels(LabelDescriptor.newBuilder().setKey("label2").setValueType(ValueType.BOOL))
            .setUnit(METRIC_DESCRIPTOR_TIME_UNIT)
            .setDescription(aMetricData.getDescription())
            .setMetricKind(MetricKind.CUMULATIVE)
            .setValueType(MetricDescriptor.ValueType.INT64);

    MetricDescriptor actualDescriptor =
        MetricTranslator.mapMetricDescriptor(customPrefix, aMetricData, aLongPoint);
    assertEquals(expectedDescriptor.build(), actualDescriptor);
  }

  @Test
  public void testMapMetricDescriptorNonMonotonicSumIsGauage() {
    String name = "Metric Name";
    String description = "Metric Description";
    String unit = "ns";
    MetricData metricData =
        ImmutableMetricData.createLongSum(
            aGceResource,
            anInstrumentationLibraryInfo,
            name,
            description,
            unit,
            ImmutableSumData.create(
                false, AggregationTemporality.CUMULATIVE, ImmutableList.of(aLongPoint)));
    MetricDescriptor.Builder expectedDescriptor =
        MetricDescriptor.newBuilder()
            .setDisplayName(metricData.getName())
            .setType(DEFAULT_PREFIX + "/" + metricData.getName())
            .addLabels(LabelDescriptor.newBuilder().setKey("label1").setValueType(ValueType.STRING))
            .addLabels(LabelDescriptor.newBuilder().setKey("label2").setValueType(ValueType.BOOL))
            .setUnit(METRIC_DESCRIPTOR_TIME_UNIT)
            .setDescription(metricData.getDescription())
            .setMetricKind(MetricKind.GAUGE)
            .setValueType(MetricDescriptor.ValueType.INT64);

    MetricDescriptor actualDescriptor =
        MetricTranslator.mapMetricDescriptor(DEFAULT_PREFIX, metricData, aLongPoint);
    assertEquals(expectedDescriptor.build(), actualDescriptor);
  }

  @Test
  public void testMapMetricDescriptorHistogramIsDistribution() {
    String name = "Metric Name";
    String description = "Metric Description";
    String unit = "ns";
    MetricData metricData =
        ImmutableMetricData.createDoubleHistogram(
            aGceResource,
            anInstrumentationLibraryInfo,
            name,
            description,
            unit,
            ImmutableHistogramData.create(
                AggregationTemporality.CUMULATIVE, ImmutableList.of(aHistogramPoint)));
    MetricDescriptor.Builder expectedDescriptor =
        MetricDescriptor.newBuilder()
            .setDisplayName(metricData.getName())
            .setType(DEFAULT_PREFIX + "/" + metricData.getName())
            .addLabels(LabelDescriptor.newBuilder().setKey("test").setValueType(ValueType.STRING))
            .setUnit(METRIC_DESCRIPTOR_TIME_UNIT)
            .setDescription(metricData.getDescription())
            .setMetricKind(MetricKind.CUMULATIVE)
            .setValueType(MetricDescriptor.ValueType.DISTRIBUTION);

    MetricDescriptor actualDescriptor =
        MetricTranslator.mapMetricDescriptor(DEFAULT_PREFIX, metricData, aHistogramPoint);
    assertEquals(expectedDescriptor.build(), actualDescriptor);
  }

  @Test
  public void testMapMetricDescriptorWithInvalidMetricKindReturnsNull() {
    String name = "Metric Name";
    String description = "Metric Description";
    String unit = "ns";
    MetricData metricData =
        ImmutableMetricData.createDoubleSummary(
            aGceResource,
            anInstrumentationLibraryInfo,
            name,
            description,
            unit,
            ImmutableSummaryData.create(ImmutableList.of(aDoubleSummaryPoint)));

    MetricDescriptor actualDescriptor =
        MetricTranslator.mapMetricDescriptor(DEFAULT_PREFIX, metricData, aLongPoint);
    assertNull(actualDescriptor);
  }

  @Test
  public void testMapMetricDescriptorWithDeltaSumReturnsNull() {
    String name = "Metric Name";
    String description = "Metric Description";
    String unit = "ns";
    MetricData metricData =
        ImmutableMetricData.createDoubleSum(
            aGceResource,
            anInstrumentationLibraryInfo,
            name,
            description,
            unit,
            ImmutableSumData.create(
                true, AggregationTemporality.DELTA, ImmutableList.of(aDoublePoint)));

    MetricDescriptor actualDescriptor =
        MetricTranslator.mapMetricDescriptor(DEFAULT_PREFIX, metricData, aLongPoint);
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
  public void testMapLabelWithPeriodInNameSucceeds() {
    LabelDescriptor actualLabel =
        MetricTranslator.mapAttribute(longKey("label.test"), 123928374982123L);
    LabelDescriptor expectedLabel =
        LabelDescriptor.newBuilder().setKey("label_test").setValueType(ValueType.INT64).build();
    assertEquals(expectedLabel, actualLabel);
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
                      "projects/projectId/traces/"
                          + FakeData.aTraceId
                          + "/spans/"
                          + FakeData.aSpanId,
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
