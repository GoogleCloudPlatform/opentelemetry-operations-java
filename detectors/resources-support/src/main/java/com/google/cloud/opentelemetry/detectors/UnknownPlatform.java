package com.google.cloud.opentelemetry.detectors;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

class UnknownPlatform implements DetectedPlatform {

  UnknownPlatform() {}

  @Override
  public GCPPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GCPPlatformDetector.SupportedPlatform.UNKNOWN_PLATFORM;
  }

  @Override
  public Map<String, Optional<String>> getAttributes() {
    return Collections.emptyMap();
  }
}
