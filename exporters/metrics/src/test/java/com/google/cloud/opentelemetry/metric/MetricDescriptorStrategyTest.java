package com.google.cloud.opentelemetry.metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.api.MetricDescriptor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetricDescriptorStrategyTest {
    @Test
    public void testNeverSendStrategy() {
        MetricDescriptorStrategy strategy = MetricDescriptorStrategy.NEVER_SEND;
        final AtomicBoolean wasExported = new AtomicBoolean(false);
        final MetricDescriptor descriptor = MetricDescriptor.newBuilder().setName("Test").build();
        strategy.exportDescriptors(
            Collections.singleton(descriptor), 
            desc -> { wasExported.set(true); });
        assertEquals("Strategy should not send descriptors", false, wasExported.get());
        strategy.exportDescriptors(
            Collections.singleton(descriptor), 
            desc -> { wasExported.set(true); });
        assertEquals("Strategy should not send descriptors", false, wasExported.get());
        strategy.exportDescriptors(
            Collections.singleton(descriptor), 
            desc -> { wasExported.set(true); });
        assertEquals("Strategy should not send descriptors", false, wasExported.get());
    }

    @Test
    public void testAlwaysSendStrategy() {
        MetricDescriptorStrategy strategy = MetricDescriptorStrategy.ALWAYS_SEND;
        final AtomicBoolean wasExported = new AtomicBoolean(false);
        final MetricDescriptor descriptor = MetricDescriptor.newBuilder().setName("Test").build();
        strategy.exportDescriptors(
            Collections.singleton(descriptor), 
            desc -> { wasExported.set(true); });
        assertTrue("Strategy should send descriptors", wasExported.get());
        wasExported.set(false);
        strategy.exportDescriptors(
            Collections.singleton(descriptor), 
            desc -> { wasExported.set(true); });
        assertTrue("Strategy should send descriptors", wasExported.get());
        wasExported.set(false);
        strategy.exportDescriptors(
            Collections.singleton(descriptor), 
            desc -> { wasExported.set(true); });
        assertTrue("Strategy should send descriptors", wasExported.get());
    }

    @Test
    public void testSendOnceStrategy() {
        MetricDescriptorStrategy strategy = MetricDescriptorStrategy.SEND_ONCE;
        final MetricDescriptor descriptor = MetricDescriptor.newBuilder().setName("Test").build();
        final AtomicBoolean wasExported = new AtomicBoolean(false);
        strategy.exportDescriptors(
            Collections.singleton(descriptor), 
            desc -> { wasExported.set(true); });
        assertEquals("Strategy should send descriptors", true, wasExported.get());
        wasExported.set(false);
        strategy.exportDescriptors(
            Collections.singleton(descriptor), 
            desc -> { wasExported.set(true); });
        assertEquals("Strategy should not send descriptors", false, wasExported.get());
        wasExported.set(false);
        strategy.exportDescriptors(
            Collections.singleton(descriptor), 
            desc -> { wasExported.set(true); });
        assertEquals("Strategy should not send descriptors", false, wasExported.get());
    }
}
