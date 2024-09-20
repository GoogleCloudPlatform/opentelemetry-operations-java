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
package com.google.cloud.opentelemetry.detection;

import java.util.HashMap;
import java.util.Map;

final class GoogleCloudRunJob implements DetectedPlatform {
  private final GCPMetadataConfig metadataConfig;
  private final EnvironmentVariables environmentVariables;
  private final Map<String, String> availableAttributes;

  GoogleCloudRunJob(EnvironmentVariables environmentVariables, GCPMetadataConfig metadataConfig) {
    this.metadataConfig = metadataConfig;
    this.environmentVariables = environmentVariables;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, String> prepareAttributes() {
    Map<String, String> map = new HashMap<>();
    map.put(AttributeKeys.SERVERLESS_COMPUTE_NAME, this.environmentVariables.get("CLOUD_RUN_JOB"));
    map.put(
        AttributeKeys.GCR_JOB_EXECUTION_KEY, this.environmentVariables.get("CLOUD_RUN_EXECUTION"));
    map.put(
        AttributeKeys.GCR_JOB_TASK_INDEX, this.environmentVariables.get("CLOUD_RUN_TASK_INDEX"));
    map.put(AttributeKeys.SERVERLESS_COMPUTE_INSTANCE_ID, this.metadataConfig.getInstanceId());
    map.put(AttributeKeys.SERVERLESS_COMPUTE_CLOUD_REGION, this.metadataConfig.getRegionFromZone());
    return map;
  }

  @Override
  public GCPPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN_JOB;
  }

  @Override
  public String getProjectId() {
    return metadataConfig.getProjectId();
  }

  @Override
  public Map<String, String> getAttributes() {
    return this.availableAttributes;
  }
}
