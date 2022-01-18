# Resource detection Example

An example application that shows what resource attributes will be detected.

To spin it up on your own GKE cluster, run the following:
```
export GOOGLE_CLOUD_PROJECT={your-project}

./gradlew :examples-resource:jib --image="gcr.io/$GOOGLE_CLOUD_PROJECT/hello-resource-java"

sed s/%GOOGLE_CLOUD_PROJECT%/$GOOGLE_CLOUD_PROJECT/g \
 examples/resource/job.yaml | kubectl apply -f -
```