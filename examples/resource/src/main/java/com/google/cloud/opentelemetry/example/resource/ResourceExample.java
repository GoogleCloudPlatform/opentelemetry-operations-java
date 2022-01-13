package com.google.cloud.opentelemetry.example.resource;

import com.google.cloud.opentelemetry.detectors.GKEResource;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

public class ResourceExample {
    public static void main(String[] args) {
        System.out.println("Detecting resource: Autoconfigure");
        io.opentelemetry.sdk.resources.Resource autoResource =
                AutoConfiguredOpenTelemetrySdk.builder().setResultAsGlobal(false).setServiceClassLoader(
                ResourceExample.class.getClassLoader()
        ).build().getResource();
        System.out.println(autoResource.getAttributes());
        System.out.println("Detecting resource: hardcoded");
        GKEResource resource = new GKEResource();
        System.out.println(resource.getAttributes());
    }
}
