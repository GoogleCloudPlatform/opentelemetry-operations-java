package com.google.cloud.opentelemetry.auto;

import java.util.Arrays;
import java.util.List;

public class Constants {
  static final List<String> CLOUD_MONITORING_EXPORTER_NAMES =
      Arrays.asList("google_cloud", "google_cloud_monitoring");
  static final List<String> CLOUD_TRACE_EXPORTER_NAMES =
      Arrays.asList("google_cloud", "google_cloud_trace");
}
