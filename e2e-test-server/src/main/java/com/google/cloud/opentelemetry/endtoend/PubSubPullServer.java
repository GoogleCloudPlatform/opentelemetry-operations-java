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

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * A {@link PubSubServer} that can handle running the integration test scenarios when {@link
 * Constants#SUBSCRIPTION_MODE} points to 'pull' mode.
 *
 * <p>This kind of {@link PubSubServer} works well with Google Cloud's non-serverless compute
 * offerings like GCE and GKE. This server should be avoided for running tests in serverless
 * environments like CloudRun.
 *
 * <p>This class is responsible for the following:
 *
 * <ul>
 *   <li>Setting up a PubSub {@link Subscriber} with the appropriate subscription name and {@link
 *       com.google.cloud.pubsub.v1.MessageReceiver}.
 *   <li>Attaching a failure listener to subscriber to get causes for failures.
 * </ul>
 */
public class PubSubPullServer implements PubSubServer {

  private final PubSubMessageHandler pubSubMessageHandler;
  private final Subscriber subscriber;

  /**
   * Public constructor for {@link PubSubPullServer}.
   *
   * @param pubSubMessageHandler The {@link PubSubMessageHandler} that will be used to process
   *     incoming {@link com.google.pubsub.v1.PubsubMessage}s.
   */
  public PubSubPullServer(PubSubMessageHandler pubSubMessageHandler) {
    this.pubSubMessageHandler = pubSubMessageHandler;
    this.subscriber =
        Subscriber.newBuilder(
                Constants.getRequestSubscription(),
                (message, consumer) -> {
                  PubSubMessageHandler.PubSubMessageResponse response =
                      pubSubMessageHandler.handlePubSubMessage(message);
                  if (response == PubSubMessageHandler.PubSubMessageResponse.ACK) {
                    consumer.ack();
                  } else {
                    consumer.nack();
                  }
                })
            .build();
    this.subscriber.addListener(
        new Subscriber.Listener() {
          @Override
          public void failed(Subscriber.State from, Throwable failure) {
            // Handle failure. This is called when the Subscriber encountered a fatal error and is
            // shutting down.
            System.err.println(failure.getMessage());
          }
        },
        MoreExecutors.directExecutor());
  }

  @Override
  public void close() {
    if (subscriber != null) {
      subscriber.stopAsync();
      subscriber.awaitTerminated();
    }
    pubSubMessageHandler.cleanupMessageHandler();
  }

  @Override
  public void start() {
    subscriber.startAsync().awaitRunning();
  }
}
