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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to help add attributes applicable to serverless compute in GCP to a given resource.
 */
abstract class GoogleServerlessCompute implements DetectedPlatform {
  private final EnvironmentVariables environmentVariables;
  private final GCPMetadataConfig metadataConfig;
  private final Map<String, String> availableAttributes;

  GoogleServerlessCompute() {
    this.environmentVariables = EnvironmentVariables.DEFAULT_INSTANCE;
    this.metadataConfig = GCPMetadataConfig.DEFAULT_INSTANCE;
    this.availableAttributes = prepareAttributes();
  }

  // for testing only
  GoogleServerlessCompute(
      EnvironmentVariables environmentVariables, GCPMetadataConfig metadataConfig) {
    this.environmentVariables = environmentVariables;
    this.metadataConfig = metadataConfig;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, String> prepareAttributes() {
    Map<String, String> map = new HashMap<>();
    map.put(AttributeKeys.SERVERLESS_COMPUTE_NAME, this.environmentVariables.get("K_SERVICE"));
    map.put(AttributeKeys.SERVERLESS_COMPUTE_REVISION, this.environmentVariables.get("K_REVISION"));
    map.put(AttributeKeys.SERVERLESS_COMPUTE_AVAILABILITY_ZONE, this.metadataConfig.getZone());
    map.put(AttributeKeys.SERVERLESS_COMPUTE_CLOUD_REGION, this.metadataConfig.getRegionFromZone());
    map.put(AttributeKeys.SERVERLESS_COMPUTE_INSTANCE_ID, this.metadataConfig.getInstanceId());
    return Collections.unmodifiableMap(map);
  }

  @Override
  public Map<String, String> getAttributes() {
    return this.availableAttributes;
  }
}
