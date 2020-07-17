package com.google.cloud.opentelemetry.trace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import io.opentelemetry.common.AttributeValue;
import java.util.Collections;
import java.util.Date;
import java.time.Duration;
import java.util.Map;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link TraceConfiguration}. */
@RunWith(JUnit4.class)
public class TraceConfigurationTest {

    private static final Credentials FAKE_CREDENTIALS = GoogleCredentials.newBuilder()
            .setAccessToken(new AccessToken("fake", new Date(100))).build();
    private static final String PROJECT_ID = "project";
    private static final Duration ONE_MINUTE = Duration.ofSeconds(60, 0);
    private static final Duration NEG_ONE_MINUTE = Duration.ofSeconds(-60, 0);

    @Test
    public void defaultConfiguration() {
        TraceConfiguration configuration;
        try {
            configuration = TraceConfiguration.builder().build();
        } catch (Exception e) {
            // Some test hosts may not have cloud project ID set up.
            configuration = TraceConfiguration.builder().setProjectId("test").build();
        }
        assertNull(configuration.getCredentials());
        assertNotNull(configuration.getProjectId());
        assertNull(configuration.getTraceServiceStub());
        assertTrue(configuration.getFixedAttributes().isEmpty());
        assertEquals(configuration.getDeadline(), TraceConfiguration.DEFAULT_DEADLINE);
    }

    @Test
    public void updateAll() {
        Map<String, AttributeValue> attributes = Collections.singletonMap("key",
                AttributeValue.stringAttributeValue("val"));
        TraceConfiguration configuration = TraceConfiguration.builder().setCredentials(FAKE_CREDENTIALS)
                .setProjectId(PROJECT_ID).setFixedAttributes(attributes).setDeadline(ONE_MINUTE).build();
        assertEquals(configuration.getCredentials(), FAKE_CREDENTIALS);
        assertEquals(configuration.getProjectId(), PROJECT_ID);
        assertEquals(configuration.getFixedAttributes(), attributes);
        assertEquals(configuration.getDeadline(), ONE_MINUTE);
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
        if (defaultProjectId != null) {
            TraceConfiguration configuration = TraceConfiguration.builder().build();
            assertEquals(configuration.getProjectId(), defaultProjectId);
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
        Map<String, AttributeValue> attributes = Collections.singletonMap(null,
                AttributeValue.stringAttributeValue("val"));
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
