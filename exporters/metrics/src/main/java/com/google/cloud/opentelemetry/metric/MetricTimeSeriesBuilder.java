/*
 * Copyright 2022 Google
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
import com.google.monitoring.v3.TimeSeries;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.util.Collection;
import java.util.List;

/** An interface that denotes how we build our API calls from metric data. */
public interface MetricTimeSeriesBuilder {
  /** Records a LongPoint of the given metric. */
  void recordPoint(MetricData metric, LongPointData point);
  /** Records a DoublePoint of the given metric. */
  void recordPoint(MetricData metric, DoublePointData point);
  /** Records a DoubleHistogramPointData for the given metric. */
  void recordPoint(MetricData metric, HistogramPointData point);

  /** The set of descriptors assocaited with the current time series. */
  Collection<MetricDescriptor> getDescriptors();
  /** The set (unique by metric+label) of time series that were built. */
  List<TimeSeries> getTimeSeries();
}
