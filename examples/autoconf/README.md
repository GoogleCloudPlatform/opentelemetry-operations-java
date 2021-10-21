# Autoconfiguration Example

An example application that sends custom traces+metric using *only* the OpenTelemetry API.  All SDK configuration is done using the autoconfiguration module.

To spin it up on your own GKE cluster, run the following:
```
export GOOGLE_CLOUD_PROJECT={your-project}

./gradlew :examples-autoconf:jib --image="gcr.io/$GOOGLE_CLOUD_PROJECT/hello-autoconfigure-java"

sed s/%GOOGLE_CLOUD_PROJECT%/$GOOGLE_CLOUD_PROJECT/g \
 examples/autoconf/job.yaml | kubectl apply -f -
```

This will run a batch job which synthesizes a nested trace, latency metrics and exemplars.