package com.google.cloud.opentelemetry.trace;

public class MockServerStartupException extends RuntimeException {
    public MockServerStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}