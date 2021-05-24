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
package com.google.cloud.opentelemetry.endtoend;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;

public class Server {
  private final ScenarioHandlerManager scenarioHandlers = new ScenarioHandlerManager();

  public void pullForever() throws IOException {
    MessageReceiver receiver =
        (PubsubMessage message, AckReplyConsumer consumer) -> {
          System.out.println("got message: " + message.getData().toStringUtf8());
          consumer.ack();
        };

    Subscriber subscriber = null;
    try {
      subscriber =
          Subscriber.newBuilder(Constants.getRequestSubscription(), this::handleMessage).build();
      subscriber.addListener(
          new Subscriber.Listener() {
            @Override
            public void failed(Subscriber.State from, Throwable failure) {
              // Handle failure. This is called when the Subscriber encountered a fatal error and is
              // shutting down.
              System.err.println(failure);
            }
          },
          MoreExecutors.directExecutor());
      subscriber.startAsync().awaitRunning();
      // Docs for Subscriber recommend doing this to block main thread while daemon thread consumes
      // stuff.
      for (; ; ) {
        Thread.sleep(Long.MAX_VALUE);
      }
    } finally {
      if (subscriber != null) {
        subscriber.stopAsync();
      }
    }
  }

  public void respond(String testId, Response response) {
    // TODO - implement.
  }

  /** Execute a scenario based on the incoming message from the test runner. */
  public void handleMessage(PubsubMessage message, AckReplyConsumer consumer) {
    if (!message.containsAttributes(Constants.TEST_ID)) {
      consumer.nack();
      return;
    }
    String testId = message.getAttributesOrDefault(Constants.TEST_ID, "");
    if (!message.containsAttributes(Constants.SCENARIO)) {
      respond(testId, Response.invalidArugment("Exepcted attribute \"{SCENARIO}\" is missing"));
      consumer.ack();
      return;
    }
    String scenario = message.getAttributesOrDefault(Constants.SCENARIO, "");
    Request request = Request.make(testId, message.getAttributesMap(), message.getData());

    // Run the given request/response cycle through a handler and respond with results.
    Response response = Response.EMPTY;
    try {
      response = scenarioHandlers.handleScenario(scenario, request);
    } catch (Throwable e) {
      response = Response.internalError(e);
    } finally {
      respond(testId, response);
      consumer.ack();
    }
  }
}
