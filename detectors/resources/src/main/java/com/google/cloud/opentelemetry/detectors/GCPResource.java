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
import io.opentelemetry.api.internal.StringUtils;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.logging.Logger;

/**
 * This class is used to detect the correct GCP compute platform resource. Supports detection of
 * Google Compute Engine (GCE), Google Kubernetes Engine (GKE), Google Cloud Functions (GCF), Google
 * App Engine (GAE) and Google Cloud Run (GCR).
 */
public class GCPResource implements ResourceProvider {
  private final GCPMetadataConfig metadata;
  private final EnvVars envVars;

  private static final Logger LOGGER = Logger.getLogger(GCPResource.class.getSimpleName());

  public GCPResource() {
    this.metadata = GCPMetadataConfig.DEFAULT_INSTANCE;
    this.envVars = EnvVars.DEFAULT_INSTANCE;
  }

  // for testing only
  GCPResource(GCPMetadataConfig metadata, EnvVars envVars) {
    this.metadata = metadata;
    this.envVars = envVars;
  }

  /**
   * Generates and returns the attributes for the resource. The attributes vary depending on the
   * type of resource detected.
   *
   * @return The {@link Attributes} for the detected resource.
   */
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

  /**
   * Updates the attributes with the required keys for a GKE (Google Kubernetes Engine) environment.
   * The attributes are not updated in case the environment is not deemed to be GKE.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   * @return a boolean indicating if the environment was determined to be GKE and GKE specific
   *     attributes were applied.
   */
  private boolean generateGKEAttributesIfApplicable(AttributesBuilder attrBuilder) {
    if (envVars.get("KUBERNETES_SERVICE_HOST") != null) {
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

      String instanceId = metadata.getInstanceId();
      if (instanceId != null) {
        attrBuilder.put(ResourceAttributes.HOST_ID, instanceId);
      }

      String clusterLocation = metadata.getClusterLocation();
      assignGKEAvailabilityZoneOrRegion(clusterLocation, attrBuilder);

      String clusterName = metadata.getClusterName();
      if (clusterName != null && !clusterName.isEmpty()) {
        attrBuilder.put(ResourceAttributes.K8S_CLUSTER_NAME, clusterName);
      }
      return true;
    }
    return false;
  }

  /**
   * Function that assigns either the cloud region or cloud availability zone depending on whether
   * the cluster is regional or zonal respectively. Assigns both values if the cluster location
   * passed is in an unexpected format.
   *
   * @param clusterLocation The location of the GKE cluster. Can either be an availability zone or a
   *     region.
   * @param attributesBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private void assignGKEAvailabilityZoneOrRegion(
      String clusterLocation, AttributesBuilder attributesBuilder) {
    long dashCount =
        StringUtils.isNullOrEmpty(clusterLocation)
            ? 0
            : clusterLocation.chars().filter(ch -> ch == '-').count();
    switch ((int) dashCount) {
      case 1:
        // this is a region
        attributesBuilder.put(ResourceAttributes.CLOUD_REGION, clusterLocation);
        break;
      case 2:
        // this is a zone
        attributesBuilder.put(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, clusterLocation);
        break;
      default:
        // TODO: Figure out how to handle unexpected conditions like this - Issue #183
        LOGGER.severe(
            String.format("Unrecognized format for cluster location: %s", clusterLocation));
    }
  }

  /**
   * Updates the attributes with the required keys for a GCR (Google Cloud Run) environment. The
   * attributes are not updated in case the environment is not deemed to be GCR.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   * @return a boolean indicating if the environment was determined to be GCR and GCR specific
   *     attributes were applied.
   */
  private boolean generateGCRAttributesIfApplicable(AttributesBuilder attrBuilder) {
    if (envVars.get("K_CONFIGURATION") != null && envVars.get("FUNCTION_TARGET") == null) {
      // add the resource attributes for Cloud Run
      attrBuilder.put(
          ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.GCP_CLOUD_RUN);

      updateCommonAttributesForServerlessCompute(attrBuilder);
      return true;
    }
    return false;
  }

  /**
   * Updates the attributes with the required keys for a GCF (Google Cloud Functions) environment.
   * The attributes are not updated in case the environment is not deemed to be GCF.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   * @return a boolean indicating if the environment was determined to be GCF and GCF specific
   *     attributes were applied.
   */
  private boolean generateGCFAttributesIfApplicable(AttributesBuilder attrBuilder) {
    if (envVars.get("FUNCTION_TARGET") != null) {
      // add the resource attributes for Cloud Function
      attrBuilder.put(
          ResourceAttributes.CLOUD_PLATFORM,
          ResourceAttributes.CloudPlatformValues.GCP_CLOUD_FUNCTIONS);

      updateCommonAttributesForServerlessCompute(attrBuilder);
      return true;
    }
    return false;
  }

  /**
   * This function adds common attributes required for most serverless compute platforms within GCP.
   * Currently, these attributes are required for both GCF and GCR.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private void updateCommonAttributesForServerlessCompute(AttributesBuilder attrBuilder) {
    String serverlessComputeName = envVars.get("K_SERVICE");
    if (serverlessComputeName != null) {
      attrBuilder.put(ResourceAttributes.FAAS_NAME, serverlessComputeName);
    }

    String serverlessComputeVersion = envVars.get("K_REVISION");
    if (serverlessComputeVersion != null) {
      attrBuilder.put(ResourceAttributes.FAAS_VERSION, serverlessComputeVersion);
    }

    AttributesExtractorUtil.addAvailabilityZoneFromMetadata(attrBuilder, metadata);
    AttributesExtractorUtil.addCloudRegionFromMetadataUsingZone(attrBuilder, metadata);
    AttributesExtractorUtil.addInstanceIdFromMetadata(attrBuilder, metadata);
  }

  /**
   * Updates the attributes with the required keys for a GAE (Google App Engine) environment. The
   * attributes are not updated in case the environment is not deemed to be GAE.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   * @return a boolean indicating if the environment was determined to be GAE and GAE specific
   *     attributes were applied.
   */
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
      AttributesExtractorUtil.addAvailabilityZoneFromMetadata(attrBuilder, metadata);
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
