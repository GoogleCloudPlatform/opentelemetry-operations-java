#!/bin/bash
#
# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
CONTAINER_REGISTRY=opentelemetry-sample-apps
REGISTRY_LOCATION=us-central1
IMAGE_NAME="${REGISTRY_LOCATION}-docker.pkg.dev/${GOOGLE_CLOUD_PROJECT}/${CONTAINER_REGISTRY}/hello-autoinstrument-java"

if [[ -z "${GOOGLE_CLOUD_PROJECT}" ]]; then
  echo "GOOGLE_CLOUD_PROJECT environment variable not set"
  exit 1
fi

if [[ -z "${GOOGLE_APPLICATION_CREDENTIALS}" ]]; then
  echo "GOOGLE_APPLICATION_CREDENTIALS environment variable not set"
  exit 1
fi

if [[ -z "${GOOGLE_CLOUD_RUN_REGION}" ]]; then
  echo "GOOGLE_CLOUD_RUN_REGION environment variable not set"
  exit 1
fi

echo "ENVIRONMENT VARIABLES VERIFIED"

echo "CREATING CLOUD ARTIFACT REPOSITORY"
gcloud artifacts repositories create ${CONTAINER_REGISTRY} --repository-format=docker --location=${REGISTRY_LOCATION} --description="OpenTelemetry auto-instrumentation sample applications"
echo "CREATED ${CONTAINER_REGISTRY} in ${REGISTRY_LOCATION}"

echo "BUILDING SAMPLE APP IMAGE"
gradle clean jib --image "${IMAGE_NAME}"

echo "RUNNING SAMPLE APP ON PORT 8080"
gcloud run deploy hello-autoinstrument-cloud-run \
        --set-env-vars="GOOGLE_CLOUD_PROJECT=${GOOGLE_CLOUD_PROJECT}" \
        --image="${IMAGE_NAME}" \
        --region="${GOOGLE_CLOUD_RUN_REGION}"
