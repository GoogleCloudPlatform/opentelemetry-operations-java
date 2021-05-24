package com.google.cloud.opentelemetry.endtoend;

import com.google.api.gax.rpc.StatusCode.Code;

import com.google.protobuf.ByteString;

public interface Response {
    Code statusCode();
    ByteString data();


    static Response make(final Code code, final ByteString data) {
        return new Response() {
            public Code statusCode() { return code; }
            public ByteString data() { return data; }
        };
    }

    static Response internalError(Throwable t) {
        return make(Code.INTERNAL, ByteString.copyFromUtf8(t.toString()));
    }
    static Response invalidArugment(String message) {
        return make(Code.INVALID_ARGUMENT, ByteString.copyFromUtf8(message));
    }

    public static Response EMPTY = make(Code.UNKNOWN, ByteString.EMPTY);
    
}
