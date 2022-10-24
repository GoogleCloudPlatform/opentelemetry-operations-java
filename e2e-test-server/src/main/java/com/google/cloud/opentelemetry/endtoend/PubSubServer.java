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

/**
 * Interface that represents a server program capable of processing {@link
 * com.google.pubsub.v1.PubsubMessage}s.
 */
public interface PubSubServer extends AutoCloseable {
  /**
   * Method responsible for starting the server. Once the server is 'started', it should begin
   * listening/processing requests.
   */
  void start();
}
