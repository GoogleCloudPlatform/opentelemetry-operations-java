/*
 * Copyright 2023 Google LLC
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class MetricConfigurationTest {

  private static final Credentials FAKE_CREDENTIALS =
      GoogleCredentials.newBuilder().setAccessToken(new AccessToken("fake", new Date(100))).build();
  private static final String PROJECT_ID = "project";

  @Test
  public void testDefaultConfigurationSucceeds() {
    MetricConfiguration configuration =
        MetricConfiguration.builder().setProjectId(PROJECT_ID).build();

    assertNull(configuration.getCredentials());
    assertEquals(PROJECT_ID, configuration.getProjectId());
  }

  @Test
  public void testSetAllConfigurationFieldsSucceeds() {
    MetricConfiguration configuration =
        MetricConfiguration.builder()
            .setProjectId(PROJECT_ID)
            .setCredentials(FAKE_CREDENTIALS)
            .build();

    assertEquals(FAKE_CREDENTIALS, configuration.getCredentials());
    assertEquals(PROJECT_ID, configuration.getProjectId());
  }

  @Test
  public void testConfigurationWithEmptyProjectIdFails() {
    Builder builder = MetricConfiguration.builder();
    assertThrows(IllegalArgumentException.class, () -> builder.setProjectId(""));
    assertThrows(IllegalArgumentException.class, () -> builder.setProjectId(null));
  }

  @Test
  public void testConfigurationWithDefaultProjectIdSucceeds() {
    try (MockedStatic<ServiceOptions> serviceOptionsMockedStatic =
        Mockito.mockStatic(ServiceOptions.class)) {
      serviceOptionsMockedStatic.when(ServiceOptions::getDefaultProjectId).thenReturn(PROJECT_ID);

      MetricConfiguration configuration = MetricConfiguration.builder().build();
      assertEquals(PROJECT_ID, configuration.getProjectId());
      serviceOptionsMockedStatic.verify(Mockito.times(1), ServiceOptions::getDefaultProjectId);
    }
  }

  @Test
  public void verifyCallToDefaultProjectIdIsMemoized() {
    try (MockedStatic<ServiceOptions> serviceOptionsMockedStatic =
        Mockito.mockStatic(ServiceOptions.class)) {

      MetricConfiguration metricConfiguration1 = MetricConfiguration.builder().build();
      metricConfiguration1.getProjectId();
      metricConfiguration1.getProjectId();
      metricConfiguration1.getProjectId();

      MetricConfiguration metricConfiguration2 = MetricConfiguration.builder().build();
      metricConfiguration2.getProjectId();
      metricConfiguration2.getProjectId();
      metricConfiguration2.getProjectId();

      // ServiceOptions#getDefaultProjectId should only be called once per TraceConfiguration object
      serviceOptionsMockedStatic.verify(Mockito.times(2), ServiceOptions::getDefaultProjectId);
    }
  }
}
