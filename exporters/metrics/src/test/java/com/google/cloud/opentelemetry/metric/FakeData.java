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

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableList;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.common.Labels;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.DoubleSummaryPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.LongSumData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.Collections;
import java.util.Date;

public class FakeData {

  private static final long NANO_PER_SECOND = (long) 1e9;

  static final String aProjectId = "TestProjectId";
  static final String aHostId = "TestHostId";
  static final String aCloudZone = "TestCloudZone";

  static final Credentials aFakeCredential =
      GoogleCredentials.newBuilder().setAccessToken(new AccessToken("fake", new Date(100))).build();

  static final Labels someLabels =
      Labels.builder().put("label1", "value1").put("label2", "False").build();

  static final Attributes someGceAttributes =
      Attributes.builder()
          .put(ResourceAttributes.CLOUD_ACCOUNT_ID, aProjectId)
          .put(ResourceAttributes.HOST_ID, aHostId)
          .put(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, aCloudZone)
          .put(ResourceAttributes.CLOUD_PROVIDER, "gcp")
          .put("extra_info", "extra")
          .put("not_gcp_resource", "value")
          .build();

  static final Resource aGceResource = Resource.create(someGceAttributes);

  static final InstrumentationLibraryInfo anInstrumentationLibraryInfo =
      InstrumentationLibraryInfo.create("instrumentName", "0");

  static final LongPointData aLongPoint =
      LongPointData.create(
          1599030114 * NANO_PER_SECOND,
          1599031814 * NANO_PER_SECOND,
          Labels.of("label1", "value1", "label2", "False"),
          32L);

  static final DoublePointData aDoublePoint =
      DoublePointData.create(
          1599030114 * NANO_PER_SECOND,
          1599031814 * NANO_PER_SECOND,
          Labels.of("label1", "value1", "label2", "False"),
          32d);

  static final DoubleSummaryPointData aDoubleSummaryPoint =
      DoubleSummaryPointData.create(
          1599030114 * NANO_PER_SECOND,
          1599031814 * NANO_PER_SECOND,
          Labels.of("label1", "value1", "label2", "False"),
          1,
          32d,
          Collections.emptyList());

  // The name does not have to start with "opentelemetry/", it is set this way because of a bug in
  // the mock server,
  // and should be changed when the following issue is resolved:
  // https://github.com/googleinterns/cloud-operations-api-mock/issues/56
  static final MetricData aMetricData =
      MetricData.createLongSum(
          aGceResource,
          anInstrumentationLibraryInfo,
          "opentelemetry/name",
          "description",
          "ns",
          LongSumData.create(
              true, AggregationTemporality.CUMULATIVE, ImmutableList.of(aLongPoint)));
}
