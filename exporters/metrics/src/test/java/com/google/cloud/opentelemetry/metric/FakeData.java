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

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableList;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.SummaryPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableDoubleExemplarData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableDoublePointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSumData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSummaryPointData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class FakeData {

  private static final long NANO_PER_SECOND = (long) 1e9;

  static final String aProjectId = "TestProjectId";
  static final String aHostId = "TestHostId";
  static final String aCloudZone = "TestCloudZone";

  static final Credentials aFakeCredential =
      GoogleCredentials.newBuilder().setAccessToken(new AccessToken("fake", new Date(100))).build();

  static final Attributes someLabels =
      Attributes.builder().put("label1", "value1").put("label2", "False").build();

  static final Attributes someGceAttributes =
      Attributes.builder()
          .put(
              ResourceAttributes.CLOUD_PLATFORM,
              ResourceAttributes.CloudPlatformValues.GCP_COMPUTE_ENGINE)
          .put(ResourceAttributes.CLOUD_ACCOUNT_ID, aProjectId)
          .put(ResourceAttributes.HOST_ID, aHostId)
          .put(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, aCloudZone)
          .put(ResourceAttributes.CLOUD_PROVIDER, "gcp")
          .put("extra_info", "extra")
          .put("not_gcp_resource", "value")
          .build();

  static final Resource aGceResource = Resource.create(someGceAttributes);

  static final InstrumentationScopeInfo anInstrumentationLibraryInfo =
      InstrumentationScopeInfo.create("instrumentName", "0", "");

  static final LongPointData aLongPoint =
      ImmutableLongPointData.create(
          1599030114 * NANO_PER_SECOND,
          1599031814 * NANO_PER_SECOND,
          Attributes.of(stringKey("label1"), "value1", booleanKey("label2"), false),
          32L);

  static final DoublePointData aDoublePoint =
      ImmutableDoublePointData.create(
          1599030114 * NANO_PER_SECOND,
          1599031814 * NANO_PER_SECOND,
          Attributes.of(stringKey("label1"), "value1", booleanKey("label2"), false),
          32d);

  static final SummaryPointData aDoubleSummaryPoint =
      ImmutableSummaryPointData.create(
          1599030114 * NANO_PER_SECOND,
          1599031814 * NANO_PER_SECOND,
          Attributes.of(stringKey("label1"), "value1", booleanKey("label2"), false),
          1,
          32d,
          Collections.emptyList());

  // The name does not have to start with "opentelemetry/", it is set this way because of a bug in
  // the mock server,
  // and should be changed when the following issue is resolved:
  // https://github.com/googleinterns/cloud-operations-api-mock/issues/56
  static final MetricData aMetricData =
      ImmutableMetricData.createLongSum(
          aGceResource,
          anInstrumentationLibraryInfo,
          "opentelemetry/name",
          "description",
          "ns",
          ImmutableSumData.create(
              true, AggregationTemporality.CUMULATIVE, ImmutableList.of(aLongPoint)));

  static final String aTraceId = "00000000000000000000000000000001";
  static final String aSpanId = "0000000000000002";
  static final SpanContext aSpanContext =
      SpanContext.create(aTraceId, aSpanId, TraceFlags.getDefault(), TraceState.getDefault());
  static final HistogramPointData aHistogramPoint =
      ImmutableHistogramPointData.create(
          0,
          1,
          Attributes.builder().put("test", "one").build(),
          3d,
          1d, // min
          2d, // max
          Arrays.asList(1.0),
          Arrays.asList(1L, 2L),
          Arrays.asList(
              ImmutableDoubleExemplarData.create(
                  Attributes.builder().put("test2", "two").build(), 2, aSpanContext, 3.0)));

  static final MetricData aHistogram =
      ImmutableMetricData.createDoubleHistogram(
          aGceResource,
          anInstrumentationLibraryInfo,
          "histogram",
          "description",
          "ms",
          ImmutableHistogramData.create(
              AggregationTemporality.CUMULATIVE, ImmutableList.of(aHistogramPoint)));
}
