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
package com.google.cloud.opentelemetry.endtoend;

import com.google.cloud.functions.CloudEventsFunction;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.pubsub.v1.PubsubMessage;
import io.cloudevents.CloudEvent;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * This class serves as an entrypoint for Google Cloud Function entrypoint. This entrypoint works
 * for 2nd gen functions that have event-based triggers.
 */
public class CloudFunctionHandler implements CloudEventsFunction {

  /**
   * Called to service an incoming event. This interface is implemented by user code to provide the
   * action for a given background function. If this method throws any exception (including any
   * {@link Error}) then the HTTP response will have a 500 status code.
   *
   * @param event the event.
   */
  @Override
  public void accept(CloudEvent event) {
    // The Pub/Sub message is passed as the CloudEvent's data payload.
    if (event.getData() != null) {
      PubsubMessage message = getDecodedMessage(event);
      Server server;
      try {
        server = new Server();
        server.handlePubSubMessage(message);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private PubsubMessage getDecodedMessage(CloudEvent event) {
    String cloudEventData =
        new String(Objects.requireNonNull(event.getData()).toBytes(), StandardCharsets.UTF_8);
    Gson gson = new Gson();
    JsonElement jsonRoot = JsonParser.parseString(cloudEventData);
    System.out.println("Parsed JSON is " + jsonRoot.toString());
    String msgStr = jsonRoot.getAsJsonObject().get("message").toString();
    System.out.println("Message String is " + msgStr);
    PubSubPushServer.Message message = gson.fromJson(msgStr, PubSubPushServer.Message.class);
    return PubsubMessage.newBuilder().putAllAttributes(message.getAttributes()).build();
  }
}
