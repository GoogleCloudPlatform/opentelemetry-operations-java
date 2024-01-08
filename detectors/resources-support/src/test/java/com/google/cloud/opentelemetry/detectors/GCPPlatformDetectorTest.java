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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GAE_CLOUD_REGION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CLUSTER_LOCATION_TYPE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_LOCATION_TYPE_REGION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_LOCATION_TYPE_ZONE;
import static com.google.cloud.opentelemetry.detectors.TestUtils.stubEndpoint;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

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
    stubEndpoint("/project/project-id", "GCE-pid");
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/id", "GCE-instance-id");
    stubEndpoint("/instance/name", "GCE-instance-name");
    stubEndpoint("/instance/machine-type", "GCE-instance-type");

    GCPPlatformDetector detector =
        new GCPPlatformDetector(mockMetadataConfig, new EnvVarMock(envVars));

    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_COMPUTE_ENGINE,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleComputeEngine(mockMetadataConfig).getAttributes(),
        detector.detectPlatform().getAttributes());
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

    stubEndpoint("/project/project-id", "GCE-pid");
    stubEndpoint("/instance/id", "GCE-instance-id");
    stubEndpoint("/instance/name", "GCE-instance-name");
    stubEndpoint("/instance/machine-type", "GCE-instance-type");
    stubEndpoint("/instance/attributes/cluster-name", "GKE-cluster-name");
    stubEndpoint("/instance/attributes/cluster-location", "country-region-zone");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleKubernetesEngine(mockEnv, mockMetadataConfig).getAttributes(),
        detector.detectPlatform().getAttributes());
    assertEquals(
        GKE_LOCATION_TYPE_ZONE,
        detector.detectPlatform().getAttributes().get(GKE_CLUSTER_LOCATION_TYPE));
  }

  @Test
  public void testGKEResourceWithGKEAttributesSucceedsLocationRegion() {
    envVars.put("KUBERNETES_SERVICE_HOST", "GKE-testHost");
    envVars.put("NAMESPACE", "GKE-testNameSpace");
    // Hostname can truncate pod name, so we test downward API override.
    envVars.put("HOSTNAME", "GKE-testHostName");
    envVars.put("POD_NAME", "GKE-testHostName-full-1234");
    envVars.put("CONTAINER_NAME", "GKE-testContainerName");

    stubEndpoint("/project/project-id", "GCE-pid");
    stubEndpoint("/instance/id", "GCE-instance-id");
    stubEndpoint("/instance/name", "GCE-instance-name");
    stubEndpoint("/instance/machine-type", "GCE-instance-type");
    stubEndpoint("/instance/attributes/cluster-name", "GKE-cluster-name");
    stubEndpoint("/instance/attributes/cluster-location", "country-region");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleKubernetesEngine(mockEnv, mockMetadataConfig).getAttributes(),
        detector.detectPlatform().getAttributes());
    assertEquals(
        GKE_LOCATION_TYPE_REGION,
        detector.detectPlatform().getAttributes().get(GKE_CLUSTER_LOCATION_TYPE));
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

    stubEndpoint("/project/project-id", "GCE-pid");
    stubEndpoint("/instance/id", "GCE-instance-id");
    stubEndpoint("/instance/name", "GCE-instance-name");
    stubEndpoint("/instance/machine-type", "GCE-instance-type");
    stubEndpoint("/instance/attributes/cluster-name", "GKE-cluster-name");
    stubEndpoint("/instance/attributes/cluster-location", clusterLocation);

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleKubernetesEngine(mockEnv, mockMetadataConfig).getAttributes(),
        detector.detectPlatform().getAttributes());
    assertEquals("", detector.detectPlatform().getAttributes().get(GKE_CLUSTER_LOCATION_TYPE));
  }

  /** Google Cloud Functions Tests * */
  @Test
  public void testGCFResourceWithCloudFunctionAttributesSucceeds() {
    // Setup GCF required env vars
    envVars.put("K_SERVICE", "cloud-function-hello");
    envVars.put("K_REVISION", "cloud-function-hello.1");
    envVars.put("FUNCTION_TARGET", "cloud-function-hello");

    stubEndpoint("/project/project-id", "GCF-pid");
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/id", "GCF-instance-id");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleCloudFunction(mockEnv, mockMetadataConfig).getAttributes(),
        detector.detectPlatform().getAttributes());
  }

  @Test
  public void testGCFDetectionWhenGCRAttributesPresent() {
    // Setup GCF required env vars
    envVars.put("K_SERVICE", "cloud-function-hello");
    envVars.put("K_REVISION", "cloud-function-hello.1");
    envVars.put("FUNCTION_TARGET", "cloud-function-hello");
    // This should be ignored and detected platform should still be GCF
    envVars.put("K_CONFIGURATION", "cloud-run-hello");

    stubEndpoint("/project/project-id", "GCF-pid");
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/id", "GCF-instance-id");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleCloudFunction(mockEnv, mockMetadataConfig).getAttributes(),
        detector.detectPlatform().getAttributes());
  }

  /** Google Cloud Run Tests * */
  @Test
  public void testGCFResourceWithCloudRunAttributesSucceeds() {
    // Setup GCR required env vars
    envVars.put("K_SERVICE", "cloud-run-hello");
    envVars.put("K_REVISION", "cloud-run-hello.1");
    envVars.put("K_CONFIGURATION", "cloud-run-hello");

    stubEndpoint("/project/project-id", "GCR-pid");
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/id", "GCR-instance-id");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleCloudFunction(mockEnv, mockMetadataConfig).getAttributes(),
        detector.detectPlatform().getAttributes());
  }

  /** Google App Engine Tests * */
  @ParameterizedTest
  @MethodSource("provideGAEVariantEnvironmentVariable")
  public void testGAEResourceWithAppEngineAttributesSucceeds(
      String gaeEnvironmentVar, String expectedRegion) {
    envVars.put("GAE_SERVICE", "app-engine-hello");
    envVars.put("GAE_VERSION", "app-engine-hello-v1");
    envVars.put("GAE_INSTANCE", "app-engine-hello-f236d");
    envVars.put("GAE_ENV", gaeEnvironmentVar);

    stubEndpoint("/project/project-id", "GAE-pid-standard");
    // for standard, the region should be extracted from region attribute
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/region", "country-region1");
    stubEndpoint("/instance/id", "GAE-instance-id");

    EnvironmentVariables mockEnv = new EnvVarMock(envVars);
    GCPPlatformDetector detector = new GCPPlatformDetector(mockMetadataConfig, mockEnv);

    assertEquals(
        GCPPlatformDetector.SupportedPlatform.GOOGLE_APP_ENGINE,
        detector.detectPlatform().getSupportedPlatform());
    assertEquals(
        new GoogleAppEngine(mockEnv, mockMetadataConfig).getAttributes(),
        detector.detectPlatform().getAttributes());
    assertEquals(expectedRegion, detector.detectPlatform().getAttributes().get(GAE_CLOUD_REGION));
  }

  // Provides key-value pair of GAE variant environment and the expected region
  // value based on the environment variable
  private static Stream<Arguments> provideGAEVariantEnvironmentVariable() {
    return Stream.of(
        Arguments.of("standard", "country-region1"),
        Arguments.of((String) null, "country-region"),
        Arguments.of("flex", "country-region"),
        Arguments.of("", "country-region"));
  }
}
