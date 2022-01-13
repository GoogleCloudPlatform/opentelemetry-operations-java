/*
 * Copyright 2021 Google
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
import static org.junit.Assert.assertTrue;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GKEResourceTest {
  @Rule public final WireMockRule wireMockRule = new WireMockRule(8089);
  private final GCPMetadataConfig metadataConfig = new GCPMetadataConfig("http://localhost:8089/");

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
        "Could not load GKE Resource detector using serviceloader, found: " + services,
        services.stream().anyMatch(provider -> provider.type().equals(GKEResource.class)));
  }

  @Test
  public void testGKEResourceNotGCP() {
    GKEResource test = new GKEResource();

    /* The default meta data url is unreachable through testing so getAttributes should not detect a
    GCP environment, hence returning empty attributes */
    // Note: Currently this does not support GKE outside of Google Cloud (e.g. GKE on AWS, GKE on
    // premise)
    assertThat(test.getAttributes()).isEmpty();
  }

  @Test
  public void testGKEResourceWithGKEAttributesSucceeds() {
    stubEndpoint("/project/project-id", "GCE-pid");
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/id", "GCE-instance-id");
    stubEndpoint("/instance/name", "GCE-instance-name");
    stubEndpoint("/instance/machine-type", "GCE-instance-type");
    stubEndpoint("/instance/attributes/cluster-name", "GKE-cluster-name");

    Map<String, String> map = new HashMap<>();
    map.put("KUBERNETES_SERVICE_HOST", "GKE-testHost");
    map.put("NAMESPACE", "GKE-testNameSpace");
    map.put("HOSTNAME", "GKE-testHostName");
    map.put("CONTAINER_NAME", "GKE-testContainerName");

    GKEResource testResource = new GKEResource(metadataConfig, new EnvVarMock(map));
    assertThat(testResource.getAttributes())
            .hasSize(12)
            .containsEntry(ResourceAttributes.CLOUD_PROVIDER, "gcp")
            .containsEntry(ResourceAttributes.CLOUD_PLATFORM, "gcp_kubernetes_engine")
            .containsEntry(ResourceAttributes.CLOUD_ACCOUNT_ID, "GCE-pid")
            .containsEntry(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone")
            .containsEntry(ResourceAttributes.CLOUD_REGION, "country-region")
            .containsEntry(ResourceAttributes.HOST_ID, "GCE-instance-id")
            .containsEntry(ResourceAttributes.HOST_NAME, "GCE-instance-name")
            .containsEntry(ResourceAttributes.HOST_TYPE, "GCE-instance-type")
            .containsEntry(ResourceAttributes.K8S_CLUSTER_NAME, "GKE-cluster-name")
            .containsEntry(ResourceAttributes.K8S_NAMESPACE_NAME, "GKE-testNameSpace")
            .containsEntry(ResourceAttributes.K8S_POD_NAME, "GKE-testHostName")
            .containsEntry(ResourceAttributes.K8S_CONTAINER_NAME, "GKE-testContainerName")
    ;
  }
}
