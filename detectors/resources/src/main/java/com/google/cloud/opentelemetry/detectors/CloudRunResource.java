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

public final class CloudRunResource implements ResourceProvider {
  private final GCPMetadataConfig metadata;
  private final EnvVars envVars;

  public CloudRunResource() {
    this.metadata = GCPMetadataConfig.DEFAULT_INSTANCE;
    this.envVars = EnvVars.DEFAULT_INSTANCE;
  }

  // For testing only
  CloudRunResource(GCPMetadataConfig metadata, EnvVars envVars) {
    this.metadata = metadata;
    this.envVars = envVars;
  }

  public Attributes getAttributes() {
    if (!metadata.isRunningOnGcp()) {
      return Attributes.empty();
    }

    AttributesBuilder attrBuilders = Attributes.builder();
    attrBuilders.put(ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP);

    if (envVars.get("K_CONFIGURATION") != null) {
      // add the resource attributes for CloudFunctions
      attrBuilders.put(
          ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.GCP_CLOUD_RUN);

      String zone = metadata.getZone();
      if (zone != null) {
        attrBuilders.put(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, zone);

        // Parsing required to scope up to a region
        String[] splitArr = zone.split("-");
        if (splitArr.length > 2) {
          attrBuilders.put(ResourceAttributes.CLOUD_REGION, splitArr[0] + "-" + splitArr[1]);
        }
      }

      String cloudRunService = envVars.get("K_SERVICE");
      if (cloudRunService != null) {
        attrBuilders.put(ResourceAttributes.FAAS_NAME, cloudRunService);
      }

      String cloudRunServiceRevision = envVars.get("K_REVISION");
      if (cloudRunServiceRevision != null) {
        attrBuilders.put(ResourceAttributes.FAAS_VERSION, cloudRunServiceRevision);
      }

      String instanceId = metadata.getInstanceId();
      if (instanceId != null) {
        attrBuilders.put(ResourceAttributes.FAAS_ID, instanceId);
      }
    }

    return attrBuilders.build();
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    return Resource.create(getAttributes());
  }
}
