package com.google.cloud.opentelemetry.detectors;

/**
 * Provides API to fetch environment variables. This is useful in order to create a mock class for
 * testing.
 */
interface EnvironmentVariables {
  /** Returns the current environment variables of the platform this is running in. */
  EnvironmentVariables DEFAULT_INSTANCE = System::getenv;

  /**
   * Grabs the system environment variable. Returns null on failure.
   *
   * @param key the key of the environment variable in {@code System.getenv()}
   * @return the value received by {@code System.getenv(key)}
   */
  String get(String key);
}
