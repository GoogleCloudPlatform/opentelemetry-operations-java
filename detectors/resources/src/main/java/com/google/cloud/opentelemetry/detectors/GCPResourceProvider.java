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

import static com.google.cloud.opentelemetry.detectors.AttributeKeys.*;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * This class is used to detect the correct GCP compute platform resource. Supports detection of
 * Google Compute Engine (GCE), Google Kubernetes Engine (GKE), Google Cloud Functions (GCF), Google
 * App Engine (GAE) and Google Cloud Run (GCR).
 */
public class GCPResourceProvider implements ResourceProvider {

  private static final Logger LOGGER = Logger.getLogger(GCPResourceProvider.class.getSimpleName());
  private final GCPPlatformDetector detector;

  // for testing only
  GCPResourceProvider(GCPPlatformDetector detector) {
    this.detector = detector;
  }

  public GCPResourceProvider() {
    this.detector = GCPPlatformDetector.DEFAULT_INSTANCE;
  }

  /**
   * Generates and returns the attributes for the resource. The attributes vary depending on the
   * type of resource detected.
   *
   * @return The {@link Attributes} for the detected resource.
   */
  public Attributes getAttributes() {
    DetectedPlatform detectedPlatform = detector.detectPlatform();
    if (detectedPlatform.getSupportedPlatform()
        == GCPPlatformDetector.SupportedPlatform.UNKNOWN_PLATFORM) {
      return Attributes.empty();
    }

    // This is running on some sort of GCPCompute - figure out the platform
    AttributesBuilder attrBuilder = Attributes.builder();
    attrBuilder.put(ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP);

    switch (detectedPlatform.getSupportedPlatform()) {
      case GOOGLE_KUBERNETES_ENGINE:
        addGKEAttributes(attrBuilder, detectedPlatform.getAttributes());
        break;
      case GOOGLE_CLOUD_RUN:
        addGCRAttributes(attrBuilder, detectedPlatform.getAttributes());
        break;
      case GOOGLE_CLOUD_FUNCTIONS:
        addGCFAttributes(attrBuilder, detectedPlatform.getAttributes());
        break;
      case GOOGLE_APP_ENGINE:
        addGAEAttributes(attrBuilder, detectedPlatform.getAttributes());
        break;
      case GOOGLE_COMPUTE_ENGINE:
      default:
        addGCEAttributes(attrBuilder, detectedPlatform.getAttributes());
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
  private void addGCEAttributes(AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    attrBuilder.put(
        ResourceAttributes.CLOUD_PLATFORM,
        ResourceAttributes.CloudPlatformValues.GCP_COMPUTE_ENGINE);

    Optional.ofNullable(attributesMap.get(GCE_PROJECT_ID))
        .ifPresent(projectId -> attrBuilder.put(ResourceAttributes.CLOUD_ACCOUNT_ID, projectId));
    Optional.ofNullable(attributesMap.get(GCE_AVAILABILITY_ZONE))
        .ifPresent(zone -> attrBuilder.put(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, zone));
    Optional.ofNullable(attributesMap.get(GCE_CLOUD_REGION))
        .ifPresent(region -> attrBuilder.put(ResourceAttributes.CLOUD_REGION, region));
    Optional.ofNullable(attributesMap.get(GCE_INSTANCE_ID))
        .ifPresent(instanceId -> attrBuilder.put(ResourceAttributes.HOST_ID, instanceId));
    Optional.ofNullable(attributesMap.get(GCE_INSTANCE_NAME))
        .ifPresent(instanceName -> attrBuilder.put(ResourceAttributes.HOST_NAME, instanceName));
    Optional.ofNullable(attributesMap.get(GCE_MACHINE_TYPE))
        .ifPresent(machineType -> attrBuilder.put(ResourceAttributes.HOST_TYPE, machineType));
  }

  /**
   * Updates the attributes with the required keys for a GKE (Google Kubernetes Engine) environment.
   * The attributes are not updated in case the environment is not deemed to be GKE.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private void addGKEAttributes(AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    attrBuilder.put(
        ResourceAttributes.CLOUD_PLATFORM,
        ResourceAttributes.CloudPlatformValues.GCP_KUBERNETES_ENGINE);

    Optional.ofNullable(attributesMap.get(GKE_POD_NAME))
        .ifPresent(podName -> attrBuilder.put(ResourceAttributes.K8S_POD_NAME, podName));
    Optional.ofNullable(attributesMap.get(GKE_NAMESPACE))
        .ifPresent(namespace -> attrBuilder.put(ResourceAttributes.K8S_NAMESPACE_NAME, namespace));
    Optional.ofNullable(attributesMap.get(GKE_CONTAINER_NAME))
        .ifPresent(
            containerName -> attrBuilder.put(ResourceAttributes.K8S_CONTAINER_NAME, containerName));
    Optional.ofNullable(attributesMap.get(GKE_CLUSTER_NAME))
        .ifPresent(
            clusterName -> attrBuilder.put(ResourceAttributes.K8S_CLUSTER_NAME, clusterName));
    Optional.ofNullable(attributesMap.get(GKE_HOST_ID))
        .ifPresent(hostId -> attrBuilder.put(ResourceAttributes.HOST_ID, hostId));
    Optional.ofNullable(attributesMap.get(GKE_CLUSTER_LOCATION_TYPE))
        .ifPresent(
            locationType -> {
              if (attributesMap.get(GKE_CLUSTER_LOCATION) != null) {
                switch (locationType) {
                  case GKE_LOCATION_TYPE_REGION:
                    attrBuilder.put(
                        ResourceAttributes.CLOUD_REGION, attributesMap.get(GKE_CLUSTER_LOCATION));
                    break;
                  case GKE_LOCATION_TYPE_ZONE:
                    attrBuilder.put(
                        ResourceAttributes.CLOUD_AVAILABILITY_ZONE,
                        attributesMap.get(GKE_CLUSTER_LOCATION));
                  default:
                    // TODO: Figure out how to handle unexpected conditions like this - Issue #183
                    LOGGER.severe(
                        String.format(
                            "Unrecognized format for cluster location: %s",
                            attributesMap.get(GKE_CLUSTER_LOCATION)));
                }
              }
            });
  }

  /**
   * Updates the attributes with the required keys for a GCR (Google Cloud Run) environment. The
   * attributes are not updated in case the environment is not deemed to be GCR.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private void addGCRAttributes(AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    attrBuilder.put(
        ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.GCP_CLOUD_RUN);
    addCommonAttributesForServerlessCompute(attrBuilder, attributesMap);
  }

  /**
   * Updates the attributes with the required keys for a GCF (Google Cloud Functions) environment.
   * The attributes are not updated in case the environment is not deemed to be GCF.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private void addGCFAttributes(AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    attrBuilder.put(
        ResourceAttributes.CLOUD_PLATFORM,
        ResourceAttributes.CloudPlatformValues.GCP_CLOUD_FUNCTIONS);
    addCommonAttributesForServerlessCompute(attrBuilder, attributesMap);
  }

  /**
   * Updates the attributes with the required keys for a GAE (Google App Engine) environment. The
   * attributes are not updated in case the environment is not deemed to be GAE.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private void addGAEAttributes(AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    attrBuilder.put(
        ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.GCP_APP_ENGINE);
    Optional.ofNullable(attributesMap.get(GAE_MODULE_NAME))
        .ifPresent(appName -> attrBuilder.put(ResourceAttributes.FAAS_NAME, appName));
    Optional.ofNullable(attributesMap.get(GAE_APP_VERSION))
        .ifPresent(appVersion -> attrBuilder.put(ResourceAttributes.FAAS_VERSION, appVersion));
    Optional.ofNullable(attributesMap.get(GAE_INSTANCE_ID))
        .ifPresent(
            appInstanceId -> attrBuilder.put(ResourceAttributes.FAAS_INSTANCE, appInstanceId));
    Optional.ofNullable(attributesMap.get(GAE_CLOUD_REGION))
        .ifPresent(cloudRegion -> attrBuilder.put(ResourceAttributes.CLOUD_REGION, cloudRegion));
    Optional.ofNullable(attributesMap.get(GAE_AVAILABILITY_ZONE))
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
  private void addCommonAttributesForServerlessCompute(
      AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    Optional.ofNullable(attributesMap.get(SERVERLESS_COMPUTE_NAME))
        .ifPresent(name -> attrBuilder.put(ResourceAttributes.FAAS_NAME, name));
    Optional.ofNullable(attributesMap.get(SERVERLESS_COMPUTE_REVISION))
        .ifPresent(revision -> attrBuilder.put(ResourceAttributes.FAAS_VERSION, revision));
    Optional.ofNullable(attributesMap.get(SERVERLESS_COMPUTE_INSTANCE_ID))
        .ifPresent(instanceId -> attrBuilder.put(ResourceAttributes.FAAS_INSTANCE, instanceId));
    Optional.ofNullable(attributesMap.get(SERVERLESS_COMPUTE_AVAILABILITY_ZONE))
        .ifPresent(zone -> attrBuilder.put(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, zone));
    Optional.ofNullable(attributesMap.get(SERVERLESS_COMPUTE_CLOUD_REGION))
        .ifPresent(region -> attrBuilder.put(ResourceAttributes.CLOUD_REGION, region));
  }
}
