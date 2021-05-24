package com.google.cloud.opentelemetry.endtoend;

import java.util.Map;
import java.util.HashMap;

/** A container for all scenarios we handle.
 * 
 * We "dependency inject" scenarios into the constructor.
 */
public class ScenarioHandlerManager {
    private Map<String, ScenarioHandler> scenarios = new HashMap<>();

    public ScenarioHandlerManager() {
        register("", (request) -> {
            return null;
        });
    }

    private void register(String scenario, ScenarioHandler handler) {
        scenarios.putIfAbsent(scenario, handler);
    }

    public Response handleScenario(String scenario, Request request) {
        return null;
    }
}
