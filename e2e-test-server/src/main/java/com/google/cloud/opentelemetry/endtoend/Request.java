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

import com.google.protobuf.ByteString;
import java.util.Map;

/** Abstraction of an "RPC Request" in our e2e integration test. */
public interface Request {
  /** The test scenario requested. */
  String testId();

  /** Text map headers (for eventual trace propagation). */
  Map<String, String> headers();

  /** Incoming message data. */
  ByteString data();

  static Request make(
      final String testId, final Map<String, String> headers, final ByteString data) {
    return new Request() {
      public String testId() {
        return testId;
      }

      public Map<String, String> headers() {
        return headers;
      }

      public ByteString data() {
        return data;
      }
    };
  }
}
