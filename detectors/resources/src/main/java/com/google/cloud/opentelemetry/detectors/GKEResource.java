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
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public final class GKEResource implements ResourceProvider {
  private final GCPMetadataConfig metadata;
  private final EnvVars envVars;

  public GKEResource() {
    this.metadata = GCPMetadataConfig.DEFAULT_INSTANCE;
    this.envVars = EnvVars.DEFAULT_INSTANCE;
  }

  // For testing only
  public GKEResource(GCPMetadataConfig metadataConfig, EnvVars envVars) {
    this.metadata = metadataConfig;
    this.envVars = envVars;
  }

  public Attributes getAttributes() {
    GCEResource gce = new GCEResource(this.metadata);

    Attributes gceAttributes = gce.getAttributes();

    if (envVars.get("KUBERNETES_SERVICE_HOST") == null) {
      return gceAttributes;
    }

    AttributesBuilder attrBuilders = Attributes.builder();
    attrBuilders.putAll(gceAttributes);

    attrBuilders.put(ResourceAttributes.K8S_NAMESPACE_NAME, envVars.get("NAMESPACE"));
    attrBuilders.put(ResourceAttributes.K8S_POD_NAME, envVars.get("HOSTNAME"));

    String containerName = envVars.get("CONTAINER_NAME");
    if (containerName != null && !containerName.isEmpty()) {
      attrBuilders.put(ResourceAttributes.K8S_CONTAINER_NAME, containerName);
    }

    String clusterName = metadata.getClusterName();
    if (clusterName != null && !clusterName.isEmpty()) {
      attrBuilders.put(ResourceAttributes.K8S_CLUSTER_NAME, clusterName);
    }

    return attrBuilders.build();
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    return Resource.create(getAttributes());
  }
}
