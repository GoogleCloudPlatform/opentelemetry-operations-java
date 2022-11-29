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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GCPResourceTest {
  @Rule public final WireMockRule wireMockRule = new WireMockRule(8089);
  private final GCPMetadataConfig metadataConfig = new GCPMetadataConfig("http://localhost:8089/");
  private static final Map<String, String> envVars = new HashMap<>();

  @Before
  public void clearEnvVars() {
    envVars.clear();
  }

  @Test
  public void findsWithServiceLoader() {
    ServiceLoader<ResourceProvider> services =
        ServiceLoader.load(ResourceProvider.class, getClass().getClassLoader());
    assertTrue(
        "Could not load GCP Resource detector using serviceloader, found: " + services,
        services.stream().anyMatch(provider -> provider.type().equals(GCPResource.class)));
  }

  @Test
  public void testGCPComputeResourceNotGCP() {
    GCPResource testResource = new GCPResource();

    // The default metadata url is unreachable through testing so getAttributes should not detect a
    // GCP environment, hence returning empty attributes.
    assertThat(testResource.getAttributes()).isEmpty();
  }

  @Test
  public void testGCPComputeResourceNonGCPEndpoint() {
    // intentionally not providing the required Metadata-Flovor header with the
    // request to mimic non GCP endpoint
    stubFor(
        get(urlEqualTo("/project/project-id"))
            .willReturn(aResponse().withBody("nonGCPendpointTest")));
    GCPResource testResource = new GCPResource(metadataConfig, new EnvVarMock(envVars));
    assertThat(testResource.getAttributes()).isEmpty();
  }

  /** Google Compute Engine Tests * */
  @Test
  public void testGCEResourceWithGCEAttributesSucceeds() {
    stubEndpoint("/project/project-id", "GCE-pid");
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/id", "GCE-instance-id");
    stubEndpoint("/instance/name", "GCE-instance-name");
    stubEndpoint("/instance/machine-type", "GCE-instance-type");

    final GCPResource testResource = new GCPResource(metadataConfig, new EnvVarMock(envVars));
    assertThat(testResource.getAttributes())
        .hasSize(8)
        .containsEntry(
            ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP)
        .containsEntry(
            ResourceAttributes.CLOUD_PLATFORM,
            ResourceAttributes.CloudPlatformValues.GCP_COMPUTE_ENGINE)
        .containsEntry(ResourceAttributes.CLOUD_ACCOUNT_ID, "GCE-pid")
        .containsEntry(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone")
        .containsEntry(ResourceAttributes.CLOUD_REGION, "country-region")
        .containsEntry(ResourceAttributes.HOST_ID, "GCE-instance-id")
        .containsEntry(ResourceAttributes.HOST_NAME, "GCE-instance-name")
        .containsEntry(ResourceAttributes.HOST_TYPE, "GCE-instance-type");
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

    GCPResource testResource = new GCPResource(metadataConfig, new EnvVarMock(envVars));
    assertThat(testResource.getAttributes())
        .hasSize(8)
        .containsEntry(ResourceAttributes.CLOUD_PROVIDER, "gcp")
        .containsEntry(ResourceAttributes.CLOUD_PLATFORM, "gcp_kubernetes_engine")
        .containsEntry(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone")
        .containsEntry(ResourceAttributes.HOST_ID, "GCE-instance-id")
        .containsEntry(ResourceAttributes.K8S_CLUSTER_NAME, "GKE-cluster-name")
        .containsEntry(ResourceAttributes.K8S_NAMESPACE_NAME, "GKE-testNameSpace")
        .containsEntry(ResourceAttributes.K8S_POD_NAME, "GKE-testHostName-full-1234")
        .containsEntry(ResourceAttributes.K8S_CONTAINER_NAME, "GKE-testContainerName");
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

    GCPResource testResource = new GCPResource(metadataConfig, new EnvVarMock(envVars));
    assertThat(testResource.getAttributes())
        .hasSize(8)
        .containsEntry(ResourceAttributes.CLOUD_PROVIDER, "gcp")
        .containsEntry(ResourceAttributes.CLOUD_PLATFORM, "gcp_kubernetes_engine")
        .containsEntry(ResourceAttributes.CLOUD_REGION, "country-region")
        .containsEntry(ResourceAttributes.HOST_ID, "GCE-instance-id")
        .containsEntry(ResourceAttributes.K8S_CLUSTER_NAME, "GKE-cluster-name")
        .containsEntry(ResourceAttributes.K8S_NAMESPACE_NAME, "GKE-testNameSpace")
        .containsEntry(ResourceAttributes.K8S_POD_NAME, "GKE-testHostName-full-1234")
        .containsEntry(ResourceAttributes.K8S_CONTAINER_NAME, "GKE-testContainerName");
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

    GCPResource testResource = new GCPResource(metadataConfig, new EnvVarMock(envVars));
    assertThat(testResource.getAttributes())
        .hasSize(7)
        .containsEntry(
            ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP)
        .containsEntry(
            ResourceAttributes.CLOUD_PLATFORM,
            ResourceAttributes.CloudPlatformValues.GCP_CLOUD_FUNCTIONS)
        .containsEntry(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone")
        .containsEntry(ResourceAttributes.CLOUD_REGION, "country-region")
        .containsEntry(ResourceAttributes.FAAS_NAME, envVars.get("K_SERVICE"))
        .containsEntry(ResourceAttributes.FAAS_VERSION, envVars.get("K_REVISION"))
        .containsEntry(ResourceAttributes.FAAS_ID, "GCF-instance-id");
  }

  /** Google App Engine Tests * */
  @Test
  public void testGAEResourceWithAppEngineAttributesSucceedsInFlex() {
    envVars.put("GAE_SERVICE", "app-engine-hello");
    envVars.put("GAE_VERSION", "app-engine-hello-v1");
    envVars.put("GAE_INSTANCE", "app-engine-hello-f236d");

    stubEndpoint("/project/project-id", "GAE-pid-flex");
    // for flex, the region should be parsed from zone attribute
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/region", "country-region1");
    stubEndpoint("/instance/id", "GAE-instance-id");

    GCPResource testResource = new GCPResource(metadataConfig, new EnvVarMock(envVars));
    assertThat(testResource.getAttributes())
        .hasSize(6)
        .containsEntry(
            ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP)
        .containsEntry(
            ResourceAttributes.CLOUD_PLATFORM,
            ResourceAttributes.CloudPlatformValues.GCP_APP_ENGINE)
        .containsEntry(ResourceAttributes.CLOUD_REGION, "country-region")
        .containsEntry(ResourceAttributes.FAAS_NAME, envVars.get("GAE_SERVICE"))
        .containsEntry(ResourceAttributes.FAAS_VERSION, envVars.get("GAE_VERSION"))
        .containsEntry(ResourceAttributes.FAAS_ID, envVars.get("GAE_INSTANCE"));
  }

  @Test
  public void testGAEResourceWithAppEngineAttributesSucceedsInStandard() {
    envVars.put("GAE_SERVICE", "app-engine-hello");
    envVars.put("GAE_VERSION", "app-engine-hello-v1");
    envVars.put("GAE_INSTANCE", "app-engine-hello-f236d");

    stubEndpoint("/project/project-id", "GAE-pid-standard");
    // for standard, the region should be extracted from region attribute
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/region", "country-region1");
    stubEndpoint("/instance/id", "GAE-instance-id");

    Map<String, String> updatedEnvVars = new HashMap<>(envVars);
    updatedEnvVars.put("GAE_ENV", "standard");
    GCPResource testResource = new GCPResource(metadataConfig, new EnvVarMock(updatedEnvVars));
    assertThat(testResource.getAttributes())
        .hasSize(6)
        .containsEntry(
            ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP)
        .containsEntry(
            ResourceAttributes.CLOUD_PLATFORM,
            ResourceAttributes.CloudPlatformValues.GCP_APP_ENGINE)
        .containsEntry(ResourceAttributes.CLOUD_REGION, "country-region1")
        .containsEntry(ResourceAttributes.FAAS_NAME, envVars.get("GAE_SERVICE"))
        .containsEntry(ResourceAttributes.FAAS_VERSION, envVars.get("GAE_VERSION"))
        .containsEntry(ResourceAttributes.FAAS_ID, envVars.get("GAE_INSTANCE"));
  }

  // Helper method to help stub endpoints
  private void stubEndpoint(String endpointPath, String responseBody) {
    stubFor(
        get(urlEqualTo(endpointPath))
            .willReturn(
                aResponse().withHeader("Metadata-Flavor", "Google").withBody(responseBody)));
  }
}
