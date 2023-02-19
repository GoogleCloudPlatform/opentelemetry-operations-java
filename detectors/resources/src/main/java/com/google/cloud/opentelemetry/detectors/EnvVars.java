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
package com.google.cloud.opentelemetry.detectors;

/**
 * Provides API to fetch environment variables. This is useful in order to create a mock class for
 * testing.
 */
public interface EnvVars {
  EnvVars DEFAULT_INSTANCE = System::getenv;

  /**
   * Grabs the system environment variable. Returns null on failure.
   *
   * @param key the key of the environment variable in {@code System.getenv()}
   * @return the value received by {@code System.getenv(key)}
   */
  String get(String key);
}
