package com.google.cloud.opentelemetry.detectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Map;
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
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/plain")
                    .withBody(responseBody)));
  }

  @Test
  public void nonGCEResourceShouldReturnEmptyAttribute() {
    GCEResource test = new GCEResource();

    Attributes attr = test.getAttributes();

    assertTrue(attr.isEmpty());
  }

  @Test
  public void fullGCEResourceShouldPopulateAttributes() {
    stubEndpoint("/project/project-id", "GCE-pid");
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/id", "GCE-instance-id");
    stubEndpoint("/instance/name", "GCE-instance-name");
    stubEndpoint("/instance/machine-type", "GCE-instance-type");

    Attributes attr = testResource.getAttributes();

    Map<AttributeKey, String> expectedAttributes =
        Map.of(
            SemanticAttributes.CLOUD_PROVIDER, "gcp",
            SemanticAttributes.CLOUD_ACCOUNT_ID, "GCE-pid",
            SemanticAttributes.CLOUD_ZONE, "country-region-zone",
            SemanticAttributes.CLOUD_REGION, "country-region",
            SemanticAttributes.HOST_ID, "GCE-instance-id",
            SemanticAttributes.HOST_NAME, "GCE-instance-name",
            SemanticAttributes.HOST_TYPE, "GCE-instance-type");

    assertEquals(7, attr.size());
    attr.forEach(
        (key, value) -> {
          assertEquals(expectedAttributes.get(key), value);
        });
  }
}
