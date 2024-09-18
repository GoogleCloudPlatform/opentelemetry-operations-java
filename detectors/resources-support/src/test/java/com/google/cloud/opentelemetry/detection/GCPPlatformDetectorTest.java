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
package com.google.cloud.opentelemetry.detection;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@WireMockTest(httpPort = 8089)
public class GCPPlatformDetectorTest {
  private final GCPMetadataConfig mockMetadataConfig =
      new GCPMetadataConfig("http://localhost:8089/");
  private static final Map<String, String> envVars = new HashMap<>();

  @BeforeEach
  public void setup() {
    envVars.clear();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {""})
  public void testGCPComputeResourceNotGCP(String projectId) {
    GCPMetadataConfig mockMetadataConfig = Mockito.mock(GCPMetadataConfig.class);
    Mockito.when(mockMetadataConfig.getProjectId()).thenReturn(projectId);

    GCPPlatformDetector detector =
        new GCPPlatformDetector(mockMetadataConfig, EnvironmentVariables.DEFAULT_INSTANCE);
    // If GCPMetadataConfig cannot find ProjectId, then the platform should be unsupported
    assertEquals(
        GCPPlatformDetector.SupportedPlatform.UNKNOWN_PLATFORM,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(Collections.emptyMap(), detector.detectPlatform().getAttributes());
  }

  @Test
  public void testGCPComputeResourceNonGCPEndpoint() {
    // intentionally not providing the required Metadata-Flavor header with the
    // request to mimic non GCP endpoint
    stubFor(
        get(urlEqualTo("/project/project-id"))
            .willReturn(aResponse().withBody("nonGCPEndpointTest")));
    GCPPlatformDetector detector =
        new GCPPlatformDetector(mockMetadataConfig, EnvironmentVariables.DEFAULT_INSTANCE);
    assertEquals(
        GCPPlatformDetector.SupportedPlatform.UNKNOWN_PLATFORM,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(Collections.emptyMap(), detector.detectPlatform().getAttributes());
  }

  /** Google Compute Engine Tests * */
  @Test
  public void testGCEResourceWithGCEAttributesSucceeds() {
    TestUtils.stubEndpoint("/project/project-id", "GCE-pid");
    TestUtils.stubEndpoint("/instance/zone", "country-gce_region-gce_zone");
    TestUtils.stubEndpoint("/instance/id", "GCE-instance-id");
    TestUtils.stubEndpoint("/instance/name", "GCE-instance-name");
    TestUtils.stubEndpoint("/instance/machine-type", "GCE-instance-type");
    TestUtils.stubEndpoint("/instance/hostname", "GCE-instance-hostname");

    GCPPlatformDetector detector =
        new GCPPlatformDetector(mockMetadataConfig, new EnvVarMock(envVars));

    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_COMPUTE_ENGINE,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals("GCE-pid", detector.detectPlatform().getProjectId());
    Map<String, String> detectedAttributes = detector.detectPlatform().getAttributes();
    assertEquals(new GoogleComputeEngine(mockMetadataConfig).getAttributes(), detectedAttributes);
    assertEquals(6, detectedAttributes.size());

    assertEquals("country-gce_region-gce_zone", detectedAttributes.get(GCE_AVAILABILITY_ZONE));
    assertEquals("country-gce_region", detectedAttributes.get(GCE_CLOUD_REGION));
    assertEquals("GCE-instance-id", detectedAttributes.get(GCE_INSTANCE_ID));
    assertEquals("GCE-instance-name", detectedAttributes.get(GCE_INSTANCE_NAME));
    assertEquals("GCE-instance-type", detectedAttributes.get(GCE_MACHINE_TYPE));
    assertEquals("GCE-instance-hostname", detectedAttributes.get(GCE_INSTANCE_HOSTNAME));
  }

  /** Google Kubernetes Engine Tests * */
  @Test
  public void testGKEResourceWithGKEAttributesSucceedsLocationZone() {
    envVars.put("KUBERNETES_SERVICE_HOST", "GKE-testHost");
    envVars.put("NAMESPACE", "GKE-testNameSpace");
    // Hostname can truncate pod name, so we test downward API override.
    envVars.put("HOSTNAME", "GKE-testHostName");
    envVars.put("POD_NAME", "GKE-testHostName-full-1234");
    envVars.put("CONTAINER_NAME", "GKE-testContainerName");

    TestUtils.stubEndpoint("/project/project-id", "GKE-pid");
    TestUtils.stubEndpoint("/instance/id", "GKE-instance-id");
    TestUtils.stubEndpoint("/instance/name", "instance-name");
    TestUtils.stubEndpoint("/instance/machine-type", "instance-type");
    TestUtils.stubEndpoint("/instance/attributes/cluster-name", "GKE-cluster-name");
    TestUtils.stubEndpoint("/instance/attributes/cluster-location", "country-region-zone");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    Map<String, String> detectedAttributes = detector.detectPlatform().getAttributes();
    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleKubernetesEngine(mockMetadataConfig).getAttributes(), detectedAttributes);
    assertEquals("GKE-pid", detector.detectPlatform().getProjectId());
    assertEquals(4, detectedAttributes.size());

    assertEquals(GKE_LOCATION_TYPE_ZONE, detectedAttributes.get(GKE_CLUSTER_LOCATION_TYPE));
    assertEquals("country-region-zone", detectedAttributes.get(GKE_CLUSTER_LOCATION));
    assertEquals("GKE-cluster-name", detectedAttributes.get(GKE_CLUSTER_NAME));
    assertEquals("GKE-instance-id", detectedAttributes.get(GKE_HOST_ID));
  }

  @Test
  public void testGKEResourceWithGKEAttributesSucceedsLocationRegion() {
    envVars.put("KUBERNETES_SERVICE_HOST", "GKE-testHost");
    envVars.put("NAMESPACE", "GKE-testNameSpace");
    // Hostname can truncate pod name, so we test downward API override.
    envVars.put("HOSTNAME", "GKE-testHostName");
    envVars.put("POD_NAME", "GKE-testHostName-full-1234");
    envVars.put("CONTAINER_NAME", "GKE-testContainerName");

    TestUtils.stubEndpoint("/project/project-id", "GKE-pid");
    TestUtils.stubEndpoint("/instance/id", "GKE-instance-id");
    TestUtils.stubEndpoint("/instance/name", "GCE-instance-name");
    TestUtils.stubEndpoint("/instance/machine-type", "GKE-instance-type");
    TestUtils.stubEndpoint("/instance/attributes/cluster-name", "GKE-cluster-name");
    TestUtils.stubEndpoint("/instance/attributes/cluster-location", "country-region");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    Map<String, String> detectedAttributes = detector.detectPlatform().getAttributes();
    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleKubernetesEngine(mockMetadataConfig).getAttributes(), detectedAttributes);
    assertEquals("GKE-pid", detector.detectPlatform().getProjectId());
    assertEquals(4, detectedAttributes.size());

    assertEquals(GKE_LOCATION_TYPE_REGION, detectedAttributes.get(GKE_CLUSTER_LOCATION_TYPE));
    assertEquals("country-region", detectedAttributes.get(GKE_CLUSTER_LOCATION));
    assertEquals("GKE-cluster-name", detectedAttributes.get(GKE_CLUSTER_NAME));
    assertEquals("GKE-instance-id", detectedAttributes.get(GKE_HOST_ID));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "country", "country-region-zone-invalid"})
  public void testGKEResourceDetectionWithInvalidLocations(String clusterLocation) {
    envVars.put("KUBERNETES_SERVICE_HOST", "GKE-testHost");
    envVars.put("NAMESPACE", "GKE-testNameSpace");
    // Hostname can truncate pod name, so we test downward API override.
    envVars.put("HOSTNAME", "GKE-testHostName");
    envVars.put("POD_NAME", "GKE-testHostName-full-1234");
    envVars.put("CONTAINER_NAME", "GKE-testContainerName");

    TestUtils.stubEndpoint("/project/project-id", "GKE-pid");
    TestUtils.stubEndpoint("/instance/id", "GKE-instance-id");
    TestUtils.stubEndpoint("/instance/name", "GKE-instance-name");
    TestUtils.stubEndpoint("/instance/machine-type", "GKE-instance-type");
    TestUtils.stubEndpoint("/instance/attributes/cluster-name", "GKE-cluster-name");
    TestUtils.stubEndpoint("/instance/attributes/cluster-location", clusterLocation);

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    Map<String, String> detectedAttributes = detector.detectPlatform().getAttributes();
    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleKubernetesEngine(mockMetadataConfig).getAttributes(), detectedAttributes);
    assertEquals("GKE-pid", detector.detectPlatform().getProjectId());
    assertEquals(4, detectedAttributes.size());

    assertEquals("", detector.detectPlatform().getAttributes().get(GKE_CLUSTER_LOCATION_TYPE));
    if (clusterLocation == null || clusterLocation.isEmpty()) {
      assertNull(detectedAttributes.get(GKE_CLUSTER_LOCATION));
    } else {
      assertEquals(clusterLocation, detectedAttributes.get(GKE_CLUSTER_LOCATION));
    }
    assertEquals("GKE-cluster-name", detectedAttributes.get(GKE_CLUSTER_NAME));
    assertEquals("GKE-instance-id", detectedAttributes.get(GKE_HOST_ID));
  }

  /** Google Cloud Functions Tests * */
  @Test
  public void testGCFResourceWithCloudFunctionAttributesSucceeds() {
    // Setup GCF required env vars
    envVars.put("K_SERVICE", "cloud-function-hello");
    envVars.put("K_REVISION", "cloud-function-hello.1");
    envVars.put("FUNCTION_TARGET", "cloud-function-hello");

    TestUtils.stubEndpoint("/project/project-id", "GCF-pid");
    TestUtils.stubEndpoint("/instance/zone", "country-region-zone");
    TestUtils.stubEndpoint("/instance/id", "GCF-instance-id");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    Map<String, String> detectedAttributes = detector.detectPlatform().getAttributes();
    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleCloudFunction(mockEnv, mockMetadataConfig).getAttributes(), detectedAttributes);
    assertEquals("GCF-pid", detector.detectPlatform().getProjectId());
    assertEquals(5, detectedAttributes.size());

    assertEquals("cloud-function-hello", detectedAttributes.get(SERVERLESS_COMPUTE_NAME));
    assertEquals("cloud-function-hello.1", detectedAttributes.get(SERVERLESS_COMPUTE_REVISION));
    assertEquals(
        "country-region-zone", detectedAttributes.get(SERVERLESS_COMPUTE_AVAILABILITY_ZONE));
    assertEquals("country-region", detectedAttributes.get(SERVERLESS_COMPUTE_CLOUD_REGION));
    assertEquals("GCF-instance-id", detectedAttributes.get(SERVERLESS_COMPUTE_INSTANCE_ID));
  }

  @Test
  public void testGCFDetectionWhenGCRAttributesPresent() {
    // Setup GCF required env vars
    envVars.put("K_SERVICE", "cloud-function-hello");
    envVars.put("K_REVISION", "cloud-function-hello.1");
    envVars.put("FUNCTION_TARGET", "cloud-function-hello");
    // This should be ignored and detected platform should still be GCF
    envVars.put("K_CONFIGURATION", "cloud-run-hello");

    TestUtils.stubEndpoint("/project/project-id", "GCF-pid");
    TestUtils.stubEndpoint("/instance/zone", "country-region-zone");
    TestUtils.stubEndpoint("/instance/id", "GCF-instance-id");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals("GCF-pid", detector.detectPlatform().getProjectId());
    assertEquals(
        new GoogleCloudFunction(mockEnv, mockMetadataConfig).getAttributes(),
        detector.detectPlatform().getAttributes());
  }

  /** Google Cloud Run Tests (Service) * */
  @Test
  public void testGCFResourceWithCloudRunAttributesSucceeds() {
    // Setup GCR service required env vars
    envVars.put("K_SERVICE", "cloud-run-hello");
    envVars.put("K_REVISION", "cloud-run-hello.1");
    envVars.put("K_CONFIGURATION", "cloud-run-hello");

    TestUtils.stubEndpoint("/project/project-id", "GCR-pid");
    TestUtils.stubEndpoint("/instance/zone", "country-region-zone");
    TestUtils.stubEndpoint("/instance/id", "GCR-instance-id");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    Map<String, String> detectedAttributes = detector.detectPlatform().getAttributes();
    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleCloudFunction(mockEnv, mockMetadataConfig).getAttributes(), detectedAttributes);
    assertEquals("GCR-pid", detector.detectPlatform().getProjectId());
    assertEquals(5, detectedAttributes.size());

    assertEquals("cloud-run-hello", detectedAttributes.get(SERVERLESS_COMPUTE_NAME));
    assertEquals("cloud-run-hello.1", detectedAttributes.get(SERVERLESS_COMPUTE_REVISION));
    assertEquals(
        "country-region-zone", detectedAttributes.get(SERVERLESS_COMPUTE_AVAILABILITY_ZONE));
    assertEquals("country-region", detectedAttributes.get(SERVERLESS_COMPUTE_CLOUD_REGION));
    assertEquals("GCR-instance-id", detectedAttributes.get(SERVERLESS_COMPUTE_INSTANCE_ID));
  }

  /** Google Cloud Run Tests (Jobs) * */
  @Test
  public void testCloudRunJobResourceWithAttributesSucceeds() {
    // Setup GCR Job required env vars
    envVars.put("CLOUD_RUN_JOB", "cloud-run-hello-job");
    envVars.put("CLOUD_RUN_EXECUTION", "cloud-run-hello-job-1a2b3c");
    envVars.put("CLOUD_RUN_TASK_INDEX", "0");

    TestUtils.stubEndpoint("/project/project-id", "GCR-pid");
    TestUtils.stubEndpoint("/instance/zone", "country-region-zone");
    TestUtils.stubEndpoint("/instance/id", "GCR-job-instance-id");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    Map<String, String> detectedAttributes = detector.detectPlatform().getAttributes();
    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN_JOB,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleCloudRunJob(mockEnv, mockMetadataConfig).getAttributes(), detectedAttributes);
    assertEquals("GCR-pid", detector.detectPlatform().getProjectId());
    assertEquals(5, detectedAttributes.size());

    assertEquals("cloud-run-hello-job-1a2b3c", detectedAttributes.get(GCR_JOB_EXECUTION_KEY));
    assertEquals("0", detectedAttributes.get(GCR_JOB_TASK_INDEX));
    assertEquals("cloud-run-hello-job", detectedAttributes.get(SERVERLESS_COMPUTE_NAME));
    assertEquals("country-region", detectedAttributes.get(SERVERLESS_COMPUTE_CLOUD_REGION));
    assertEquals("GCR-job-instance-id", detectedAttributes.get(SERVERLESS_COMPUTE_INSTANCE_ID));
  }

  /** Google App Engine Tests * */
  @ParameterizedTest
  @MethodSource("provideGAEVariantEnvironmentVariable")
  public void testGAEResourceWithAppEngineAttributesSucceeds(
      String gaeEnvironmentVar,
      String metadataZone,
      String expectedZone,
      String metadataRegion,
      String expectedRegion) {
    envVars.put("GAE_SERVICE", "app-engine-hello");
    envVars.put("GAE_VERSION", "app-engine-hello-v1");
    envVars.put("GAE_INSTANCE", "app-engine-hello-f236d");
    envVars.put("GAE_ENV", gaeEnvironmentVar);

    TestUtils.stubEndpoint("/project/project-id", "GAE-pid");
    TestUtils.stubEndpoint("/instance/zone", metadataZone);
    TestUtils.stubEndpoint("/instance/region", metadataRegion);
    TestUtils.stubEndpoint("/instance/id", "GAE-instance-id");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    Map<String, String> detectedAttributes = detector.detectPlatform().getAttributes();
    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_APP_ENGINE,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleAppEngine(mockEnv, mockMetadataConfig).getAttributes(), detectedAttributes);
    assertEquals("GAE-pid", detector.detectPlatform().getProjectId());
    assertEquals(5, detectedAttributes.size());
    assertEquals(expectedRegion, detector.detectPlatform().getAttributes().get(GAE_CLOUD_REGION));
    assertEquals("app-engine-hello", detectedAttributes.get(GAE_MODULE_NAME));
    assertEquals("app-engine-hello-v1", detectedAttributes.get(GAE_APP_VERSION));
    assertEquals("app-engine-hello-f236d", detectedAttributes.get(GAE_INSTANCE_ID));
    assertEquals(expectedZone, detectedAttributes.get(GAE_AVAILABILITY_ZONE));
  }

  private static Arguments gaeTestArguments(
      String gaeEnvironmentVar,
      String metadataZone,
      String expectedZone,
      String metadataRegion,
      String expectedRegion) {
    return Arguments.of(
        gaeEnvironmentVar, metadataZone, expectedZone, metadataRegion, expectedRegion);
  }

  // Provides parameterized arguments for testGAEResourceWithAppEngineAttributesSucceeds
  private static Stream<Arguments> provideGAEVariantEnvironmentVariable() {

    return Stream.of(
        // GAE standard
        gaeTestArguments(
            "standard",
            // the zone should be extracted from the zone attribute
            "projects/233510669999/zones/us15",
            "us15",
            // the region should be extracted from region attribute
            "projects/233510669999/regions/us-central1",
            "us-central1"),

        // GAE flex
        gaeTestArguments(
            (String) null,
            "country-region-zone",
            "country-region-zone",
            // the region should be extracted from the zone attribute
            "",
            "country-region"),
        gaeTestArguments(
            "flex", "country-region-zone", "country-region-zone", "", "country-region"),
        gaeTestArguments("", "country-region-zone", "country-region-zone", "", "country-region"));
  }
}
