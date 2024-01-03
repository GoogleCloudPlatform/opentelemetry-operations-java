package com.google.cloud.opentelemetry.detectors;

import java.util.Map;
import java.util.Optional;

public interface DetectedPlatform {
  GCPPlatformDetector.SupportedPlatform getSupportedPlatform();

  Map<String, Optional<String>> getAttributes();
}
