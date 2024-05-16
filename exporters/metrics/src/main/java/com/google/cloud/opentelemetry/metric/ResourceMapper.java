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
package com.google.cloud.opentelemetry.metric;

import com.google.api.MonitoredResource;
import io.opentelemetry.sdk.resources.Resource;

/**
 * Specifies mapping rules to convert OpenTelemetry's {@link Resource} to Google's {@link
 * MonitoredResource}.
 */
public interface ResourceMapper {
  /**
   * Method to convert a given OpenTelemetry {@link Resource} to Google specific {@link
   * MonitoredResource}.
   *
   * @param resource The OpenTelemetry resource to convert.
   * @return the converted {@link MonitoredResource}.
   */
  MonitoredResource mapResource(Resource resource);
}
