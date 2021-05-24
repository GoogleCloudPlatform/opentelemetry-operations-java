package com.google.cloud.opentelemetry.endtoend;


/**
 * A handler for testing scenarios.
 */
public interface ScenarioHandler {
    /** Handles a given tracing scenario request, reporting errors appropriately. */
    public Response handle(Request request);
}
