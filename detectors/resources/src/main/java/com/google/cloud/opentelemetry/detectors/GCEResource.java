/*
 * Copyright 2021 Google
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
import io.opentelemetry.sdk.resources.ResourceProvider;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

public final class GCEResource extends ResourceProvider {
  private final GCPMetadataConfig metadata;

  public GCEResource() {
    this(GCPMetadataConfig.DEFAULT_INSTANCE);
  }

  // For testing only
  public GCEResource(GCPMetadataConfig metadataConfig) {
    this.metadata = metadataConfig;
  }

  @Override
  public Attributes getAttributes() {
    if (!metadata.isRunningOnGcp()) {
      return Attributes.empty();
    }

    AttributesBuilder attrBuilders = Attributes.builder();
    attrBuilders.put(SemanticAttributes.CLOUD_PROVIDER, "gcp");

    String projectId = metadata.getProjectId();
    if (!projectId.isEmpty()) {
      attrBuilders.put(SemanticAttributes.CLOUD_ACCOUNT_ID, projectId);
    }

    // Example zone: australia-southeast1-a
    String zone = metadata.getZone();
    if (!zone.isEmpty()) {
      attrBuilders.put(SemanticAttributes.CLOUD_ZONE, zone);

      // Parsing required to scope up to a region
      String[] splitArr = zone.split("-");
      if (splitArr.length > 2) {
        attrBuilders.put(SemanticAttributes.CLOUD_REGION, splitArr[0] + "-" + splitArr[1]);
      }
    }

    String instanceId = metadata.getInstanceId();
    if (!instanceId.isEmpty()) {
      attrBuilders.put(SemanticAttributes.HOST_ID, instanceId);
    }

    String instanceName = metadata.getInstanceName();
    if (!instanceName.isEmpty()) {
      attrBuilders.put(SemanticAttributes.HOST_NAME, instanceName);
    }

    String hostType = metadata.getMachineType();
    if (!hostType.isEmpty()) {
      attrBuilders.put(SemanticAttributes.HOST_TYPE, hostType);
    }

    return attrBuilders.build();
  }
}
