package com.google.cloud.opentelemetry.endtoend;

import java.util.Map;

import com.google.protobuf.ByteString;

public interface Request {
    String testId();
    Map<String, String> headers();
    ByteString data();


    static Request make(final String testId, final Map<String, String> headers, final ByteString data) {
        return new Request() {
            public String testId() {
                return testId;
            }
            public Map<String,String> headers() {
                return headers;
            }
            public ByteString data() {
                return data;
            }
        };
    }
}
