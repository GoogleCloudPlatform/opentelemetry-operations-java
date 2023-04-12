/*
 * Copyright 2023 Google LLC
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
package com.google.cloud.opentelemetry.resource;

import com.google.auto.value.AutoValue;

/**
 * A representation of GCP resource types.
 *
 * <p>Unlike pure OpenTelemetry, GCP adds a "type" to a raw bundle of labels.
 */
@AutoValue
public abstract class GcpResource {
  /** The type of resource, e.g. gce_instance. */
  public abstract String getResourceType();
  /** The labels associated with the resource. */
  public abstract ResourceLabels getResourceLabels();

  static Builder builder() {
    return new AutoValue_GcpResource.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder setResourceType(String value);

    abstract ResourceLabels.Builder resourceLabelsBuilder();

    final Builder addResourceLabels(String key, String value) {
      resourceLabelsBuilder().put(key, value);
      return this;
    }

    abstract GcpResource build();
  }
}
