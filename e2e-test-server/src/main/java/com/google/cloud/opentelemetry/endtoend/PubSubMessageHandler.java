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

import com.google.pubsub.v1.PubsubMessage;

/**
 * An interface containing functionality to handle an incoming {@link PubsubMessage} and returning
 * appropriate response indicating if handling was successful or not.
 */
public interface PubSubMessageHandler {

  /**
   * Represents the possible responses for handling an incoming {@link PubsubMessage} handled via
   * PubSubMessageHandler.
   */
  enum PubSubMessageResponse {
    /**
     * Response that should be sent when a {@link PubsubMessage} has been successfully processed.
     * The service should not send the message again.
     */
    ACK("ack"),

    /**
     * Response that should be sent when a {@link PubsubMessage} has not been successfully
     * processed. The service should resend the message.
     */
    NACK("nack");

    private final String stringValue;

    /**
     * Constructor for the {@link PubSubMessageResponse}.
     *
     * @param stringValue the string representation for the enum value.
     */
    PubSubMessageResponse(String stringValue) {
      this.stringValue = stringValue;
    }

    @Override
    public String toString() {
      return this.stringValue;
    }
  }

  /**
   * This method accepts and processes an incoming {@link PubsubMessage}.
   *
   * @param message The incoming {@link PubsubMessage} that should be processed.
   * @return a {@link PubSubMessageResponse} indicating if the message was processed successfully.
   */
  PubSubMessageResponse handlePubSubMessage(PubsubMessage message);

  /**
   * This method is responsible for doing any cleanup tasks required for the {@link
   * PubSubMessageHandler} when handler is no longer required.
   */
  void cleanupMessageHandler();
}
