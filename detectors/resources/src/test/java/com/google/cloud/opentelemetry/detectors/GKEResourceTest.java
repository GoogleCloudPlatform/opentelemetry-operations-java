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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  public void testGKEResourceNotGCP() {
    GKEResource test = new GKEResource();

    /* The default meta data url is unreachable through testing so getAttributes should not detect a
    GCP environment, hence returning empty attributes */
    Attributes attr = test.getAttributes();

    assertTrue(attr.isEmpty());
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
    Attributes attr = testResource.getAttributes();

    Map<AttributeKey<String>, String> expectedAttributes =
        Stream.of(
                new Object[][] {
                  {SemanticAttributes.CLOUD_PROVIDER, "gcp"},
                  {SemanticAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"},
                  {SemanticAttributes.CLOUD_ZONE, "country-region-zone"},
                  {SemanticAttributes.CLOUD_REGION, "country-region"},
                  {SemanticAttributes.HOST_ID, "GCE-instance-id"},
                  {SemanticAttributes.HOST_NAME, "GCE-instance-name"},
                  {SemanticAttributes.HOST_TYPE, "GCE-instance-type"},
                  {SemanticAttributes.K8S_CLUSTER_NAME, "GKE-cluster-name"},
                  {SemanticAttributes.K8S_NAMESPACE_NAME, "GKE-testNameSpace"},
                  {SemanticAttributes.K8S_POD_NAME, "GKE-testHostName"},
                  {SemanticAttributes.K8S_CONTAINER_NAME, "GKE-testContainerName"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));

    assertEquals(11, attr.size());
    attr.forEach(
        (key, value) -> {
          assertEquals(expectedAttributes.get(key), value);
        });
  }
}
