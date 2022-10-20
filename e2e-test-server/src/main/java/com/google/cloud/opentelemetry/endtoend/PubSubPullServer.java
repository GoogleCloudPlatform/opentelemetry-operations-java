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

import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.util.concurrent.MoreExecutors;

public class PubSubPullServer implements PubSubServer {

  private final Publisher publisher;
  private final Subscriber subscriber;

  public PubSubPullServer(Publisher publisher, PubSubMessageHandler pubSubMessageHandler) {
    this.publisher = publisher;
    this.subscriber =
        Subscriber.newBuilder(
                Constants.getRequestSubscription(), pubSubMessageHandler::handlePubSubMessage)
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
    if (publisher != null) {
      publisher.shutdown();
    }
  }

  @Override
  public void start() {
    subscriber.startAsync().awaitRunning();
  }
}
