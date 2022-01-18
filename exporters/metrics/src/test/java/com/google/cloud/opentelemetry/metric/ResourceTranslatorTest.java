/*
 * Copyright 2022 Google
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
package com.google.cloud.opentelemetry.metric;

import static org.junit.Assert.assertEquals;

import com.google.api.MonitoredResource;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ResourceTranslatorTest {
  @Test
  public void testMapResourcesWithGCEResource() {
    Map<AttributeKey<String>, String> testAttributes =
        java.util.stream.Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.GCP_COMPUTE_ENGINE
                  },
                  {ResourceAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"},
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.CLOUD_REGION, "country-region"},
                  {ResourceAttributes.HOST_ID, "GCE-instance-id"},
                  {ResourceAttributes.HOST_NAME, "GCE-instance-name"},
                  {ResourceAttributes.HOST_TYPE, "GCE-instance-type"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(
        (key, value) -> {
          attrBuilder.put(key, value);
        });
    Attributes attributes = attrBuilder.build();

    MonitoredResource monitoredResource =
        ResourceTranslator.mapResource(io.opentelemetry.sdk.resources.Resource.create(attributes));

    assertEquals("gce_instance", monitoredResource.getType());

    Map<String, String> monitoredResourceMap = monitoredResource.getLabelsMap();
    assertEquals(2, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"instance_id", "GCE-instance-id"},
                  {"zone", "country-region-zone"},
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithGKEResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.GCP_KUBERNETES_ENGINE
                  },
                  {ResourceAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"},
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.CLOUD_REGION, "country-region"},
                  {ResourceAttributes.HOST_ID, "GCE-instance-id"},
                  {ResourceAttributes.HOST_NAME, "GCE-instance-name"},
                  {ResourceAttributes.HOST_TYPE, "GCE-instance-type"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "GKE-cluster-name"},
                  {ResourceAttributes.K8S_NAMESPACE_NAME, "GKE-testNameSpace"},
                  {ResourceAttributes.K8S_POD_NAME, "GKE-testHostName"},
                  {ResourceAttributes.K8S_CONTAINER_NAME, "GKE-testContainerName"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    MonitoredResource monitoredResource =
        ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_container", monitoredResource.getType());
    Map<String, String> monitoredResourceMap = monitoredResource.getLabelsMap();
    assertEquals(5, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"location", "country-region-zone"},
                  {"cluster_name", "GKE-cluster-name"},
                  {"namespace_name", "GKE-testNameSpace"},
                  {"pod_name", "GKE-testHostName"},
                  {"container_name", "GKE-testContainerName"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithAwsEc2Instance() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EC2
                  },
                  {ResourceAttributes.CLOUD_ACCOUNT_ID, "aws-id"},
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.CLOUD_REGION, "country-region"},
                  {ResourceAttributes.HOST_ID, "aws-instance-id"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    MonitoredResource monitoredResource =
        ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("aws_ec2_instance", monitoredResource.getType());
    Map<String, String> monitoredResourceMap = monitoredResource.getLabelsMap();
    assertEquals(3, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"region", "country-region-zone"},
                  {"instance_id", "aws-instance-id"},
                  {"aws_account", "aws-id"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithGlobal() {
    Map<AttributeKey<String>, String> testAttributes =
        java.util.stream.Stream.of(
                new Object[][] {
                  {ResourceAttributes.SERVICE_NAME, "my-service-name"},
                  {ResourceAttributes.SERVICE_NAMESPACE, "prod"},
                  {ResourceAttributes.SERVICE_INSTANCE_ID, "1234"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(
        (key, value) -> {
          attrBuilder.put(key, value);
        });
    Attributes attributes = attrBuilder.build();

    MonitoredResource monitoredResource =
        ResourceTranslator.mapResource(io.opentelemetry.sdk.resources.Resource.create(attributes));

    assertEquals("generic_task", monitoredResource.getType());

    Map<String, String> monitoredResourceMap = monitoredResource.getLabelsMap();
    assertEquals(4, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"job", "my-service-name"},
                  {"namespace", "prod"},
                  {"task_id", "1234"},
                  {"location", "global"},
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }
}
