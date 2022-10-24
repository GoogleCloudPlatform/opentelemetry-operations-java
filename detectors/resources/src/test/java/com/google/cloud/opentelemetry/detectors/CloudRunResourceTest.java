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
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CloudRunResourceTest {
  @Rule public final WireMockRule wireMockRule = new WireMockRule(8089);
  private final GCPMetadataConfig metadataConfig = new GCPMetadataConfig("http://localhost:8089/");
  private static final Map<String, String> envVars = new HashMap<>();

  @BeforeClass
  public static void setup() {
    envVars.put("K_SERVICE", "cloud-run-hello");
    envVars.put("K_REVISION", "cloud-run-hello.1");
    envVars.put("K_CONFIGURATION", "cloud-run-hello");
  }

  @Test
  public void findsWithServiceLoader() {
    ServiceLoader<ResourceProvider> services =
        ServiceLoader.load(ResourceProvider.class, getClass().getClassLoader());
    assertTrue(
        "Could not load CloudRun Resource detector using serviceloader, found: " + services,
        services.stream().anyMatch(provider -> provider.type().equals(CloudRunResource.class)));
  }

  @Test
  public void testCloudRunNotGCP() {
    CloudRunResource testResource = new CloudRunResource();

    // The default metadata url is unreachable through testing so getAttributes should not detect a
    // GCP environment, hence returning empty attributes.
    // Note: Currently this does not support GKE outside of Google Cloud (e.g. GKE on AWS, GKE on
    // premise)
    assertThat(testResource.getAttributes()).isEmpty();
  }

  @Test
  public void testCloudRunResourceNonGCPEndpoint() {
    // intentionally not providing the required Metadata-Flovor header with the
    // request to mimic non GCP endpoint
    stubFor(
        get(urlEqualTo("/project/project-id"))
            .willReturn(aResponse().withBody("nonGCPendpointTest")));
    CloudRunResource testResource = new CloudRunResource(metadataConfig, new EnvVarMock(envVars));
    assertThat(testResource.getAttributes()).isEmpty();
  }

  @Test
  public void testCloudRunResourceWithCloudRunAttributesSucceeds() {
    stubEndpoint("/project/project-id", "GCE-pid");
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/id", "GCE-instance-id");

    CloudRunResource testResource = new CloudRunResource(metadataConfig, new EnvVarMock(envVars));
    assertThat(testResource.getAttributes())
        .hasSize(7)
        .containsEntry(
            ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP)
        .containsEntry(
            ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.GCP_CLOUD_RUN)
        .containsEntry(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone")
        .containsEntry(ResourceAttributes.CLOUD_REGION, "country-region")
        .containsEntry(ResourceAttributes.FAAS_NAME, envVars.get("K_SERVICE"))
        .containsEntry(ResourceAttributes.FAAS_VERSION, envVars.get("K_REVISION"))
        .containsEntry(ResourceAttributes.FAAS_ID, "GCE-instance-id");
  }

  // Helper method to help stub endpoints
  private void stubEndpoint(String endpointPath, String responseBody) {
    stubFor(
        get(urlEqualTo(endpointPath))
            .willReturn(
                aResponse().withHeader("Metadata-Flavor", "Google").withBody(responseBody)));
  }
}
