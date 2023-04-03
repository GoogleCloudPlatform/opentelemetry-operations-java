#!/bin/bash
echo "BUILDING SAMPLE APP"
gradle clean build

if [[ -z "${GOOGLE_CLOUD_PROJECT}" ]]; then
  echo "GOOGLE_CLOUD_PROJECT environment variable not set"
  exit 1
fi

if [[ -z "${GOOGLE_APPLICATION_CREDENTIALS}" ]]; then
  echo "GOOGLE_APPLICATION_CREDENTIALS environment variable not set"
  exit 1
fi

if [[ -z "${GOOGLE_AUTO_EXPORTER_PATH}" ]]; then
  echo "GOOGLE_AUTO_EXPORTER_PATH environment variable not set"
  exit 1
fi
echo "ENVIRONMENT VARIABLES VERIFIED"

echo "BUILDING DOCKER IMAGE"
docker build --build-arg project_id=$GOOGLE_CLOUD_PROJECT --build-arg credentials_location=$GOOGLE_APPLICATION_CREDENTIALS -t examples/autoinstrument-local .

echo "RUNNING SAMPLE APP ON PORT 8080"
docker run \
      --rm \
      -v "${GOOGLE_APPLICATION_CREDENTIALS}:${GOOGLE_APPLICATION_CREDENTIALS}:ro" \
      -v "${GOOGLE_AUTO_EXPORTER_PATH}:/exporter-auto.jar" \
      -p 8080:8080 \
      examples/autoinstrument-local
