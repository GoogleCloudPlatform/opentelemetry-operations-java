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

public class GCPComputeResource implements ResourceProvider {
  private final GCPMetadataConfig metadata;
  private final EnvVars envVars;

  public GCPComputeResource() {
    this.metadata = GCPMetadataConfig.DEFAULT_INSTANCE;
    this.envVars = EnvVars.DEFAULT_INSTANCE;
  }

  // for testing only
  GCPComputeResource(GCPMetadataConfig metadata, EnvVars envVars) {
    this.metadata = metadata;
    this.envVars = envVars;
  }

  public Attributes getAttributes() {
    if (!metadata.isRunningOnGcp()) {
      return Attributes.empty();
    }

    // This is running on some sort of GCPCompute - figure out the platform
    AttributesBuilder attrBuilder = Attributes.builder();
    attrBuilder.put(ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP);

    if (!(generateGKEAttributesIfApplicable(attrBuilder)
        || generateGCRAttributesIfApplicable(attrBuilder)
        || generateGCFAttributesIfApplicable(attrBuilder)
        || generateGAEAttributesIfApplicable(attrBuilder))) {
      // none of the above GCP platforms is applicable, default to GCE
      addGCEAttributes(attrBuilder);
    }

    return attrBuilder.build();
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    return Resource.create(getAttributes());
  }

  /**
   * Updates the attributes builder with required attributes for GCE resource, if GCE resource is
   * applicable. By default, if the resource is running on GCP, it is assumed to be GCE. This means
   * additional attributes are added/overwritten if later on, the resource is identified to be some
   * other platform - like GKE, GAE, etc.
   */
  private void addGCEAttributes(AttributesBuilder attrBuilder) {
    attrBuilder.put(
        ResourceAttributes.CLOUD_PLATFORM,
        ResourceAttributes.CloudPlatformValues.GCP_COMPUTE_ENGINE);

    String projectId = metadata.getProjectId();
    if (projectId != null) {
      attrBuilder.put(ResourceAttributes.CLOUD_ACCOUNT_ID, projectId);
    }

    AttributesExtractorUtil.addAvailabilityZoneFromMetadata(attrBuilder, metadata);
    AttributesExtractorUtil.addCloudRegionFromMetadataUsingZone(attrBuilder, metadata);

    String instanceId = metadata.getInstanceId();
    if (instanceId != null) {
      attrBuilder.put(ResourceAttributes.HOST_ID, instanceId);
    }

    String instanceName = metadata.getInstanceName();
    if (instanceName != null) {
      attrBuilder.put(ResourceAttributes.HOST_NAME, instanceName);
    }

    String hostType = metadata.getMachineType();
    if (hostType != null) {
      attrBuilder.put(ResourceAttributes.HOST_TYPE, hostType);
    }
  }

  private boolean generateGKEAttributesIfApplicable(AttributesBuilder attrBuilder) {
    if (envVars.get("KUBERNETES_SERVICE_HOST") != null) {
      // add all gce attributes
      addGCEAttributes(attrBuilder);
      // overwrite/add GKE specific attributes
      attrBuilder.put(
          ResourceAttributes.CLOUD_PLATFORM,
          ResourceAttributes.CloudPlatformValues.GCP_KUBERNETES_ENGINE);
      String podName = envVars.get("POD_NAME");
      if (podName != null && !podName.isEmpty()) {
        attrBuilder.put(ResourceAttributes.K8S_POD_NAME, podName);
      } else {
        // If nothing else is set, at least use hostname for pod name.
        attrBuilder.put(ResourceAttributes.K8S_POD_NAME, envVars.get("HOSTNAME"));
      }

      String namespace = envVars.get("NAMESPACE");
      if (namespace != null && !namespace.isEmpty()) {
        attrBuilder.put(ResourceAttributes.K8S_NAMESPACE_NAME, namespace);
      }

      String containerName = envVars.get("CONTAINER_NAME");
      if (containerName != null && !containerName.isEmpty()) {
        attrBuilder.put(ResourceAttributes.K8S_CONTAINER_NAME, containerName);
      }

      String clusterName = metadata.getClusterName();
      if (clusterName != null && !clusterName.isEmpty()) {
        attrBuilder.put(ResourceAttributes.K8S_CLUSTER_NAME, clusterName);
      }
      return true;
    }
    return false;
  }

  private boolean generateGCRAttributesIfApplicable(AttributesBuilder attrBuilders) {
    if (envVars.get("K_CONFIGURATION") != null) {
      // add the resource attributes for Cloud Run
      attrBuilders.put(
          ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.GCP_CLOUD_RUN);

      String cloudRunService = envVars.get("K_SERVICE");
      if (cloudRunService != null) {
        attrBuilders.put(ResourceAttributes.FAAS_NAME, cloudRunService);
      }

      String cloudRunServiceRevision = envVars.get("K_REVISION");
      if (cloudRunServiceRevision != null) {
        attrBuilders.put(ResourceAttributes.FAAS_VERSION, cloudRunServiceRevision);
      }

      AttributesExtractorUtil.addAvailabilityZoneFromMetadata(attrBuilders, metadata);
      AttributesExtractorUtil.addCloudRegionFromMetadataUsingZone(attrBuilders, metadata);
      AttributesExtractorUtil.addInstanceIdFromMetadata(attrBuilders, metadata);
      return true;
    }
    return false;
  }

  private boolean generateGCFAttributesIfApplicable(AttributesBuilder attrBuilder) {
    if (envVars.get("FUNCTION_TARGET") != null) {
      // add the resource attributes for Cloud Function
      attrBuilder.put(
          ResourceAttributes.CLOUD_PLATFORM,
          ResourceAttributes.CloudPlatformValues.GCP_CLOUD_FUNCTIONS);

      String cloudFunctionName = envVars.get("K_SERVICE");
      if (cloudFunctionName != null) {
        attrBuilder.put(ResourceAttributes.FAAS_NAME, cloudFunctionName);
      }

      String cloudFunctionVersion = envVars.get("K_REVISION");
      if (cloudFunctionVersion != null) {
        attrBuilder.put(ResourceAttributes.FAAS_VERSION, cloudFunctionVersion);
      }

      AttributesExtractorUtil.addAvailabilityZoneFromMetadata(attrBuilder, metadata);
      AttributesExtractorUtil.addCloudRegionFromMetadataUsingZone(attrBuilder, metadata);
      AttributesExtractorUtil.addInstanceIdFromMetadata(attrBuilder, metadata);
      return true;
    }
    return false;
  }

  private boolean generateGAEAttributesIfApplicable(AttributesBuilder attrBuilder) {
    if (envVars.get("GAE_SERVICE") != null) {
      // add the resource attributes for App Engine
      attrBuilder.put(
          ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.GCP_APP_ENGINE);

      String appModuleName = envVars.get("GAE_SERVICE");
      if (appModuleName != null) {
        attrBuilder.put(ResourceAttributes.FAAS_NAME, appModuleName);
      }

      String appVersionId = envVars.get("GAE_VERSION");
      if (appVersionId != null) {
        attrBuilder.put(ResourceAttributes.FAAS_VERSION, appVersionId);
      }

      String appInstanceId = envVars.get("GAE_INSTANCE");
      if (appInstanceId != null) {
        attrBuilder.put(ResourceAttributes.FAAS_ID, appInstanceId);
      }
      updateAttributesWithRegion(attrBuilder);
      return true;
    }
    return false;
  }

  /**
   * Selects the correct method to extract the region, depending on the GAE environment.
   *
   * @param attributesBuilder The {@link AttributesBuilder} object to which the extracted region
   *     would be added.
   */
  private void updateAttributesWithRegion(AttributesBuilder attributesBuilder) {
    if (envVars.get("GAE_ENV") != null && envVars.get("GAE_ENV").equals("standard")) {
      AttributesExtractorUtil.addCloudRegionFromMetadataUsingRegion(attributesBuilder, metadata);
    } else {
      AttributesExtractorUtil.addCloudRegionFromMetadataUsingZone(attributesBuilder, metadata);
    }
  }
}
