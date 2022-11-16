/*
 * Copyright 2022 Google
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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class GAEResource implements ResourceProvider {
  private final GCPMetadataConfig metadata;
  private final EnvVars envVars;

  public GAEResource() {
    this.metadata = GCPMetadataConfig.DEFAULT_INSTANCE;
    this.envVars = EnvVars.DEFAULT_INSTANCE;
  }

  // For testing only
  GAEResource(GCPMetadataConfig metadata, EnvVars envVars) {
    this.metadata = metadata;
    this.envVars = envVars;
  }

  public Attributes getAttributes() {
    if (!metadata.isRunningOnGcp()) {
      return Attributes.empty();
    }

    AttributesBuilder attrBuilders = Attributes.builder();
    attrBuilders.put(ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP);

    if (envVars.get("GAE_SERVICE") != null) {
      // add the resource attributes for App Engine
      attrBuilders.put(
          ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.GCP_APP_ENGINE);

      String appModuleName = envVars.get("GAE_SERVICE");
      if (appModuleName != null) {
        attrBuilders.put(ResourceAttributes.FAAS_NAME, appModuleName);
      }

      String appVersionId = envVars.get("GAE_VERSION");
      if (appVersionId != null) {
        attrBuilders.put(ResourceAttributes.FAAS_VERSION, appVersionId);
      }

      String appInstanceId = envVars.get("GAE_INSTANCE");
      if (appInstanceId != null) {
        attrBuilders.put(ResourceAttributes.FAAS_ID, appInstanceId);
      }

      AttributesExtractorUtil.addCloudRegionFromMetadataUsingZone(attrBuilders, metadata);
    }
    return attrBuilders.build();
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    return Resource.create(getAttributes());
  }
}
