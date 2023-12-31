/*
 * Copyright 2023 Google LLC
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

/**
 * Utility class to help add attributes applicable to serverless compute in GCP to a given resource.
 */
public final class ServerlessComputeDetector {
  public static final ServerlessComputeDetector DEFAULT_INSTANCE = new ServerlessComputeDetector();

  private final EnvironmentVariables environmentVariables;
  private final GCPMetadataConfig metadataConfig;

  ServerlessComputeDetector() {
    this.environmentVariables = EnvironmentVariables.DEFAULT_INSTANCE;
    this.metadataConfig = GCPMetadataConfig.DEFAULT_INSTANCE;
  }

  // for testing only
  ServerlessComputeDetector(
      EnvironmentVariables environmentVariables, GCPMetadataConfig metadataConfig) {
    this.environmentVariables = environmentVariables;
    this.metadataConfig = metadataConfig;
  }

  public Optional<String> getServerlessComputeName() {
    return Optional.ofNullable(this.environmentVariables.get("K_SERVICE"));
  }

  public Optional<String> getServerlessComputeRevision() {
    return Optional.ofNullable(this.environmentVariables.get("K_REVISION"));
  }

  public Optional<String> getAvailabilityZone() {
    return CloudLocationUtil.getAvailabilityZoneFromMetadata(this.metadataConfig);
  }

  public Optional<String> getCloudRegion() {
    return CloudLocationUtil.getCloudRegionFromMetadataUsingZone(this.metadataConfig);
  }

  public Optional<String> getInstanceId() {
    return Optional.ofNullable(this.metadataConfig.getInstanceId());
  }
}
