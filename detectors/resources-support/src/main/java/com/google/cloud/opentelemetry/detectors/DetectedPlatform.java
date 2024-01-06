/*
 * Copyright 2024 Google LLC
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

import java.util.Map;

/** Represents a GCP specific platform on which a cloud application can run. */
public interface DetectedPlatform {
  /**
   * Method to retrieve the underlying compute platform on which application is running.
   *
   * @return the {@link GCPPlatformDetector.SupportedPlatform} representing the Google Cloud
   *     platform on which application is running.
   */
  GCPPlatformDetector.SupportedPlatform getSupportedPlatform();

  /**
   * Method to retrieve the attributes associated with the compute platform on which the application
   * is running as key-value pairs.
   *
   * @return a {@link Map} of attributes specific to the underlying compute platform.
   */
  Map<String, String> getAttributes();
}
