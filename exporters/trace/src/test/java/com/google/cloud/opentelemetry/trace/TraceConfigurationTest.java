/*
 * Copyright 2023 Google
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
package com.google.cloud.opentelemetry.trace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.trace.v2.TraceServiceClient;
import com.google.cloud.trace.v2.TraceServiceSettings;
import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.ProjectName;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

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
  public void allowToUseDefaultProjectId() {
    // some test providers might not have project IDs set up - so we use mocks
    try (MockedStatic<ServiceOptions> mockedServiceOptions =
            Mockito.mockStatic(ServiceOptions.class);
        MockedStatic<TraceServiceClient> mockedTraceServiceClient =
            Mockito.mockStatic(TraceServiceClient.class)) {
      TraceServiceClient mockedTraceServiceClientObject = Mockito.mock(TraceServiceClient.class);
      mockedServiceOptions.when(ServiceOptions::getDefaultProjectId).thenReturn(PROJECT_ID);
      mockedTraceServiceClient
          .when(() -> TraceServiceClient.create((TraceServiceSettings) Mockito.any()))
          .thenReturn(mockedTraceServiceClientObject);
      SpanExporter exporter = TraceExporter.createWithDefaultConfiguration();
      // export triggers the lazy initialization
      exporter.export(Collections.emptyList());
      // verify that there was an attempt to retrieve the default project ID
      mockedServiceOptions.verify(Mockito.times(1), ServiceOptions::getDefaultProjectId);
      // verify that the default project ID was indeed used when exporting spans
      Mockito.doAnswer(
              (Answer<Void>)
                  invocation -> {
                    Object[] args = invocation.getArguments();
                    assertEquals(ProjectName.of(PROJECT_ID).toString(), args[0].toString());
                    return null;
                  })
          .when(mockedTraceServiceClientObject)
          .batchWriteSpans((ProjectName) Mockito.any(), Mockito.anyList());
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

    assertThrows(NullPointerException.class, builder::build);
  }

  @Test
  public void disallowNullFixedAttributeValue() {
    TraceConfiguration.Builder builder = TraceConfiguration.builder().setProjectId("test");

    Map<String, AttributeValue> attributes = Collections.singletonMap("key", null);
    builder.setFixedAttributes(attributes);

    assertThrows(NullPointerException.class, builder::build);
  }

  @Test
  public void disallowZeroDuration() {
    TraceConfiguration.Builder builder = TraceConfiguration.builder().setProjectId("test");

    builder.setDeadline(TraceConfiguration.Builder.ZERO);

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  public void disallowNegativeDuration() {
    TraceConfiguration.Builder builder = TraceConfiguration.builder().setProjectId("test");

    builder.setDeadline(NEG_ONE_MINUTE);

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  public void verifyCallToDefaultProjectIdIsMemoize() {
    try (MockedStatic<ServiceOptions> serviceOptionsMockedStatic =
        Mockito.mockStatic(ServiceOptions.class)) {

      TraceConfiguration traceConfiguration1 = TraceConfiguration.builder().build();
      traceConfiguration1.getProjectId();
      traceConfiguration1.getProjectId();
      traceConfiguration1.getProjectId();

      TraceConfiguration traceConfiguration2 = TraceConfiguration.builder().build();
      traceConfiguration2.getProjectId();
      traceConfiguration2.getProjectId();
      traceConfiguration2.getProjectId();

      // ServiceOptions#getDefaultProjectId should only be called once per TraceConfiguration object
      serviceOptionsMockedStatic.verify(Mockito.times(2), ServiceOptions::getDefaultProjectId);
    }
  }
}
