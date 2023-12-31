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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.logging.Logger;

/**
 * This class is used to detect the correct GCP compute platform resource. Supports detection of
 * Google Compute Engine (GCE), Google Kubernetes Engine (GKE), Google Cloud Functions (GCF), Google
 * App Engine (GAE) and Google Cloud Run (GCR).
 */
public class GCPResourceProvider implements ResourceProvider {

  private static final Logger LOGGER = Logger.getLogger(GCPResourceProvider.class.getSimpleName());

  /**
   * Generates and returns the attributes for the resource. The attributes vary depending on the
   * type of resource detected.
   *
   * @return The {@link Attributes} for the detected resource.
   */
  public Attributes getAttributes() {
    GCPPlatformDetector.GCPPlatform detectedPlatform =
        GCPPlatformDetector.DEFAULT_INSTANCE.detectPlatform();
    if (detectedPlatform == GCPPlatformDetector.GCPPlatform.UNKNOWN_PLATFORM) {
      return Attributes.empty();
    }

    // This is running on some sort of GCPCompute - figure out the platform
    AttributesBuilder attrBuilder = Attributes.builder();
    attrBuilder.put(ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP);

    switch (detectedPlatform) {
      case GOOGLE_KUBERNETES_ENGINE:
        addGKEAttributes(attrBuilder);
        break;
      case GOOGLE_CLOUD_RUN:
        addGCRAttributes(attrBuilder);
        break;
      case GOOGLE_CLOUD_FUNCTIONS:
        addGCFAttributes(attrBuilder);
        break;
      case GOOGLE_APP_ENGINE:
        addGAEAttributes(attrBuilder);
        break;
      case GOOGLE_COMPUTE_ENGINE:
      default:
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

    GCEDetector.DEFAULT_INSTANCE
        .getProjectId()
        .ifPresent(projectId -> attrBuilder.put(ResourceAttributes.CLOUD_ACCOUNT_ID, projectId));
    GCEDetector.DEFAULT_INSTANCE
        .getAvailabilityZone()
        .ifPresent(zone -> attrBuilder.put(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, zone));
    GCEDetector.DEFAULT_INSTANCE
        .getCloudRegion()
        .ifPresent(region -> attrBuilder.put(ResourceAttributes.CLOUD_REGION, region));
    GCEDetector.DEFAULT_INSTANCE
        .getInstanceId()
        .ifPresent(instanceId -> attrBuilder.put(ResourceAttributes.HOST_ID, instanceId));
    GCEDetector.DEFAULT_INSTANCE
        .getInstanceName()
        .ifPresent(instanceName -> attrBuilder.put(ResourceAttributes.HOST_NAME, instanceName));
    GCEDetector.DEFAULT_INSTANCE
        .getMachineType()
        .ifPresent(machineType -> attrBuilder.put(ResourceAttributes.HOST_TYPE, machineType));
  }

  /**
   * Updates the attributes with the required keys for a GKE (Google Kubernetes Engine) environment.
   * The attributes are not updated in case the environment is not deemed to be GKE.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private void addGKEAttributes(AttributesBuilder attrBuilder) {
    attrBuilder.put(
        ResourceAttributes.CLOUD_PLATFORM,
        ResourceAttributes.CloudPlatformValues.GCP_KUBERNETES_ENGINE);
    attrBuilder.put(ResourceAttributes.K8S_POD_NAME, GKEDetector.DEFAULT_INSTANCE.getPodName());
    GKEDetector.DEFAULT_INSTANCE
        .getNamespace()
        .ifPresent(namespace -> attrBuilder.put(ResourceAttributes.K8S_NAMESPACE_NAME, namespace));
    GKEDetector.DEFAULT_INSTANCE
        .getContainerName()
        .ifPresent(
            containerName -> attrBuilder.put(ResourceAttributes.K8S_CONTAINER_NAME, containerName));
    GKEDetector.DEFAULT_INSTANCE
        .getClusterName()
        .ifPresent(
            clusterName -> attrBuilder.put(ResourceAttributes.K8S_CLUSTER_NAME, clusterName));
    GKEDetector.GKEZoneOrRegion zoneOrRegion = GKEDetector.DEFAULT_INSTANCE.getGKEClusterLocation();
    switch (zoneOrRegion.getLocationType()) {
      case REGION:
        attrBuilder.put(ResourceAttributes.CLOUD_REGION, zoneOrRegion.getClusterLocation());
        break;
      case ZONE:
        attrBuilder.put(
            ResourceAttributes.CLOUD_AVAILABILITY_ZONE, zoneOrRegion.getClusterLocation());
        break;
      case UNDEFINED:
      default:
        // TODO: Figure out how to handle unexpected conditions like this - Issue #183
        LOGGER.severe(
            String.format(
                "Unrecognized format for cluster location: %s", zoneOrRegion.getClusterLocation()));
    }
  }

  /**
   * Updates the attributes with the required keys for a GCR (Google Cloud Run) environment. The
   * attributes are not updated in case the environment is not deemed to be GCR.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private void addGCRAttributes(AttributesBuilder attrBuilder) {
    attrBuilder.put(
        ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.GCP_CLOUD_RUN);
    addCommonAttributesForServerlessCompute(attrBuilder);
  }

  /**
   * Updates the attributes with the required keys for a GCF (Google Cloud Functions) environment.
   * The attributes are not updated in case the environment is not deemed to be GCF.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private void addGCFAttributes(AttributesBuilder attrBuilder) {
    attrBuilder.put(
        ResourceAttributes.CLOUD_PLATFORM,
        ResourceAttributes.CloudPlatformValues.GCP_CLOUD_FUNCTIONS);
    addCommonAttributesForServerlessCompute(attrBuilder);
  }

  /**
   * Updates the attributes with the required keys for a GAE (Google App Engine) environment. The
   * attributes are not updated in case the environment is not deemed to be GAE.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private void addGAEAttributes(AttributesBuilder attrBuilder) {
    attrBuilder.put(
        ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.GCP_APP_ENGINE);
    GAEDetector.DEFAULT_INSTANCE
        .getAppModuleName()
        .ifPresent(appName -> attrBuilder.put(ResourceAttributes.FAAS_NAME, appName));
    GAEDetector.DEFAULT_INSTANCE
        .getAppVersion()
        .ifPresent(appVersion -> attrBuilder.put(ResourceAttributes.FAAS_VERSION, appVersion));
    GAEDetector.DEFAULT_INSTANCE
        .getAppInstanceId()
        .ifPresent(
            appInstanceId -> attrBuilder.put(ResourceAttributes.FAAS_INSTANCE, appInstanceId));
    GAEDetector.DEFAULT_INSTANCE
        .getCloudRegion()
        .ifPresent(cloudRegion -> attrBuilder.put(ResourceAttributes.CLOUD_REGION, cloudRegion));
    GAEDetector.DEFAULT_INSTANCE
        .getAvailabilityZone()
        .ifPresent(
            cloudAvailabilityZone ->
                attrBuilder.put(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, cloudAvailabilityZone));
  }

  /**
   * This function adds common attributes required for most serverless compute platforms within GCP.
   * Currently, these attributes are required for both GCF and GCR.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private void addCommonAttributesForServerlessCompute(AttributesBuilder attrBuilder) {
    ServerlessComputeDetector.DEFAULT_INSTANCE
        .getServerlessComputeName()
        .ifPresent(
            serverlessComputeName ->
                attrBuilder.put(ResourceAttributes.FAAS_NAME, serverlessComputeName));
    ServerlessComputeDetector.DEFAULT_INSTANCE
        .getServerlessComputeRevision()
        .ifPresent(
            serverlessComputeVersion ->
                attrBuilder.put(ResourceAttributes.FAAS_VERSION, serverlessComputeVersion));
    ServerlessComputeDetector.DEFAULT_INSTANCE
        .getInstanceId()
        .ifPresent(instanceId -> attrBuilder.put(ResourceAttributes.FAAS_INSTANCE, instanceId));
    ServerlessComputeDetector.DEFAULT_INSTANCE
        .getAvailabilityZone()
        .ifPresent(zone -> attrBuilder.put(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, zone));
    ServerlessComputeDetector.DEFAULT_INSTANCE
        .getCloudRegion()
        .ifPresent(region -> attrBuilder.put(ResourceAttributes.CLOUD_REGION, region));
  }
}
