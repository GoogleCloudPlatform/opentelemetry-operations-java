package com.google.cloud.operations.opentelemetry.exporter.util;

import static com.google.cloud.operations.opentelemetry.exporter.util.ResourceUtils.EMPTY_RESOURCE_LABELS;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper class for Cloud {@code Resource} environment.
 */
public final class CloudResource {
  /**
   * The type of this {@code Resource}.
   */
  public static final String TYPE = "cloud";

  /**
   * Key for the name of the cloud provider. Example values are aws, azure, gcp.
   */
  public static final String PROVIDER_KEY = "cloud.provider";

  /**
   * The value of the provider when running in AWS.
   */
  public static final String PROVIDER_AWS = "aws";

  /**
   * The value of the provider when running in AZURE.
   */
  public static final String PROVIDER_AZURE = "azure";

  /**
   * The value of the provider when running in GCP.
   */
  public static final String PROVIDER_GCP = "gcp";

  /**
   * Key for the cloud account id used to identify different entities.
   */
  public static final String ACCOUNT_ID_KEY = "cloud.account.id";

  /**
   * Key for the region in which entities are running.
   */
  public static final String REGION_KEY = "cloud.region";

  /**
   * Key for the zone in which entities are running.
   */
  public static final String ZONE_KEY = "cloud.zone";

  /**
   * Returns resource labels that describe a cloud environment.
   *
   * @param provider the name of the cloud provider.
   * @param accountId the cloud account id used to identify different entities.
   * @param region the region in which entities are running.
   * @param zone the zone in which entities are running.
   * @return resource labels that describe a aws ec2 instance.
   */
  public static Map<String, String> create(String provider, String accountId, String region, String zone) {
    Map<String, String> labels = new LinkedHashMap<String, String>();
    labels.put(PROVIDER_KEY, checkNotNull(provider, "provider"));
    labels.put(ACCOUNT_ID_KEY, checkNotNull(accountId, "accountId"));
    labels.put(REGION_KEY, checkNotNull(region, "availabilityZone"));
    labels.put(ZONE_KEY, checkNotNull(zone, "zone"));
    return labels;
  }

  static Map<String, String> detect() {
    if (AwsIdentityDocUtils.isRunningOnAws()) {
      return create(
          PROVIDER_AWS,
          AwsIdentityDocUtils.getAccountId(),
          AwsIdentityDocUtils.getRegion(),
          AwsIdentityDocUtils.getAvailabilityZone());
    }
    if (GcpMetadataConfig.isRunningOnGcp()) {
      return create(
          PROVIDER_GCP, GcpMetadataConfig.getProjectId(), "", GcpMetadataConfig.getZone());
    }
    // TODO: Add support for PROVIDER_AZURE.
    return EMPTY_RESOURCE_LABELS;
  }

  private CloudResource() {}
}
