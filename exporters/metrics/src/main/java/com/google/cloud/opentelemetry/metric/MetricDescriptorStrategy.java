package com.google.cloud.opentelemetry.metric;

import com.google.api.MetricDescriptor;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * The strategy for how to handle metric descriptors.
 */
public interface MetricDescriptorStrategy {
    /**
     * Determines what to do with metrci descriptors.
     * 
     * @param batchDescriptors The set of metrics being exported in a batch.
     * @param client           A consumer that will ensure metric descriptors are
     *                         registered to cloud monitoring.
     */
    void exportDescriptors(Collection<MetricDescriptor> batchDescriptors, Consumer<MetricDescriptor> export);

    /** A strategy that always sends metric descriptors. */
    public static MetricDescriptorStrategy ALWAYS_SEND = new MetricDescriptorStrategy() {

        @Override
        public void exportDescriptors(Collection<MetricDescriptor> batchDescriptors,
                Consumer<MetricDescriptor> export) {
            for (MetricDescriptor descriptor : batchDescriptors) {
                export.accept(descriptor);
            }

        }
    };
    /**
     * A strategy that never sends metric descriptors and relies on auto-creation.
     */
    public static MetricDescriptorStrategy NEVER_SEND = new MetricDescriptorStrategy() {
        @Override
        public void exportDescriptors(Collection<MetricDescriptor> batchDescriptors,
                Consumer<MetricDescriptor> export) {
        }
    };

    /** A strategy that sends descriptors once per classloader instance. */
    public static MetricDescriptorStrategy SEND_ONCE = new MetricDescriptorStrategy() {
        private Set<String> alreadySent = new HashSet<>();

        @Override
        public synchronized void exportDescriptors(Collection<MetricDescriptor> batchDescriptors,
                Consumer<MetricDescriptor> export) {
            for (MetricDescriptor descriptor : batchDescriptors) {
                if (!alreadySent.contains(descriptor.getName())) {
                    export.accept(descriptor);
                    alreadySent.add(descriptor.getName());
                }
            }

        }
    };
}
