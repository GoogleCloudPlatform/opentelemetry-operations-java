/*
 * Copyright 2023 Google LLC
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
package com.google.cloud.opentelemetry.resource;

import static org.junit.Assert.assertEquals;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
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

    GcpResource monitoredResource =
        ResourceTranslator.mapResource(io.opentelemetry.sdk.resources.Resource.create(attributes));

    assertEquals("gce_instance", monitoredResource.getResourceType());

    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
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

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_container", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
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
  public void testMapResourcesWithEksClusterResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "EKS-cluster-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_cluster", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(2, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "EKS-cluster-name"},
                  {"location", "country-region-zone"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithAksClusterResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AZURE_AKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "AKS-cluster-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_cluster", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(2, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "AKS-cluster-name"},
                  {"location", "country-region-zone"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithEksPodResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "EKS-cluster-name"},
                  {ResourceAttributes.K8S_NAMESPACE_NAME, "EKS-namespace-name"},
                  {ResourceAttributes.K8S_POD_NAME, "EKS-pod-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_pod", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(4, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "EKS-cluster-name"},
                  {"pod_name", "EKS-pod-name"},
                  {"namespace_name", "EKS-namespace-name"},
                  {"location", "country-region-zone"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithAksPodResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AZURE_AKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "AKS-cluster-name"},
                  {ResourceAttributes.K8S_NAMESPACE_NAME, "AKS-namespace-name"},
                  {ResourceAttributes.K8S_POD_NAME, "AKS-pod-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_pod", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(4, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "AKS-cluster-name"},
                  {"pod_name", "AKS-pod-name"},
                  {"namespace_name", "AKS-namespace-name"},
                  {"location", "country-region-zone"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithEksNodeResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "EKS-cluster-name"},
                  {ResourceAttributes.K8S_NODE_NAME, "EKS-node-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_node", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(3, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "EKS-cluster-name"},
                  {"node_name", "EKS-node-name"},
                  {"location", "country-region-zone"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithAksNodeResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AZURE_AKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "AKS-cluster-name"},
                  {ResourceAttributes.K8S_NODE_NAME, "AKS-node-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_node", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(3, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "AKS-cluster-name"},
                  {"node_name", "AKS-node-name"},
                  {"location", "country-region-zone"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithEksContainerResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "EKS-cluster-name"},
                  {ResourceAttributes.K8S_NAMESPACE_NAME, "EKS-namespace-name"},
                  {ResourceAttributes.K8S_POD_NAME, "EKS-pod-name"},
                  {ResourceAttributes.K8S_CONTAINER_NAME, "EKS-container-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_container", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(5, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "EKS-cluster-name"},
                  {"namespace_name", "EKS-namespace-name"},
                  {"pod_name", "EKS-pod-name"},
                  {"container_name", "EKS-container-name"},
                  {"location", "country-region-zone"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithAksContainerResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AZURE_AKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "AKS-cluster-name"},
                  {ResourceAttributes.K8S_NAMESPACE_NAME, "AKS-namespace-name"},
                  {ResourceAttributes.K8S_POD_NAME, "AKS-pod-name"},
                  {ResourceAttributes.K8S_CONTAINER_NAME, "AKS-container-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_container", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(5, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "AKS-cluster-name"},
                  {"namespace_name", "AKS-namespace-name"},
                  {"pod_name", "AKS-pod-name"},
                  {"container_name", "AKS-container-name"},
                  {"location", "country-region-zone"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithNonCloudClusterResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "non-cloud-cluster-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_cluster", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(2, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "non-cloud-cluster-name"},
                  {"location", "country-region-zone"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithNonCloudPodResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "non-cloud-cluster-name"},
                  {ResourceAttributes.K8S_NAMESPACE_NAME, "non-cloud-namespace-name"},
                  {ResourceAttributes.K8S_POD_NAME, "non-cloud-pod-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_pod", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(4, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "non-cloud-cluster-name"},
                  {"pod_name", "non-cloud-pod-name"},
                  {"namespace_name", "non-cloud-namespace-name"},
                  {"location", "country-region-zone"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithNonCloudNodeResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AZURE_AKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "non-cloud-cluster-name"},
                  {ResourceAttributes.K8S_NODE_NAME, "non-cloud-node-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_node", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(3, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "non-cloud-cluster-name"},
                  {"node_name", "non-cloud-node-name"},
                  {"location", "country-region-zone"}
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesWithNonCloudContainerResource() {
    Map<AttributeKey<String>, String> testAttributes =
        Stream.of(
                new Object[][] {
                  {ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP},
                  {
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AZURE_AKS
                  },
                  {ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"},
                  {ResourceAttributes.K8S_CLUSTER_NAME, "non-cloud-cluster-name"},
                  {ResourceAttributes.K8S_NAMESPACE_NAME, "non-cloud-namespace-name"},
                  {ResourceAttributes.K8S_POD_NAME, "non-cloud-pod-name"},
                  {ResourceAttributes.K8S_CONTAINER_NAME, "non-cloud-container-name"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("k8s_container", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(5, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"cluster_name", "non-cloud-cluster-name"},
                  {"namespace_name", "non-cloud-namespace-name"},
                  {"pod_name", "non-cloud-pod-name"},
                  {"container_name", "non-cloud-container-name"},
                  {"location", "country-region-zone"}
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

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));

    assertEquals("aws_ec2_instance", monitoredResource.getResourceType());
    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
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
  public void testMapResourcesWithGenericTaskFallback_FAASIgnored() {
    Map<AttributeKey<String>, String> testAttributes =
        java.util.stream.Stream.of(
                new Object[][] {
                  {ResourceAttributes.SERVICE_NAME, "my-service-prevailed"},
                  {ResourceAttributes.FAAS_NAME, "my-service-ignored"},
                  {ResourceAttributes.SERVICE_NAMESPACE, "prod"},
                  {ResourceAttributes.FAAS_INSTANCE, "1234"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource =
        ResourceTranslator.mapResource(io.opentelemetry.sdk.resources.Resource.create(attributes));

    assertEquals("generic_task", monitoredResource.getResourceType());

    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(4, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"job", "my-service-prevailed"},
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

  @Test
  public void testMapResourcesWithGenericTaskFallback_FAASPrevailed() {
    Map<AttributeKey<String>, String> testAttributes =
        java.util.stream.Stream.of(
                new Object[][] {
                  {ResourceAttributes.SERVICE_NAME, "unknown_service_foo"},
                  {ResourceAttributes.FAAS_NAME, "my-service-faas"},
                  {ResourceAttributes.SERVICE_NAMESPACE, "prod"},
                  {ResourceAttributes.FAAS_INSTANCE, "1234"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource =
        ResourceTranslator.mapResource(io.opentelemetry.sdk.resources.Resource.create(attributes));

    assertEquals("generic_task", monitoredResource.getResourceType());

    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(4, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"job", "my-service-faas"},
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

  @Test
  public void testMapResourcesWithGenericTaskFallback_UnknownService() {
    Map<AttributeKey<String>, String> testAttributes =
        java.util.stream.Stream.of(
                new Object[][] {
                  {ResourceAttributes.SERVICE_NAME, "unknown_service_foo"},
                  {ResourceAttributes.SERVICE_NAMESPACE, "prod"},
                  {ResourceAttributes.FAAS_INSTANCE, "1234"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource =
        ResourceTranslator.mapResource(io.opentelemetry.sdk.resources.Resource.create(attributes));

    assertEquals("generic_task", monitoredResource.getResourceType());

    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(4, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"job", "unknown_service_foo"},
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
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource =
        ResourceTranslator.mapResource(io.opentelemetry.sdk.resources.Resource.create(attributes));

    assertEquals("generic_task", monitoredResource.getResourceType());

    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
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

  @Test
  public void testMapResourcesFallbackServiceNameOnly() {
    Map<AttributeKey<String>, String> testAttributes =
        java.util.stream.Stream.of(
                new Object[][] {
                  {ResourceAttributes.SERVICE_NAME, "unknown_service"},
                  {ResourceAttributes.SERVICE_NAMESPACE, "prod"}
                })
            .collect(
                Collectors.toMap(data -> (AttributeKey<String>) data[0], data -> (String) data[1]));
    AttributesBuilder attrBuilder = Attributes.builder();
    testAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource =
        ResourceTranslator.mapResource(io.opentelemetry.sdk.resources.Resource.create(attributes));

    assertEquals("generic_node", monitoredResource.getResourceType());

    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(3, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"namespace", "prod"},
                  {"node_id", ""},
                  {"location", "global"},
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }

  @Test
  public void testMapResourcesFallback() {
    Attributes attributes = Attributes.builder().build();

    GcpResource monitoredResource =
        ResourceTranslator.mapResource(io.opentelemetry.sdk.resources.Resource.create(attributes));

    assertEquals("generic_node", monitoredResource.getResourceType());

    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(3, monitoredResourceMap.size());

    Map<String, String> expectedMappings =
        Stream.of(
                new Object[][] {
                  {"namespace", ""},
                  {"node_id", ""},
                  {"location", "global"},
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));
    expectedMappings.forEach(
        (key, value) -> {
          assertEquals(value, monitoredResourceMap.get(key));
        });
  }
}
