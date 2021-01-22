package com.google.cloud.opentelemetry.metric;

import com.google.api.MetricDescriptor;
import com.google.monitoring.v3.TimeSeries;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.LongPoint;
import io.opentelemetry.sdk.metrics.data.DoublePoint;
import java.util.Collection;
import java.util.List;

/**
 * An interface that denotes how we build our API calls from metric data.
 */
public interface MetricTimeSeriesBuilder {
    /** Records a LongPoint of the given metric. */
    void recordPoint(MetricData metric, LongPoint point);
    /** Records a DoublePoint of the given metric. */
    void recordPoint(MetricData metric, DoublePoint point);

    /** The set of descriptors assocaited with the current time series. */
    Collection<MetricDescriptor> getDescriptors();
    /** The set (unique by metric+label) of time series that were built. */
    List<TimeSeries> getTimeSeries();    
}
