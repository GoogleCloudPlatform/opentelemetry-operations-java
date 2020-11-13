package com.google.cloud.opentelemetry.metric;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableList;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Labels;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import io.opentelemetry.sdk.metrics.data.MetricData.Type;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Date;

public class FakeData {

  private static final long NANO_PER_SECOND = (long) 1e9;

  static final String aProjectId = "TestProjectId";

  static final Credentials aFakeCredential =
      GoogleCredentials.newBuilder().setAccessToken(new AccessToken("fake", new Date(100))).build();

  static final Labels someLabels =
      Labels.builder().put("label1", "value1").put("label2", "False").build();

  static final Attributes someGceAttributes =
      Attributes.builder()
          .put("cloud.account.id", 123)
          .put("host.id", "host")
          .put("cloud.zone", "US")
          .put("cloud.provider", "gcp")
          .put("extra_info", "extra")
          .put("not_gcp_resource", "value")
          .build();

  static final Resource aGceResource = Resource.create(someGceAttributes);

  static final InstrumentationLibraryInfo anInstrumentationLibraryInfo =
      InstrumentationLibraryInfo.create("instrumentName", "0");

  static final Point aLongPoint =
      LongPoint.create(
          1599030114 * NANO_PER_SECOND,
          1599031814 * NANO_PER_SECOND,
          Labels.of("label1", "value1", "label2", "False"),
          32L);

  // The name does not have to start with "opentelemetry/", it is set this way because of a bug in
  // the mock server,
  // and should be changed when the following issue is resolved:
  // https://github.com/googleinterns/cloud-operations-api-mock/issues/56
  static final MetricData aMetricData =
      MetricData.create(
          aGceResource,
          anInstrumentationLibraryInfo,
          "opentelemetry/name",
          "description",
          "ns",
          Type.MONOTONIC_LONG,
          ImmutableList.of(aLongPoint));
}
