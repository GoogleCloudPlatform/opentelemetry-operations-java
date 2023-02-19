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
package com.google.cloud.opentelemetry.metric;

import com.google.api.MetricDescriptor;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** The strategy for how to handle metric descriptors. */
public interface MetricDescriptorStrategy {
  /**
   * Determines what to do with metric descriptors.
   *
   * @param batchDescriptors The set of metrics being exported in a batch.
   * @param export A consumer that will ensure metric descriptors are registered to cloud
   *     monitoring.
   */
  void exportDescriptors(
      Iterable<MetricDescriptor> batchDescriptors, Consumer<MetricDescriptor> export);

  /**
   * A strategy that always sends metric descriptors.
   *
   * <p>This means EVERY create timeseries call will include several metric descriptor calls. This
   * is not recommended.
   */
  public static MetricDescriptorStrategy ALWAYS_SEND =
      new MetricDescriptorStrategy() {

        @Override
        public void exportDescriptors(
            Iterable<MetricDescriptor> batchDescriptors, Consumer<MetricDescriptor> export) {
          for (MetricDescriptor descriptor : batchDescriptors) {
            export.accept(descriptor);
          }
        }
      };
  /** A strategy that never sends metric descriptors and relies on auto-creation. */
  public static MetricDescriptorStrategy NEVER_SEND =
      new MetricDescriptorStrategy() {
        @Override
        public void exportDescriptors(
            Iterable<MetricDescriptor> batchDescriptors, Consumer<MetricDescriptor> export) {}
      };

  /** A strategy that sends descriptors once per classloader instance. */
  public static MetricDescriptorStrategy SEND_ONCE =
      new MetricDescriptorStrategy() {
        private final Set<String> alreadySent = new HashSet<>();

        @Override
        public synchronized void exportDescriptors(
            Iterable<MetricDescriptor> batchDescriptors, Consumer<MetricDescriptor> export) {
          for (MetricDescriptor descriptor : batchDescriptors) {
            if (!alreadySent.contains(descriptor.getType())) {
              export.accept(descriptor);
              alreadySent.add(descriptor.getType());
            }
          }
        }
      };
}
