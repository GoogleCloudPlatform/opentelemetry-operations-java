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
package com.google.cloud.opentelemetry.endtoend;

import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;

/** Constants we use in this test. Note: Some are pulled from the env. */
public class Constants {

  public static final String INSTRUMENTING_MODULE_NAME = "opentelemetry-ops-e2e-test-server";
  public static final String SCENARIO = "scenario";
  public static final String STATUS_CODE = "status_code";
  public static final String TEST_ID = "test_id";
  public static final String TRACE_ID = "trace_id";
  public static final String SUBSCRIPTION_MODE_PUSH = "push";
  public static final String SUBSCRIPTION_MODE_PULL = "pull";

  // TODO: Add good error messages below.
  public static String SUBSCRIPTION_MODE = System.getenv().getOrDefault("SUBSCRIPTION_MODE", "");
  public static String PROJECT_ID = System.getenv().getOrDefault("PROJECT_ID", "");
  public static String REQUEST_SUBSCRIPTION_NAME =
      System.getenv().getOrDefault("REQUEST_SUBSCRIPTION_NAME", "");
  public static String RESPONSE_TOPIC_NAME =
      System.getenv().getOrDefault("RESPONSE_TOPIC_NAME", "");
  public static String PUSH_PORT = System.getenv().getOrDefault("PUSH_PORT", "");

  public static ProjectSubscriptionName getRequestSubscription() {
    return ProjectSubscriptionName.of(PROJECT_ID, REQUEST_SUBSCRIPTION_NAME);
  }

  /** @return The pubsub channel we get test requests from. */
  public static ProjectTopicName getResponseTopic() {
    return ProjectTopicName.of(PROJECT_ID, RESPONSE_TOPIC_NAME);
  }
}
