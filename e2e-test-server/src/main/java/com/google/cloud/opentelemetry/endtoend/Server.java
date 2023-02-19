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
package com.google.cloud.opentelemetry.endtoend;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.PubsubMessage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Server implements the "integration test driver" for this language.
 *
 * <p>It is responsible for the following:
 *
 * <ul>
 *   <li>Implementing logic for handling incoming {@link PubsubMessage}s.
 *   <li>Starting the correct server to run integration tests depending on {@link
 *       Constants#SUBSCRIPTION_MODE}.
 * </ul>
 *
 * <p>This class includes a main method which runs the integration test driver using locally
 * available credentials to access pubsub channels.
 */
public class Server implements PubSubMessageHandler {

  private final ScenarioHandlerManager scenarioHandlers;
  private final Publisher publisher;

  public Server() throws Exception {
    this.scenarioHandlers = new ScenarioHandlerManager();
    this.publisher = Publisher.newBuilder(Constants.getResponseTopic()).build();
  }

  @Override
  public PubSubMessageResponse handlePubSubMessage(PubsubMessage message) {
    if (!message.containsAttributes(Constants.TEST_ID)) {
      return PubSubMessageResponse.NACK;
    }
    String testId = message.getAttributesOrDefault(Constants.TEST_ID, "");
    if (!message.containsAttributes(Constants.SCENARIO)) {
      respond(
          testId,
          Response.invalidArgument(
              String.format("Expected attribute \"%s\" is missing", Constants.SCENARIO)));
      return PubSubMessageResponse.ACK;
    }
    String scenario = message.getAttributesOrDefault(Constants.SCENARIO, "");
    Request request = Request.make(testId, message.getAttributesMap(), message.getData());

    // Run the given request/response cycle through a handler and respond with results.
    Response response = Response.EMPTY;
    try {
      response = scenarioHandlers.handleScenario(scenario, request);
    } catch (Throwable e) {
      e.printStackTrace(System.err);
      response = Response.internalError(e);
    } finally {
      respond(testId, response);
    }
    return PubSubMessageResponse.ACK;
  }

  /**
   * This method is responsible for doing any cleanup tasks required for the {@link
   * PubSubMessageHandler} when handler is no longer required.
   */
  @Override
  public void cleanupMessageHandler() {
    if (publisher != null) {
      publisher.shutdown();
    }
  }

  /** This method converts from {@link Response} to pubsub and sends out the publisher channel. */
  private void respond(final String testId, final Response response) {
    final PubsubMessage message =
        PubsubMessage.newBuilder()
            .putAllAttributes(response.headers())
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
            t.printStackTrace();
          }
        },
        MoreExecutors.directExecutor());
    try {
      // Wait for the future to get completed.
      // This prevents cloud functions from exiting too quickly
      messageIdFuture.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      e.printStackTrace();
    }
  }

  private static PubSubServer createPubSubServer(Server server) {
    if (Constants.SUBSCRIPTION_MODE.equals(Constants.SUBSCRIPTION_MODE_PULL)) {
      return new PubSubPullServer(server);
    }
    return new PubSubPushServer(Integer.parseInt(Constants.PUSH_PORT), server);
  }

  /** Runs our server. */
  public static void main(String[] args) throws Exception {
    Server server = new Server();
    try (PubSubServer pubSubServer = createPubSubServer(server)) {
      pubSubServer.start();
      // Docs for Subscriber recommend doing this to block main thread while daemon thread
      // consumes stuff.
      for (; ; ) {
        Thread.sleep(Long.MAX_VALUE);
      }
    }
  }
}
