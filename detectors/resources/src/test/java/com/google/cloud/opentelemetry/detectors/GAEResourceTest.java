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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.junit.Assert.*;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class GAEResourceTest {
  @Rule public final WireMockRule wireMockRule = new WireMockRule(8089);
  private final GCPMetadataConfig metadataConfig = new GCPMetadataConfig("http://localhost:8089/");
  private static final Map<String, String> envVars = new HashMap<>();

  @BeforeClass
  public static void setup() {
    envVars.put("GAE_SERVICE", "app-engine-hello");
    envVars.put("GAE_VERSION", "app-engine-hello-v1");
    envVars.put("GAE_INSTANCE", "app-engine-hello-f236d");
  }

  @Test
  public void findsWithServiceLoader() {
    ServiceLoader<ResourceProvider> services =
        ServiceLoader.load(ResourceProvider.class, getClass().getClassLoader());
    assertTrue(
        "Could not load AppEngine Resource detector using serviceloader, found: " + services,
        services.stream().anyMatch(provider -> provider.type().equals(GAEResource.class)));
  }

  @Test
  public void testAppEngineNotGCP() {
    GAEResource testResource = new GAEResource();

    // The default metadata url is unreachable through testing so getAttributes should not detect a
    // GCP environment, hence returning empty attributes.
    assertThat(testResource.getAttributes()).isEmpty();
  }

  @Test
  public void testAppEngineResourceNonGCPEndpoint() {
    // intentionally not providing the required Metadata-Flovor header with the
    // request to mimic non GCP endpoint
    stubFor(
        get(urlEqualTo("/project/project-id"))
            .willReturn(aResponse().withBody("nonGCPendpointTest")));
    GAEResource testResource = new GAEResource(metadataConfig, new EnvVarMock(envVars));
    assertThat(testResource.getAttributes()).isEmpty();
  }

  @Test
  public void testAppEngineResourceWithAppEngineAttributesSucceeds() {
    stubEndpoint("/project/project-id", "GCF-pid");
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/id", "GCF-instance-id");

    GAEResource testResource = new GAEResource(metadataConfig, new EnvVarMock(envVars));
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

  // Helper method to help stub endpoints
  private void stubEndpoint(String endpointPath, String responseBody) {
    stubFor(
        get(urlEqualTo(endpointPath))
            .willReturn(
                aResponse().withHeader("Metadata-Flavor", "Google").withBody(responseBody)));
  }
}
