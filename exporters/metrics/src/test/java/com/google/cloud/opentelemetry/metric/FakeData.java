package com.google.cloud.opentelemetry.metric;

import com.sun.tools.javac.util.List;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.Labels;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Collection;

public class FakeData {

  static final long NANO_PER_SECOND = (long) 1e9;

  static String anUniqueIdentifier = "UniqueIdentifier123";

  static Labels someLabels = Labels.newBuilder().setLabel("label1", "value1").setLabel("label2", "False").build();

  static Descriptor aMonotonicLongDescriptor = Descriptor
      .create("Descriptor Name", "Descriptor description", "Unit", Type.MONOTONIC_LONG,
          someLabels);

  static Descriptor aNonMonotonicDoubleDescriptor = Descriptor
      .create("Descriptor Name", "Descriptor description", "Unit", Type.NON_MONOTONIC_DOUBLE,
          someLabels);

  static Attributes someAttributes = Attributes.newBuilder()
      .setAttribute("cloud.account.id", 123)
      .setAttribute("host.id", "host")
      .setAttribute("cloud.zone", "US")
      .setAttribute("cloud.provider", "gcp")
      .setAttribute("extra_info", "extra")
      .setAttribute("gcp.resource_type", "gce_instance")
      .setAttribute("not_gcp_resource", "value")
      .build();

  static Resource aResource = Resource.create(someAttributes);

  static InstrumentationLibraryInfo anInstrumentationLibraryInfo = InstrumentationLibraryInfo
      .create("InstrumentName", "Instrument version 0");

  static Collection<Point> someLongPoints = List
      .of(LongPoint.create(1599032114 * NANO_PER_SECOND, 1599031814 * NANO_PER_SECOND, Labels.empty(), 32L));

  static Collection<Point> someDoublePoints = List
      .of(DoublePoint.create(1599032114 * NANO_PER_SECOND, 1599031814 * NANO_PER_SECOND, Labels.empty(), 32.35));

}
