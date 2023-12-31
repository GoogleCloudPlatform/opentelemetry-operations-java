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

public final class GAEDetector {
  public static final GAEDetector DEFAULT_INSTANCE = new GAEDetector();

  private final EnvironmentVariables environmentVariables;
  private final GCPMetadataConfig metadataConfig;

  GAEDetector() {
    this.environmentVariables = EnvironmentVariables.DEFAULT_INSTANCE;
    this.metadataConfig = GCPMetadataConfig.DEFAULT_INSTANCE;
  }

  // for testing only
  GAEDetector(EnvironmentVariables environmentVariables, GCPMetadataConfig metadataConfig) {
    this.environmentVariables = environmentVariables;
    this.metadataConfig = metadataConfig;
  }

  public Optional<String> getAppModuleName() {
    return Optional.ofNullable(this.environmentVariables.get("GAE_SERVICE"));
  }

  public Optional<String> getAppVersion() {
    return Optional.ofNullable(this.environmentVariables.get("GAE_VERSION"));
  }

  public Optional<String> getAppInstanceId() {
    return Optional.ofNullable(this.environmentVariables.get("GAE_INSTANCE"));
  }

  public Optional<String> getAvailabilityZone() {
    return CloudLocationUtil.getAvailabilityZoneFromMetadata(this.metadataConfig);
  }

  public Optional<String> getCloudRegion() {
    if (this.environmentVariables.get("GAE_ENV") != null
        && this.environmentVariables.get("GAE_ENV").equals("standard")) {
      return CloudLocationUtil.getCloudRegionFromMetadataUsingRegion(this.metadataConfig);
    } else {
      return CloudLocationUtil.getCloudRegionFromMetadataUsingZone(this.metadataConfig);
    }
  }
}
