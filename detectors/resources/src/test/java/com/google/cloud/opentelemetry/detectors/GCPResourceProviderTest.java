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

import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_CLOUD_REGION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_INSTANCE_ID;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_INSTANCE_NAME;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_MACHINE_TYPE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_PROJECT_ID;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CLUSTER_LOCATION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CLUSTER_LOCATION_TYPE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CLUSTER_NAME;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CONTAINER_NAME;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_LOCATION_TYPE_REGION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_LOCATION_TYPE_ZONE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_NAMESPACE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_POD_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class GCPResourceProviderTest {

  private final ConfigProperties mockConfigProps = Mockito.mock(ConfigProperties.class);

  private DetectedPlatform generateMockGCEPlatform() {
    return new DetectedPlatform() {
      @Override
      public GCPPlatformDetector.SupportedPlatform getSupportedPlatform() {
        return GCPPlatformDetector.SupportedPlatform.GOOGLE_COMPUTE_ENGINE;
      }

      @Override
      public Map<String, String> getAttributes() {
        return new HashMap<>() {
          {
            put(GCE_PROJECT_ID, "test-project-id");
            put(GCE_CLOUD_REGION, "australia-southeast1");
            put(GCE_INSTANCE_ID, "random-id");
            put(GCE_INSTANCE_NAME, "instance-name");
            put(GCE_MACHINE_TYPE, "gce-m2");
          }
        };
      }
    };
  }

  private DetectedPlatform generateMockGKEPlatform(String gkeClusterLocationType) {
    String gkeClusterLocation;
    if (gkeClusterLocationType.equals(GKE_LOCATION_TYPE_ZONE)) {
      gkeClusterLocation = "australia-southeast1-a";
    } else {
      gkeClusterLocation = "australia-southeast1";
    }
    return new DetectedPlatform() {
      @Override
      public GCPPlatformDetector.SupportedPlatform getSupportedPlatform() {
        return GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE;
      }

      @Override
      public Map<String, String> getAttributes() {
        return new HashMap<>() {
          {
            put(GKE_POD_NAME, "gke-pod-123");
            put(GKE_NAMESPACE, "gke-namespace-default");
            put(GKE_CONTAINER_NAME, "gke-container-2");
            put(GKE_CLUSTER_NAME, "gke-cluster");
            put(GKE_CLUSTER_LOCATION_TYPE, gkeClusterLocationType);
            put(GKE_CLUSTER_LOCATION, gkeClusterLocation);
          }
        };
      }
    };
  }

  @Test
  public void testGCEResourceAttributesMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGCEPlatform();
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);

    assertEquals(
        ResourceAttributes.CloudPlatformValues.GCP_COMPUTE_ENGINE,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM));
    assertEquals(
        ResourceAttributes.CloudProviderValues.GCP,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PROVIDER));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_PROJECT_ID),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_ACCOUNT_ID));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_INSTANCE_ID),
        gotResource.getAttributes().get(ResourceAttributes.HOST_ID));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_INSTANCE_NAME),
        gotResource.getAttributes().get(ResourceAttributes.HOST_NAME));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_MACHINE_TYPE),
        gotResource.getAttributes().get(ResourceAttributes.HOST_TYPE));
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_CLOUD_REGION),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
  }

  @Test
  public void testGKEResourceAttributesMapping_LocationTypeRegion() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGKEPlatform(GKE_LOCATION_TYPE_REGION);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);

    verifyGKEMapping(gotResource, mockPlatform);
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(
        mockPlatform.getAttributes().get(GKE_CLUSTER_LOCATION),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
  }

  @Test
  public void testGKEResourceAttributesMapping_LocationTypeZone() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGKEPlatform(GKE_LOCATION_TYPE_ZONE);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);

    verifyGKEMapping(gotResource, mockPlatform);
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
    assertEquals(
        mockPlatform.getAttributes().get(GKE_CLUSTER_LOCATION),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
  }

  private void verifyGKEMapping(Resource gotResource, DetectedPlatform detectedPlatform) {
    assertEquals(
        ResourceAttributes.CloudPlatformValues.GCP_KUBERNETES_ENGINE,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM));
    assertEquals(
        ResourceAttributes.CloudProviderValues.GCP,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PROVIDER));
    assertEquals(
        detectedPlatform.getAttributes().get(GKE_POD_NAME),
        gotResource.getAttributes().get(ResourceAttributes.K8S_POD_NAME));
    assertEquals(
        detectedPlatform.getAttributes().get(GKE_NAMESPACE),
        gotResource.getAttributes().get(ResourceAttributes.K8S_NAMESPACE_NAME));
    assertEquals(
        detectedPlatform.getAttributes().get(GKE_CONTAINER_NAME),
        gotResource.getAttributes().get(ResourceAttributes.K8S_CONTAINER_NAME));
  }
}
