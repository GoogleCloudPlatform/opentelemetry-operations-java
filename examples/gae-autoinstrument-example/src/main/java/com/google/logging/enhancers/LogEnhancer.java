package com.google.logging.enhancers;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.LoggingEnhancer;

public class LogEnhancer implements LoggingEnhancer {
  @Override
  public void enhanceLogEntry(LogEntry.Builder builder) {
    builder.addLabel("LoggingLibrary", "JUL");
  }
}
