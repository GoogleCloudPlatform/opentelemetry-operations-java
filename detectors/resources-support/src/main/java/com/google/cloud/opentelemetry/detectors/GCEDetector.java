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

public final class GCEDetector {
  public static final GCEDetector DEFAULT_INSTANCE = new GCEDetector();

  private final GCPMetadataConfig metadataConfig;

  GCEDetector() {
    this.metadataConfig = GCPMetadataConfig.DEFAULT_INSTANCE;
  }

  // for testing only
  GCEDetector(GCPMetadataConfig metadataConfig) {
    this.metadataConfig = metadataConfig;
  }

  public Optional<String> getProjectId() {
    return Optional.ofNullable(this.metadataConfig.getProjectId());
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

  public Optional<String> getInstanceName() {
    return Optional.ofNullable(this.metadataConfig.getInstanceName());
  }

  public Optional<String> getMachineType() {
    return Optional.ofNullable(this.metadataConfig.getMachineType());
  }
}
