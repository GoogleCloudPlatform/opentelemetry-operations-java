package com.google.cloud.opentelemetry.metric;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.Labels;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Date;

public class FakeData {

  static final long NANO_PER_SECOND = (long) 1e9;

  static final String aFakeProjectId = "TestProjectId";

  static final Credentials aFakeCredential =
      GoogleCredentials.newBuilder().setAccessToken(new AccessToken("fake", new Date(100))).build();

  static Labels someLabels = Labels.newBuilder().setLabel("label1", "value1").setLabel("label2", "False").build();

  static Descriptor aMonotonicLongDescriptor = Descriptor
      .create("Descriptor Name", "Descriptor description", "Unit", Type.MONOTONIC_LONG,
          someLabels);

  static Descriptor aNonMonotonicDoubleDescriptor = Descriptor
      .create("Descriptor Name", "Descriptor description", "Unit", Type.NON_MONOTONIC_DOUBLE,
          someLabels);

  static Attributes someGceAttributes = Attributes.newBuilder()
      .setAttribute("cloud.account.id", 123)
      .setAttribute("host.id", "host")
      .setAttribute("cloud.zone", "US")
      .setAttribute("cloud.provider", "gcp")
      .setAttribute("extra_info", "extra")
      .setAttribute("gcp.resource_type", "gce_instance")
      .setAttribute("not_gcp_resource", "value")
      .build();

  static Resource aGceResource = Resource.create(someGceAttributes);

  static InstrumentationLibraryInfo anInstrumentationLibraryInfo = InstrumentationLibraryInfo
      .create("instrument name", "0");

  static Point aLongPoint = LongPoint
      .create(1599032114 * NANO_PER_SECOND, 1599031814 * NANO_PER_SECOND, Labels.empty(), 32L);

  static Point aDoublePoint = DoublePoint
      .create(1599032114 * NANO_PER_SECOND, 1599031814 * NANO_PER_SECOND, Labels.empty(), 32.35);
}
