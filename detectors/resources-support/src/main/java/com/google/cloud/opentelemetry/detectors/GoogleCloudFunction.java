package com.google.cloud.opentelemetry.detectors;

class GoogleCloudFunction extends GoogleServerlessCompute {
  @Override
  public GCPPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS;
  }
}
