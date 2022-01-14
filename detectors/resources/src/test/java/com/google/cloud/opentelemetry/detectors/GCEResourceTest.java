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
import java.util.ServiceLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GCEResourceTest {
  @Rule public final WireMockRule wireMockRule = new WireMockRule(8089);
  private final GCPMetadataConfig metadataConfig = new GCPMetadataConfig("http://localhost:8089/");
  private final GCEResource testResource = new GCEResource(metadataConfig);

  // Helper method to help stub endpoints
  private void stubEndpoint(String endpointPath, String responseBody) {
    stubFor(
        get(urlEqualTo(endpointPath))
            .willReturn(
                aResponse().withHeader("Metadata-Flavor", "Google").withBody(responseBody)));
  }

  @Test
  public void findsWithServiceLoader() {
    ServiceLoader<ResourceProvider> services =
        ServiceLoader.load(ResourceProvider.class, getClass().getClassLoader());
    assertTrue(
        "Could not load GCE Resource detector using serviceloader, found: " + services,
        services.stream().anyMatch(provider -> provider.type().equals(GCEResource.class)));
  }

  @Test
  public void testGCEResourceNotGCP() {
    GCEResource test = new GCEResource();

    /* The default meta data url is unreachable through testing so getAttributes should not detect a
    GCP environment, hence returning empty attributes */
    assertThat(test.getAttributes()).isEmpty();
  }

  @Test
  public void testGCEResourceWithGCEAttributesSucceeds() {
    stubEndpoint("/project/project-id", "GCE-pid");
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/id", "GCE-instance-id");
    stubEndpoint("/instance/name", "GCE-instance-name");
    stubEndpoint("/instance/machine-type", "GCE-instance-type");

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

  @Test
  public void testGCEResourceNonGCPEndpoint() {
    stubFor(
        get(urlEqualTo("/project/project-id"))
            .willReturn(aResponse().withBody("nonGCPendpointTest")));
    assertThat(testResource.getAttributes()).isEmpty();
  }
}
