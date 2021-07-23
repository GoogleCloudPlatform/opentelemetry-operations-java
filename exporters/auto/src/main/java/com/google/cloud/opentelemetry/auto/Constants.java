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
package com.google.cloud.opentelemetry.auto;

import java.util.Arrays;
import java.util.List;

public class Constants {

  static final String CLOUD_TRACE_NAME = "google_cloud_trace";

  static final List<String> CLOUD_MONITORING_EXPORTER_NAMES =
      Arrays.asList("google_cloud", "google_cloud_monitoring");
  static final List<String> CLOUD_TRACE_EXPORTER_NAMES =
      Arrays.asList("google_cloud", CLOUD_TRACE_NAME);
}
