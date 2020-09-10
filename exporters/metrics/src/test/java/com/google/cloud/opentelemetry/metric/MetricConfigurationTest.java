package com.google.cloud.opentelemetry.metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.opentelemetry.metric.MetricConfiguration.Builder;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetricConfigurationTest {

  private static final Credentials FAKE_CREDENTIALS =
      GoogleCredentials.newBuilder().setAccessToken(new AccessToken("fake", new Date(100))).build();
  private static final String PROJECT_ID = "project";

  @Test
  public void testDefaultConfigurationSucceeds() {
    MetricConfiguration configuration = MetricConfiguration.builder().setProjectId(PROJECT_ID).build();

    assertNull(configuration.getCredentials());
    assertEquals(PROJECT_ID, configuration.getProjectId());
    assertNull(configuration.getMetricServiceStub());
  }

  @Test
  public void testSetAllConfigurationFieldsSucceeds() {
    MetricConfiguration configuration = MetricConfiguration.builder()
        .setProjectId(PROJECT_ID)
        .setCredentials(FAKE_CREDENTIALS)
        .build();

    assertEquals(FAKE_CREDENTIALS, configuration.getCredentials());
    assertEquals(PROJECT_ID, configuration.getProjectId());
  }

  @Test
  public void testConfigurationWithNullProjectIdFails() {
    Builder builder = MetricConfiguration.builder();
    assertThrows(NullPointerException.class, () -> builder.setProjectId(null));
  }

  @Test
  public void testConfigurationWithEmptyProjectIdFails() {
    Builder builder = MetricConfiguration.builder();
    builder.setProjectId("");
    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  public void testConfigurationWithDefaultProjectIdSucceeds() {
    String defaultProjectId = ServiceOptions.getDefaultProjectId();
    if (defaultProjectId != null) {
      MetricConfiguration configuration = MetricConfiguration.builder().build();
      assertEquals(defaultProjectId, configuration.getProjectId());
    }
  }
}
