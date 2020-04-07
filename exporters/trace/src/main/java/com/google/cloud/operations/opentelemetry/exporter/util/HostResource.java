package com.google.cloud.operations.opentelemetry.exporter.util;

import static com.google.cloud.operations.opentelemetry.exporter.util.ResourceUtils.EMPTY_RESOURCE_LABELS;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper class for Host {@code Resource}. A host is defined as a general computing instance.
 */
public final class HostResource {
  /**
   * The type of this {@code Resource}.
   */
  public static final String TYPE = "host";

  /**
   * Key for the hostname of the host.
   *
   * <p>It contains what the `hostname` command returns on the host machine.
   */
  public static final String HOSTNAME_KEY = "host.hostname";

  /**
   * Key for the name of the host.
   *
   * <p>It may contain what `hostname` returns on Unix systems, the fully qualified, or a name
   * specified by the user.
   */
  public static final String NAME_KEY = "host.name";

  /**
   * Key for the unique host id (instance id in Cloud).
   */
  public static final String ID_KEY = "host.id";

  /**
   * Key for the type of the host (machine type).
   */
  public static final String TYPE_KEY = "host.type";

  /**
   * Returns resource labels that describe a k8s container.
   *
   * @param hostname the hostname of the host.
   * @param name the name of the host.
   * @param id the unique host id (instance id in Cloud).
   * @param type the type of the host (machine type).
   * @return resource labels that describe a k8s container.
   */
  public static Map<String, String> create(String hostname, String name, String id, String type) {
    Map<String, String> labels = new LinkedHashMap<String, String>();
    labels.put(HOSTNAME_KEY, checkNotNull(hostname, "hostname"));
    labels.put(NAME_KEY, checkNotNull(name, "name"));
    labels.put(ID_KEY, checkNotNull(id, "id"));
    labels.put(TYPE_KEY, checkNotNull(type, "type"));
    return labels;
  }

  static Map<String, String> detect() {
    if (AwsIdentityDocUtils.isRunningOnAws()) {
      return create(
          "", "", AwsIdentityDocUtils.getInstanceId(), AwsIdentityDocUtils.getMachineType());
    }
    if (GcpMetadataConfig.isRunningOnGcp()) {
      return create(
          GcpMetadataConfig.getInstanceHostname(),
          GcpMetadataConfig.getInstanceName(),
          GcpMetadataConfig.getInstanceId(),
          GcpMetadataConfig.getMachineType());
    }
    return EMPTY_RESOURCE_LABELS;
  }

  private HostResource() {}
}
