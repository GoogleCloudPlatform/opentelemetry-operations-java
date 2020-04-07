package com.google.cloud.operations.opentelemetry.exporter.util;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for Kubernetes deployment service {@code Resource}.
 */
public class K8sResource {
  /**
   * The type of this {@code Resource}.
   */
  public static final String TYPE = "k8s";

  /**
   * Key for the name of the cluster.
   */
  public static final String CLUSTER_NAME_KEY = "k8s.cluster.name";

  /**
   * Key for the name of the namespace.
   */
  public static final String NAMESPACE_NAME_KEY = "k8s.namespace.name";

  /**
   * Key for the name of the pod.
   */
  public static final String POD_NAME_KEY = "k8s.pod.name";

  /**
   * Key for the name of the deployment.
   */
  public static final String DEPLOYMENT_NAME_KEY = "k8s.deployment.name";

  private static final Splitter splitter = Splitter.on('-');

  /**
   * Returns resource labels that describe Kubernetes deployment service.
   *
   * @param clusterName the k8s cluster name.
   * @param namespace the k8s namespace.
   * @param podName the k8s pod name.
   * @param deploymentName the k8s deployment name.
   * @return resource labels that describe a k8s container.
   */
  public static Map<String, String> create(
      String clusterName, String namespace, String podName, String deploymentName) {
    Map<String, String> labels = new LinkedHashMap<String, String>();
    labels.put(CLUSTER_NAME_KEY, checkNotNull(clusterName, "clusterName"));
    labels.put(NAMESPACE_NAME_KEY, checkNotNull(namespace, "namespace"));
    labels.put(POD_NAME_KEY, checkNotNull(podName, "podName"));
    labels.put(DEPLOYMENT_NAME_KEY, checkNotNull(deploymentName, "deploymentName"));
    return labels;
  }

  static Map<String, String> detect() {
    String podName = firstNonNull(System.getenv("HOSTNAME"), "");
    String deploymentName = getDeploymentNameFromPodName(podName);
    return create(
        GcpMetadataConfig.getClusterName(),
        firstNonNull(System.getenv("NAMESPACE"), ""),
        podName,
        deploymentName);
  }

  @VisibleForTesting
  static String getDeploymentNameFromPodName(String podName) {
    StringBuilder deploymentName = new StringBuilder();
    // Extract deployment name from the pod name. Pod name is created using
    // format: [deployment-name]-[Random-String-For-ReplicaSet]-[Random-String-For-Pod]
    List<String> parts = splitter.splitToList(podName);
    if (parts.size() == 3) {
      deploymentName.append(parts.get(0));
    } else if (parts.size() > 3) { // Deployment name could also contain '-'
      for (int i = 0; i < parts.size() - 2; i++) {
        if (deploymentName.length() > 0) {
          deploymentName.append('-');
        }
        deploymentName.append(parts.get(i));
      }
    }
    return deploymentName.toString();
  }

  private K8sResource() {}
}
