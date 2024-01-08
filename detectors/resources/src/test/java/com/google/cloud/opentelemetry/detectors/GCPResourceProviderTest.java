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
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class GCPResourceProviderTest {

  private final ConfigProperties mockConfigProps = Mockito.mock(ConfigProperties.class);
  private final Map<String, String> mockGKECommonAttributes =
      new HashMap<>() {
        {
          put(GKE_POD_NAME, "gke-pod-123");
          put(GKE_NAMESPACE, "gke-namespace-default");
          put(GKE_CONTAINER_NAME, "gke-container-2");
          put(GKE_CLUSTER_NAME, "gke-cluster");
          put(GKE_HOST_ID, "host1");
        }
      };

  // Mock Platforms
  private DetectedPlatform generateMockGCEPlatform() {
    Map<String, String> mockAttributes =
        new HashMap<>() {
          {
            put(GCE_PROJECT_ID, "test-project-id");
            put(GCE_CLOUD_REGION, "australia-southeast1");
            put(GCE_AVAILABILITY_ZONE, "australia-southeast1-b");
            put(GCE_INSTANCE_ID, "random-id");
            put(GCE_INSTANCE_NAME, "instance-name");
            put(GCE_MACHINE_TYPE, "gce-m2");
          }
        };
    DetectedPlatform mockGCEPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockGCEPlatform.getSupportedPlatform())
        .thenReturn(GCPPlatformDetector.SupportedPlatform.GOOGLE_COMPUTE_ENGINE);
    Mockito.when(mockGCEPlatform.getAttributes()).thenReturn(mockAttributes);
    return mockGCEPlatform;
  }

  private DetectedPlatform generateMockGKEPlatform(String gkeClusterLocationType) {
    Map<String, String> mockAttributes = new HashMap<>(mockGKECommonAttributes);
    if (gkeClusterLocationType.equals(GKE_LOCATION_TYPE_ZONE)) {
      mockAttributes.put(GKE_CLUSTER_LOCATION, "australia-southeast1-a");
    } else if (gkeClusterLocationType.equals(GKE_LOCATION_TYPE_REGION)) {
      mockAttributes.put(GKE_CLUSTER_LOCATION, "australia-southeast1");
    }
    mockAttributes.put(GKE_CLUSTER_LOCATION_TYPE, gkeClusterLocationType);

    DetectedPlatform mockGKEPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockGKEPlatform.getSupportedPlatform())
        .thenReturn(GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE);
    Mockito.when(mockGKEPlatform.getAttributes()).thenReturn(mockAttributes);
    return mockGKEPlatform;
  }

  private DetectedPlatform generateMockServerlessPlatform(
      GCPPlatformDetector.SupportedPlatform platform) {
    final EnumSet<GCPPlatformDetector.SupportedPlatform> serverlessPlatforms =
        EnumSet.of(
            GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN,
            GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS);
    if (!serverlessPlatforms.contains(platform)) {
      throw new IllegalArgumentException();
    }
    Map<String, String> mockAttributes =
        new HashMap<>() {
          {
            put(SERVERLESS_COMPUTE_NAME, "serverless-app");
            put(SERVERLESS_COMPUTE_REVISION, "v2");
            put(SERVERLESS_COMPUTE_INSTANCE_ID, "serverless-instance-id");
            put(SERVERLESS_COMPUTE_CLOUD_REGION, "us-central1");
            put(SERVERLESS_COMPUTE_AVAILABILITY_ZONE, "us-central1-b");
          }
        };
    DetectedPlatform mockServerlessPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockServerlessPlatform.getSupportedPlatform()).thenReturn(platform);
    Mockito.when(mockServerlessPlatform.getAttributes()).thenReturn(mockAttributes);
    return mockServerlessPlatform;
  }

  private DetectedPlatform generateMockGAEPlatform() {
    Map<String, String> mockAttributes =
        new HashMap<>() {
          {
            put(GAE_MODULE_NAME, "gae-app");
            put(GAE_APP_VERSION, "v1");
            put(GAE_INSTANCE_ID, "gae-instance-id");
            put(GAE_CLOUD_REGION, "us-central1");
            put(GAE_AVAILABILITY_ZONE, "us-central1-b");
          }
        };
    DetectedPlatform mockGAEPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockGAEPlatform.getSupportedPlatform())
        .thenReturn(GCPPlatformDetector.SupportedPlatform.GOOGLE_APP_ENGINE);
    Mockito.when(mockGAEPlatform.getAttributes()).thenReturn(mockAttributes);
    return mockGAEPlatform;
  }

  private DetectedPlatform generateMockUnknownPlatform() {
    Map<String, String> mockAttributes =
        new HashMap<>() {
          {
            put(GCE_INSTANCE_ID, "instance-id");
            put(GCE_CLOUD_REGION, "australia-southeast1");
          }
        };

    DetectedPlatform mockUnknownPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockUnknownPlatform.getSupportedPlatform())
        .thenReturn(GCPPlatformDetector.SupportedPlatform.UNKNOWN_PLATFORM);
    Mockito.when(mockUnknownPlatform.getAttributes()).thenReturn(mockAttributes);
    return mockUnknownPlatform;
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
    assertEquals(
        mockPlatform.getAttributes().get(GCE_AVAILABILITY_ZONE),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_CLOUD_REGION),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
    assertThat(gotResource.getAttributes()).hasSize(8);
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
    assertThat(gotResource.getAttributes()).hasSize(8);
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
    assertThat(gotResource.getAttributes()).hasSize(8);
  }

  @Test
  public void testGKEResourceAttributesMapping_LocationTypeInvalid() {
    Map<String, String> mockGKEAttributes = new HashMap<>(mockGKECommonAttributes);
    mockGKEAttributes.put(GKE_CLUSTER_LOCATION_TYPE, "INVALID");
    mockGKEAttributes.put(GKE_CLUSTER_LOCATION, "some-location");

    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockPlatform.getSupportedPlatform())
        .thenReturn(GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE);
    Mockito.when(mockPlatform.getAttributes()).thenReturn(mockGKEAttributes);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);

    verifyGKEMapping(gotResource, mockPlatform);
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertThat(gotResource.getAttributes()).hasSize(7);
  }

  @Test
  public void testGKEResourceAttributesMapping_LocationMissing() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGKEPlatform("");
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);

    verifyGKEMapping(gotResource, mockPlatform);
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertThat(gotResource.getAttributes()).hasSize(7);
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
    assertEquals(
        detectedPlatform.getAttributes().get(GKE_HOST_ID),
        gotResource.getAttributes().get(ResourceAttributes.HOST_ID));
    assertEquals(
        detectedPlatform.getAttributes().get(GKE_CLUSTER_NAME),
        gotResource.getAttributes().get(ResourceAttributes.K8S_CLUSTER_NAME));
  }

  @Test
  public void testGCRResourceAttributesMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform =
        generateMockServerlessPlatform(GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    assertEquals(
        ResourceAttributes.CloudPlatformValues.GCP_CLOUD_RUN,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM));
    verifyServerlessMapping(gotResource, mockPlatform);
    assertThat(gotResource.getAttributes()).hasSize(7);
  }

  @Test
  public void testGCFResourceAttributeMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform =
        generateMockServerlessPlatform(
            GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    assertEquals(
        ResourceAttributes.CloudPlatformValues.GCP_CLOUD_FUNCTIONS,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM));
    verifyServerlessMapping(gotResource, mockPlatform);
    assertThat(gotResource.getAttributes()).hasSize(7);
  }

  private void verifyServerlessMapping(Resource gotResource, DetectedPlatform detectedPlatform) {
    assertEquals(
        ResourceAttributes.CloudProviderValues.GCP,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PROVIDER));
    assertEquals(
        detectedPlatform.getAttributes().get(SERVERLESS_COMPUTE_NAME),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_NAME));
    assertEquals(
        detectedPlatform.getAttributes().get(SERVERLESS_COMPUTE_REVISION),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_VERSION));
    assertEquals(
        detectedPlatform.getAttributes().get(SERVERLESS_COMPUTE_INSTANCE_ID),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_INSTANCE));
    assertEquals(
        detectedPlatform.getAttributes().get(SERVERLESS_COMPUTE_AVAILABILITY_ZONE),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(
        detectedPlatform.getAttributes().get(SERVERLESS_COMPUTE_CLOUD_REGION),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
  }

  @Test
  public void testGAEResourceAttributeMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGAEPlatform();
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    assertEquals(
        ResourceAttributes.CloudPlatformValues.GCP_APP_ENGINE,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM));
    assertEquals(
        ResourceAttributes.CloudProviderValues.GCP,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PROVIDER));
    assertEquals(
        mockPlatform.getAttributes().get(GAE_MODULE_NAME),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_NAME));
    assertEquals(
        mockPlatform.getAttributes().get(GAE_APP_VERSION),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_VERSION));
    assertEquals(
        mockPlatform.getAttributes().get(GAE_INSTANCE_ID),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_INSTANCE));
    assertEquals(
        mockPlatform.getAttributes().get(GAE_AVAILABILITY_ZONE),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(
        mockPlatform.getAttributes().get(GAE_CLOUD_REGION),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
    assertThat(gotResource.getAttributes()).hasSize(7);
  }

  @Test
  public void testUnknownPlatformResourceAttributesMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockUnknownPlatform();
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    assertTrue("no attributes for unknown platform", gotResource.getAttributes().isEmpty());
  }

  @Test
  public void findsWithServiceLoader() {
    ServiceLoader<ResourceProvider> services =
        ServiceLoader.load(ResourceProvider.class, getClass().getClassLoader());
    assertTrue(
        "Could not load GCP Resource detector using serviceloader, found: " + services,
        services.stream().anyMatch(provider -> provider.type().equals(GCPResourceProvider.class)));
  }
}
