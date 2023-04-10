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
import java.util.HashMap;
import java.util.Map;

/**
 * A storage of key-value pairs.
 *
 * <p>This is a workaround to get AutoValue/AutoBuilder convenience without a full dependency on
 * Guice for collections.
 */
@AutoValue
public abstract class ResourceLabels {
  public abstract Map<String, String> getLabels();

  static Builder builder() {
    return new Builder();
  }

  Builder toBuilder() {
    return new Builder(this);
  }

  static final class Builder {
    private Map<String, String> labels;

    public Builder() {
      this.labels = new HashMap<>();
    }

    Builder(ResourceLabels labels) {
      this.labels = new HashMap<>(labels.getLabels());
    }

    public Builder put(String key, String value) {
      this.labels.put(key, value);
      return this;
    }

    public ResourceLabels build() {
      return new AutoValue_ResourceLabels(labels);
    }
  }
}
