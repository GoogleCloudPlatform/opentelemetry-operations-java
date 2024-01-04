/*
 * Copyright 2024 Google LLC
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
package com.google.cloud.opentelemetry.detectors;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class GCPPlatformDetector {
  public static final GCPPlatformDetector DEFAULT_INSTANCE = new GCPPlatformDetector();

  private final GCPMetadataConfig metadataConfig;
  private final EnvironmentVariables environmentVariables;

  // for testing only
  GCPPlatformDetector(GCPMetadataConfig metadataConfig, EnvironmentVariables environmentVariables) {
    this.metadataConfig = metadataConfig;
    this.environmentVariables = environmentVariables;
  }

  private GCPPlatformDetector() {
    this.metadataConfig = GCPMetadataConfig.DEFAULT_INSTANCE;
    this.environmentVariables = EnvironmentVariables.DEFAULT_INSTANCE;
  }

  // Detects the GCP platform on which the application is running
  public DetectedPlatform detectPlatform() {
    if (!isRunningOnGcp()) {
      return generateDetectedPlatform(SupportedPlatform.UNKNOWN_PLATFORM);
    }
    Function<EnvironmentVariables, Optional<SupportedPlatform>> detectGKE =
        environmentVariables ->
            environmentVariables.get("KUBERNETES_SERVICE_HOST") != null
                ? Optional.of(SupportedPlatform.GOOGLE_KUBERNETES_ENGINE)
                : Optional.empty();
    Function<EnvironmentVariables, Optional<SupportedPlatform>> detectGCR =
        environmentVariables ->
            environmentVariables.get("K_CONFIGURATION") != null
                    && environmentVariables.get("FUNCTION_TARGET") == null
                ? Optional.of(SupportedPlatform.GOOGLE_CLOUD_RUN)
                : Optional.empty();
    Function<EnvironmentVariables, Optional<SupportedPlatform>> detectGCF =
        environmentVariables ->
            environmentVariables.get("FUNCTION_TARGET") != null
                ? Optional.of(SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS)
                : Optional.empty();
    Function<EnvironmentVariables, Optional<SupportedPlatform>> detectGAE =
        environmentVariables ->
            environmentVariables.get("GAE_SERVICE") != null
                ? Optional.of(SupportedPlatform.GOOGLE_APP_ENGINE)
                : Optional.empty();

    // Order of detection functions matters here
    Stream<Function<EnvironmentVariables, Optional<SupportedPlatform>>> platforms =
        Stream.of(detectGKE, detectGCR, detectGCF, detectGAE);
    SupportedPlatform platform =
        platforms
            .map(detectionFn -> detectionFn.apply(environmentVariables))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElse(SupportedPlatform.GOOGLE_COMPUTE_ENGINE); // defaults to GCE
    return generateDetectedPlatform(platform);
  }

  private boolean isRunningOnGcp() {
    return metadataConfig.getProjectId() != null && !metadataConfig.getProjectId().isEmpty();
  }

  private DetectedPlatform generateDetectedPlatform(SupportedPlatform platform) {
    DetectedPlatform detectedPlatform;
    switch (platform) {
      case GOOGLE_KUBERNETES_ENGINE:
        detectedPlatform = new GoogleKubernetesEngine();
        break;
      case GOOGLE_CLOUD_RUN:
        detectedPlatform = new GoogleCloudRun();
        break;
      case GOOGLE_CLOUD_FUNCTIONS:
        detectedPlatform = new GoogleCloudFunction();
        break;
      case GOOGLE_APP_ENGINE:
        detectedPlatform = new GoogleAppEngine();
        break;
      case GOOGLE_COMPUTE_ENGINE:
        detectedPlatform = new GoogleComputeEngine();
        break;
      default:
        detectedPlatform = new UnknownPlatform();
    }
    return detectedPlatform;
  }

  public enum SupportedPlatform {
    GOOGLE_COMPUTE_ENGINE,
    GOOGLE_KUBERNETES_ENGINE,
    GOOGLE_APP_ENGINE,
    GOOGLE_CLOUD_RUN,
    GOOGLE_CLOUD_FUNCTIONS,
    UNKNOWN_PLATFORM, // Not running on GCP
  }
}
