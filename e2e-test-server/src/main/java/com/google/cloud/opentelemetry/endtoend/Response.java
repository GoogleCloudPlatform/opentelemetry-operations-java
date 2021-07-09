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
import java.util.HashMap;
import java.util.Map;

/**
 * An "RPC Response", generified.
 *
 * <p>This class exposes the minimum API to write rpc-like integration tests.
 */
public interface Response {
  /**
   * Status code associate with response.
   *
   * <p>If this is `OK`, then data will be empty.
   */
  Code statusCode();

  /** string explanation of error codes. */
  ByteString data();

  Map<String, String> headers();

  static Response make(final Code code, final ByteString data) {
    return make(code, data, new HashMap<>());
  }

  static Response make(final Code code, final ByteString data, final Map<String, String> headers) {
    return new Response() {
      public Code statusCode() {
        return code;
      }

      public ByteString data() {
        return data;
      }

      public Map<String, String> headers() {
        return headers;
      }
    };
  }

  public static Response internalError(Throwable t) {
    return make(Code.INTERNAL, ByteString.copyFromUtf8(t.toString()));
  }

  public static Response invalidArgument(String message) {
    return make(Code.INVALID_ARGUMENT, ByteString.copyFromUtf8(message));
  }

  public static Response unimplemented(String message) {
    return make(Code.UNIMPLEMENTED, ByteString.copyFromUtf8(message));
  }

  public static Response ok() {
    return make(Code.OK, ByteString.EMPTY);
  }

  public static Response ok(Map<String, String> headers) {
    return make(Code.OK, ByteString.EMPTY, headers);
  }

  public static Response EMPTY = make(Code.UNKNOWN, ByteString.EMPTY);
}
