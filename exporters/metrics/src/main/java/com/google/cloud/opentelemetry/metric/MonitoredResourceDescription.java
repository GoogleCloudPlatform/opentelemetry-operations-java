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

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Set;

/**
 * This class holds the mapping between Google Cloud's monitored resource type and the labels for
 * identifying the given monitored resource type.
 */
@Immutable
public final class MonitoredResourceDescription {
  private final String mrType;
  private final Set<String> mrLabels;

  /**
   * Public constructor.
   *
   * @param mrType The monitored resource type for which the mapping is being specified.
   * @param mrLabels A set of labels which uniquely identify a given monitored resource.
   */
  public MonitoredResourceDescription(String mrType, Set<String> mrLabels) {
    this.mrType = mrType;
    this.mrLabels = Collections.unmodifiableSet(mrLabels);
  }

  /**
   * Returns the set of labels used to identify the monitored resource represented in this mapping.
   *
   * @return Immutable set of labels that map to the specified monitored resource type.
   */
  public Set<String> getMonitoredResourceLabels() {
    return mrLabels;
  }

  /**
   * The type of the monitored resource for which mapping is defined.
   *
   * @return The type of the monitored resource.
   */
  public String getMonitoredResourceType() {
    return mrType;
  }
}
