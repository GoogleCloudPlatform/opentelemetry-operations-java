/*
 * Copyright 2021 Google
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
package com.google.cloud.opentelemetry.endtoend;

import java.util.HashMap;
import java.util.Map;

/**
 * A container for all scenarios we handle.
 *
 * <p>We "dependency inject" scenarios into the constructor.
 */
public class ScenarioHandlerManager {
  private Map<String, ScenarioHandler> scenarios = new HashMap<>();

  public ScenarioHandlerManager() {
    register("health", this::health);
  }

  /** Health check test. */
  private Response health(Request request) {
      return Response.ok("");
  }

  private Response unimplemented(Request request) {
      return Response.invalidArugment("Unimplemented.");
  }


  private void register(String scenario, ScenarioHandler handler) {
    scenarios.putIfAbsent(scenario, handler);
  }

  public Response handleScenario(String scenario, Request request) {
    ScenarioHandler handler = scenarios.getOrDefault(scenario, this::unimplemented);
    return handler.handle(request);
  }
}
