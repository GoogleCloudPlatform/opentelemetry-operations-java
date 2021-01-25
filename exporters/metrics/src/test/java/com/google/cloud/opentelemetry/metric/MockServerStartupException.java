package com.google.cloud.opentelemetry.metric;

public class MockServerStartupException extends RuntimeException {
    public MockServerStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}