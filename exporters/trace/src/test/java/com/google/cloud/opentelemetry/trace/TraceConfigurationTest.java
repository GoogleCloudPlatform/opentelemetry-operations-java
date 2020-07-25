package com.google.cloud.opentelemetry.trace;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import com.google.devtools.cloudtrace.v2.AttributeValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;

/** Unit tests for {@link TraceConfiguration}. */
@RunWith(JUnit4.class)
public class TraceConfigurationTest {

  private static final Credentials FAKE_CREDENTIALS =
      GoogleCredentials.newBuilder().setAccessToken(new AccessToken("fake", new Date(100))).build();
  private static final String PROJECT_ID = "project";
  private static final Duration ONE_MINUTE = Duration.ofSeconds(60, 0);
  private static final Duration NEG_ONE_MINUTE = Duration.ofSeconds(-60, 0);

  @Test
  public void defaultConfiguration() {
    // Some test hosts may not have cloud project ID set up, so setting it explicitly
    TraceConfiguration configuration = TraceConfiguration.builder().setProjectId("test").build();

    assertNull(configuration.getCredentials());
    assertNotNull(configuration.getProjectId());
    assertNull(configuration.getTraceServiceStub());
    assertTrue(configuration.getFixedAttributes().isEmpty());
    assertEquals(TraceConfiguration.DEFAULT_DEADLINE, configuration.getDeadline());
  }

  @Test
  public void setAllConfigurationFields() {
    Map<String, AttributeValue> attributes =
        Collections.singletonMap("key", AttributeValue.newBuilder().setBoolValue(true).build());

    // set all the fields different from their default values
    TraceConfiguration configuration =
        TraceConfiguration.builder()
            .setCredentials(FAKE_CREDENTIALS)
            .setProjectId(PROJECT_ID)
            .setFixedAttributes(attributes)
            .setDeadline(ONE_MINUTE)
            .build();

    // make sure the changes are reflected
    assertEquals(FAKE_CREDENTIALS, configuration.getCredentials());
    assertEquals(PROJECT_ID, configuration.getProjectId());
    assertEquals(attributes, configuration.getFixedAttributes());
    assertEquals(ONE_MINUTE, configuration.getDeadline());
  }

  @Test
  public void disallowNullProjectId() {
    TraceConfiguration.Builder builder = TraceConfiguration.builder();

    assertThrows(NullPointerException.class, () -> builder.setProjectId(null));
  }

  @Test
  public void disallowEmptyProjectId() {
    TraceConfiguration.Builder builder = TraceConfiguration.builder();

    builder.setProjectId("");

    assertThrows(IllegalArgumentException.class, () -> builder.build());
  }

  @Test
  public void allowToUseDefaultProjectId() {
    String defaultProjectId = ServiceOptions.getDefaultProjectId();
    // some test providers might not have project IDs set up
    if (defaultProjectId != null) {
      TraceConfiguration configuration = TraceConfiguration.builder().build();

      assertEquals(defaultProjectId, configuration.getProjectId());
    }
  }

  @Test
  public void disallowNullFixedAttributes() {
    TraceConfiguration.Builder builder = TraceConfiguration.builder().setProjectId("test");

    assertThrows(NullPointerException.class, () -> builder.setFixedAttributes(null));
  }

  @Test
  public void disallowNullFixedAttributeKey() {
    TraceConfiguration.Builder builder = TraceConfiguration.builder().setProjectId("test");

    Map<String, AttributeValue> attributes =
        Collections.singletonMap(null, AttributeValue.newBuilder().setBoolValue(true).build());
    builder.setFixedAttributes(attributes);

    assertThrows(NullPointerException.class, () -> builder.build());
  }

  @Test
  public void disallowNullFixedAttributeValue() {
    TraceConfiguration.Builder builder = TraceConfiguration.builder().setProjectId("test");

    Map<String, AttributeValue> attributes = Collections.singletonMap("key", null);
    builder.setFixedAttributes(attributes);

    assertThrows(NullPointerException.class, () -> builder.build());
  }

  @Test
  public void disallowZeroDuration() {
    TraceConfiguration.Builder builder = TraceConfiguration.builder().setProjectId("test");

    builder.setDeadline(TraceConfiguration.Builder.ZERO);

    assertThrows(IllegalArgumentException.class, () -> builder.build());
  }

  @Test
  public void disallowNegativeDuration() {
    TraceConfiguration.Builder builder = TraceConfiguration.builder().setProjectId("test");

    builder.setDeadline(NEG_ONE_MINUTE);

    assertThrows(IllegalArgumentException.class, () -> builder.build());
  }
}
