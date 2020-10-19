package com.google.cloud.opentelemetry.metric;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableList;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.Labels;
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

  static final Labels someLabels = Labels.newBuilder().setLabel("label1", "value1").setLabel("label2", "False").build();

  static final Attributes someGceAttributes = Attributes.newBuilder()
      .setAttribute("cloud.account.id", 123)
      .setAttribute("host.id", "host")
      .setAttribute("cloud.zone", "US")
      .setAttribute("cloud.provider", "gcp")
      .setAttribute("extra_info", "extra")
      .setAttribute("not_gcp_resource", "value")
      .build();

  static final Resource aGceResource = Resource.create(someGceAttributes);

  static final InstrumentationLibraryInfo anInstrumentationLibraryInfo = InstrumentationLibraryInfo
      .create("instrumentName", "0");

  static final Point aLongPoint = LongPoint
      .create(1599030114 * NANO_PER_SECOND, 1599031814 * NANO_PER_SECOND,
          Labels.of("label1", "value1", "label2", "False"), 32L);

  // The name does not have to start with "opentelemetry/", it is set this way because of a bug in the mock server,
  // and should be changed when the following issue is resolved:
  // https://github.com/googleinterns/cloud-operations-api-mock/issues/56
  static final MetricData aMetricData = MetricData
      .create(aGceResource, anInstrumentationLibraryInfo, "opentelemetry/name", "description", "ns",
          Type.MONOTONIC_LONG, ImmutableList.of(aLongPoint));
}
