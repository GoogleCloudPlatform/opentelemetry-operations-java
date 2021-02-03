package com.google.cloud.opentelemetry.detectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import io.opentelemetry.api.common.Attributes;

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
    stubEndpoint("/project/project-id", "");

    Attributes attr = testResource.getAttributes();

    assertTrue(attr.isEmpty());
  }

  @Test
  public void fullGCEResourceShouldPopulateAttributes() {
    stubEndpoint("/project/project-id", "cyan-pid");
    stubEndpoint("/instance/zone", "country-region-zone");
    stubEndpoint("/instance/id", "cyan-instance-id");
    stubEndpoint("/instance/hostname", "cyan-instance-hostname");
    stubEndpoint("/instance/name", "cyan-instance-name");
    stubEndpoint("/instance/machine-type", "cyan-instance-type");

    Attributes attr = testResource.getAttributes();

    assertEquals("gcp", attr.get(SemanticAttributes.CLOUD_PROVIDER));
    assertEquals("cyan-pid", attr.get(SemanticAttributes.CLOUD_ACCOUNT_ID));
    assertEquals("country-region-zone", attr.get(SemanticAttributes.CLOUD_ZONE));
    assertEquals("country-region", attr.get(SemanticAttributes.CLOUD_REGION));
    assertEquals("cyan-instance-id", attr.get(SemanticAttributes.HOST_ID));
    assertEquals("cyan-instance-hostname", attr.get(SemanticAttributes.HOST_NAME));
    assertEquals("cyan-instance-type", attr.get(SemanticAttributes.HOST_TYPE));
  }
}
