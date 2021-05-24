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

import com.google.api.gax.rpc.StatusCode.Code;
import com.google.protobuf.ByteString;

public interface Response {
  Code statusCode();

  ByteString data();

  static Response make(final Code code, final ByteString data) {
    return new Response() {
      public Code statusCode() {
        return code;
      }

      public ByteString data() {
        return data;
      }
    };
  }

  static Response internalError(Throwable t) {
    return make(Code.INTERNAL, ByteString.copyFromUtf8(t.toString()));
  }

  static Response invalidArugment(String message) {
    return make(Code.INVALID_ARGUMENT, ByteString.copyFromUtf8(message));
  }

  public static Response EMPTY = make(Code.UNKNOWN, ByteString.EMPTY);
}
