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
package com.google.cloud.opentelemetry.examples.otlpmetricsfunction;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.Random;

public class HelloWorld implements HttpFunction {
  private static final OpenTelemetryConfig openTelemetryConfig = OpenTelemetryConfig.getInstance();
  private static final LongCounter counter =
      openTelemetryConfig
          .getMeterProvider()
          .get("sample-function-library")
          .counterBuilder("function_counter_psx")
          .setDescription("random counter")
          .build();
  private static final Random random = new Random();

  public HelloWorld() {
    super();
    // Register a shutdown hook as soon as the function object is instantiated
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.out.println("Closing OpenTelemetry SDK");
                  openTelemetryConfig.closeSdk();
                  System.out.println("OpenTelemetry SDK closed");
                }));
  }

  @Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {
    System.out.println("received request: " + request.toString());
    counter.add(random.nextInt(100));
    response.getWriter().write("Hello, World\n");
    System.out.println("Function exited");
  }
}
