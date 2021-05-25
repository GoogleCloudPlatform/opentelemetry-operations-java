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

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.PubsubMessage;

/**
 * Server implements the "integration test driver" for this language.
 *
 * <p>It is responsible for the following:
 *
 * <ul>
 *   <li>Setting up a subscriber queue for inbound "RPC Request" messages
 *   <li>Converting incoming pub sub messages to {@link Request}
 *   <li>Setting up a publisher queue for outbound "RPC Response" messages
 *   <li>Converting from outbound {@link Response} to pubsub messages.
 *   <li>Handling any/all failures escaping the test scenario.
 * </ul>
 *
 * <p>This class includes a main method which runs the integration test driver using locally
 * available credentials to acccess pubsub channels.
 */
public class Server implements AutoCloseable {
  private final ScenarioHandlerManager scenarioHandlers = new ScenarioHandlerManager();
  private final Publisher publisher;
  private final Subscriber subscriber;

  public Server() throws Exception {
    this.publisher = Publisher.newBuilder(Constants.getResponseTopic()).build();
    this.subscriber =
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
  }

  /** Starts the subcriber pulling requests. */
  public void start() {
    subscriber.startAsync().awaitRunning();
  }

  /** Closes our subscriptions. */
  public void close() {
    if (subscriber != null) {
      subscriber.stopAsync();
      subscriber.awaitTerminated();
    }
    if (publisher != null) {
      publisher.shutdown();
    }
  }

  /** This method converts from {@link Response} to pubsub and sends out the publisher channel. */
  public void respond(final String testId, final Response response) {
    final PubsubMessage message =
        PubsubMessage.newBuilder()
            .putAttributes(Constants.TEST_ID, testId)
            .putAttributes(Constants.STATUS_CODE, Integer.toString(response.statusCode().ordinal()))
            .setData(response.data())
            .build();
    ApiFuture<String> messageIdFuture = publisher.publish(message);
    ApiFutures.addCallback(
        messageIdFuture,
        new ApiFutureCallback<String>() {
          public void onSuccess(String messageId) {}

          public void onFailure(Throwable t) {
            System.out.println("failed to publish response to test: " + testId);
            t.printStackTrace();
          }
        },
        MoreExecutors.directExecutor());
  }

  /** Execute a scenario based on the incoming message from the test runner. */
  public void handleMessage(PubsubMessage message, AckReplyConsumer consumer) {
    if (!message.containsAttributes(Constants.TEST_ID)) {
      consumer.nack();
      return;
    }
    String testId = message.getAttributesOrDefault(Constants.TEST_ID, "");
    if (!message.containsAttributes(Constants.SCENARIO)) {
      respond(
          testId,
          Response.invalidArgument(
              String.format("Expected attribute \"%s\" is missing", Constants.SCENARIO)));
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

  /** Runs our server. */
  public static void main(String[] args) throws Exception {
    try (Server server = new Server()) {
      server.start();
      // Docs for Subscriber recommend doing this to block main thread while daemon thread consumes
      // stuff.
      for (; ; ) {
        Thread.sleep(Long.MAX_VALUE);
      }
    }
  }
}
