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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ResourceTranslatorTest {

  private static Arguments generateOTelResourceMappingTestArgs(
      Map<AttributeKey<String>, String> otelResourceLabels,
      String expectedMRType,
      Map<String, String> expectedMRLabels) {
    return Arguments.of(otelResourceLabels, expectedMRType, expectedMRLabels);
  }

  private static Stream<Arguments> provideOTelResourceAttributesToMonitoredResourceMapping() {
    return Stream.of(
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.GCP_COMPUTE_ENGINE),
                new SimpleEntry<>(ResourceAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.CLOUD_REGION, "country-region"),
                new SimpleEntry<>(ResourceAttributes.HOST_ID, "GCE-instance-id"),
                new SimpleEntry<>(ResourceAttributes.HOST_NAME, "GCE-instance-name"),
                new SimpleEntry<>(ResourceAttributes.HOST_TYPE, "GCE-instance-type")),
            "gce_instance",
            Map.ofEntries(
                new SimpleEntry<>("instance_id", "GCE-instance-id"),
                new SimpleEntry<>("zone", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.GCP_KUBERNETES_ENGINE),
                new SimpleEntry<>(ResourceAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.CLOUD_REGION, "country-region"),
                new SimpleEntry<>(ResourceAttributes.HOST_ID, "GCE-instance-id"),
                new SimpleEntry<>(ResourceAttributes.HOST_NAME, "GCE-instance-name"),
                new SimpleEntry<>(ResourceAttributes.HOST_TYPE, "GCE-instance-type"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "GKE-cluster-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_NAMESPACE_NAME, "GKE-testNameSpace"),
                new SimpleEntry<>(ResourceAttributes.K8S_POD_NAME, "GKE-testHostName"),
                new SimpleEntry<>(ResourceAttributes.K8S_CONTAINER_NAME, "GKE-testContainerName")),
            "k8s_container",
            Map.ofEntries(
                new SimpleEntry<>("location", "country-region-zone"),
                new SimpleEntry<>("cluster_name", "GKE-cluster-name"),
                new SimpleEntry<>("namespace_name", "GKE-testNameSpace"),
                new SimpleEntry<>("pod_name", "GKE-testHostName"),
                new SimpleEntry<>("container_name", "GKE-testContainerName"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.GCP_KUBERNETES_ENGINE),
                new SimpleEntry<>(ResourceAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.CLOUD_REGION, "country-region"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "GKE-cluster-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_NAMESPACE_NAME, "GKE-testNameSpace"),
                new SimpleEntry<>(ResourceAttributes.K8S_POD_NAME, "GKE-testHostName")),
            "k8s_pod",
            Map.ofEntries(
                new SimpleEntry<>("location", "country-region-zone"),
                new SimpleEntry<>("cluster_name", "GKE-cluster-name"),
                new SimpleEntry<>("namespace_name", "GKE-testNameSpace"),
                new SimpleEntry<>("pod_name", "GKE-testHostName"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.GCP_KUBERNETES_ENGINE),
                new SimpleEntry<>(ResourceAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.CLOUD_REGION, "country-region"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "GKE-cluster-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_NODE_NAME, "GKE-node-name")),
            "k8s_node",
            Map.ofEntries(
                new SimpleEntry<>("location", "country-region-zone"),
                new SimpleEntry<>("node_name", "GKE-node-name"),
                new SimpleEntry<>("cluster_name", "GKE-cluster-name"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.GCP_KUBERNETES_ENGINE),
                new SimpleEntry<>(ResourceAttributes.CLOUD_ACCOUNT_ID, "GCE-pid"),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.CLOUD_REGION, "country-region"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "GKE-cluster-name")),
            "k8s_cluster",
            Map.ofEntries(
                new SimpleEntry<>("location", "country-region-zone"),
                new SimpleEntry<>("cluster_name", "GKE-cluster-name"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.AWS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EKS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "EKS-cluster-name")),
            "k8s_cluster",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "EKS-cluster-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER,
                    ResourceAttributes.CloudProviderValues.AZURE),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AZURE_AKS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "AKS-cluster-name")),
            "k8s_cluster",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "AKS-cluster-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.AWS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EKS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "EKS-cluster-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_NAMESPACE_NAME, "EKS-namespace-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_POD_NAME, "EKS-pod-name")),
            "k8s_pod",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "EKS-cluster-name"),
                new SimpleEntry<>("pod_name", "EKS-pod-name"),
                new SimpleEntry<>("namespace_name", "EKS-namespace-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER,
                    ResourceAttributes.CloudProviderValues.AZURE),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AZURE_AKS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "AKS-cluster-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_NAMESPACE_NAME, "AKS-namespace-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_POD_NAME, "AKS-pod-name")),
            "k8s_pod",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "AKS-cluster-name"),
                new SimpleEntry<>("pod_name", "AKS-pod-name"),
                new SimpleEntry<>("namespace_name", "AKS-namespace-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.AWS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EKS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "EKS-cluster-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_NODE_NAME, "EKS-node-name")),
            "k8s_node",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "EKS-cluster-name"),
                new SimpleEntry<>("node_name", "EKS-node-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER,
                    ResourceAttributes.CloudProviderValues.AZURE),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AZURE_AKS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "AKS-cluster-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_NODE_NAME, "AKS-node-name")),
            "k8s_node",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "AKS-cluster-name"),
                new SimpleEntry<>("node_name", "AKS-node-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.AWS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EKS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "EKS-cluster-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_NAMESPACE_NAME, "EKS-namespace-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_POD_NAME, "EKS-pod-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_CONTAINER_NAME, "EKS-container-name")),
            "k8s_container",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "EKS-cluster-name"),
                new SimpleEntry<>("namespace_name", "EKS-namespace-name"),
                new SimpleEntry<>("pod_name", "EKS-pod-name"),
                new SimpleEntry<>("container_name", "EKS-container-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER,
                    ResourceAttributes.CloudProviderValues.AZURE),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AZURE_AKS),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "AKS-cluster-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_NAMESPACE_NAME, "AKS-namespace-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_POD_NAME, "AKS-pod-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_CONTAINER_NAME, "AKS-container-name")),
            "k8s_container",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "AKS-cluster-name"),
                new SimpleEntry<>("namespace_name", "AKS-namespace-name"),
                new SimpleEntry<>("pod_name", "AKS-pod-name"),
                new SimpleEntry<>("container_name", "AKS-container-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "non-cloud-cluster-name")),
            "k8s_cluster",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "non-cloud-cluster-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "non-cloud-cluster-name"),
                new SimpleEntry<>(
                    ResourceAttributes.K8S_NAMESPACE_NAME, "non-cloud-namespace-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_POD_NAME, "non-cloud-pod-name")),
            "k8s_pod",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "non-cloud-cluster-name"),
                new SimpleEntry<>("pod_name", "non-cloud-pod-name"),
                new SimpleEntry<>("namespace_name", "non-cloud-namespace-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "non-cloud-cluster-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_NODE_NAME, "non-cloud-node-name")),
            "k8s_node",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "non-cloud-cluster-name"),
                new SimpleEntry<>("node_name", "non-cloud-node-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.K8S_CLUSTER_NAME, "non-cloud-cluster-name"),
                new SimpleEntry<>(
                    ResourceAttributes.K8S_NAMESPACE_NAME, "non-cloud-namespace-name"),
                new SimpleEntry<>(ResourceAttributes.K8S_POD_NAME, "non-cloud-pod-name"),
                new SimpleEntry<>(
                    ResourceAttributes.K8S_CONTAINER_NAME, "non-cloud-container-name")),
            "k8s_container",
            Map.ofEntries(
                new SimpleEntry<>("cluster_name", "non-cloud-cluster-name"),
                new SimpleEntry<>("namespace_name", "non-cloud-namespace-name"),
                new SimpleEntry<>("pod_name", "non-cloud-pod-name"),
                new SimpleEntry<>("container_name", "non-cloud-container-name"),
                new SimpleEntry<>("location", "country-region-zone"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.GCP),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_PLATFORM,
                    ResourceAttributes.CloudPlatformValues.AWS_EC2),
                new SimpleEntry<>(ResourceAttributes.CLOUD_ACCOUNT_ID, "aws-id"),
                new SimpleEntry<>(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE, "country-region-zone"),
                new SimpleEntry<>(ResourceAttributes.CLOUD_REGION, "country-region"),
                new SimpleEntry<>(ResourceAttributes.HOST_ID, "aws-instance-id")),
            "aws_ec2_instance",
            Map.ofEntries(
                new SimpleEntry<>("region", "country-region-zone"),
                new SimpleEntry<>("instance_id", "aws-instance-id"),
                new SimpleEntry<>("aws_account", "aws-id"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(ResourceAttributes.SERVICE_NAME, "my-service-prevailed"),
                new SimpleEntry<>(ResourceAttributes.FAAS_NAME, "my-service-ignored"),
                new SimpleEntry<>(ResourceAttributes.SERVICE_NAMESPACE, "prod"),
                new SimpleEntry<>(ResourceAttributes.FAAS_INSTANCE, "1234")),
            "generic_task",
            Map.ofEntries(
                new SimpleEntry<>("job", "my-service-prevailed"),
                new SimpleEntry<>("namespace", "prod"),
                new SimpleEntry<>("task_id", "1234"),
                new SimpleEntry<>("location", "global"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(ResourceAttributes.SERVICE_NAME, "unknown_service_foo"),
                new SimpleEntry<>(ResourceAttributes.FAAS_NAME, "my-service-faas"),
                new SimpleEntry<>(ResourceAttributes.SERVICE_NAMESPACE, "prod"),
                new SimpleEntry<>(ResourceAttributes.FAAS_INSTANCE, "1234")),
            "generic_task",
            Map.ofEntries(
                new SimpleEntry<>("job", "my-service-faas"),
                new SimpleEntry<>("namespace", "prod"),
                new SimpleEntry<>("task_id", "1234"),
                new SimpleEntry<>("location", "global"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(ResourceAttributes.SERVICE_NAME, "unknown_service_foo"),
                new SimpleEntry<>(ResourceAttributes.SERVICE_NAMESPACE, "prod"),
                new SimpleEntry<>(ResourceAttributes.FAAS_INSTANCE, "1234")),
            "generic_task",
            Map.ofEntries(
                new SimpleEntry<>("job", "unknown_service_foo"),
                new SimpleEntry<>("namespace", "prod"),
                new SimpleEntry<>("task_id", "1234"),
                new SimpleEntry<>("location", "global"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(ResourceAttributes.SERVICE_NAME, "my-service-name"),
                new SimpleEntry<>(ResourceAttributes.SERVICE_NAMESPACE, "prod"),
                new SimpleEntry<>(ResourceAttributes.SERVICE_INSTANCE_ID, "1234")),
            "generic_task",
            Map.ofEntries(
                new SimpleEntry<>("job", "my-service-name"),
                new SimpleEntry<>("namespace", "prod"),
                new SimpleEntry<>("task_id", "1234"),
                new SimpleEntry<>("location", "global"))),
        generateOTelResourceMappingTestArgs(
            Map.ofEntries(
                new SimpleEntry<>(ResourceAttributes.SERVICE_NAME, "unknown_service"),
                new SimpleEntry<>(ResourceAttributes.SERVICE_NAMESPACE, "prod")),
            "generic_node",
            Map.ofEntries(
                new SimpleEntry<>("namespace", "prod"),
                new SimpleEntry<>("node_id", ""),
                new SimpleEntry<>("location", "global"))),
        generateOTelResourceMappingTestArgs(
            Collections.emptyMap(),
            "generic_node",
            Map.ofEntries(
                new SimpleEntry<>("namespace", ""),
                new SimpleEntry<>("node_id", ""),
                new SimpleEntry<>("location", "global"))));
  }

  @ParameterizedTest
  @MethodSource("provideOTelResourceAttributesToMonitoredResourceMapping")
  public void testMapResourcesWithGCPResources(
      Map<AttributeKey<String>, String> resourceAttributes,
      String expectedResourceType,
      Map<String, String> expectedResources) {

    AttributesBuilder attrBuilder = Attributes.builder();
    resourceAttributes.forEach(attrBuilder::put);
    Attributes attributes = attrBuilder.build();

    GcpResource monitoredResource = ResourceTranslator.mapResource(Resource.create(attributes));
    assertEquals(expectedResourceType, monitoredResource.getResourceType());

    Map<String, String> monitoredResourceMap = monitoredResource.getResourceLabels().getLabels();
    assertEquals(expectedResources.size(), monitoredResourceMap.size());

    expectedResources.forEach((key, value) -> assertEquals(value, monitoredResourceMap.get(key)));
  }
}
