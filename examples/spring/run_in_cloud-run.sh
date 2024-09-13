#!/bin/bash
#
# Copyright 2024 Google LLC
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
CONTAINER_REGISTRY=cloud-run-applications
REGISTRY_LOCATION=us-central1

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
set -x

echo "CREATING CLOUD ARTIFACT REPOSITORY"
gcloud artifacts repositories create ${CONTAINER_REGISTRY} --repository-format=docker --location=${REGISTRY_LOCATION} --description="Sample applications to run on Google Cloud Run"
echo "CREATED ${CONTAINER_REGISTRY} in ${REGISTRY_LOCATION}"

echo "BUILDING SAMPLE APP IMAGE"
gradle clean jib --image "${REGISTRY_LOCATION}-docker.pkg.dev/${GOOGLE_CLOUD_PROJECT}/${CONTAINER_REGISTRY}/spring-java-sample"

echo "DEPLOYING APPLICATION ON CLOUD RUN"
gcloud run deploy spring-java-cloud-run \
        --image="${REGISTRY_LOCATION}-docker.pkg.dev/${GOOGLE_CLOUD_PROJECT}/${CONTAINER_REGISTRY}/spring-java-sample" \
        --region="${GOOGLE_CLOUD_RUN_REGION}" \
        --no-allow-unauthenticated \
        --no-cpu-throttling \
        --max-instances=5 \
        --min-instances=3

echo "ENABLING SAMPLE APP ON PORT 8080 VIA PROXY"
echo "VISIT http://localhost:8080 TO ACCESS THE APPLICATION OR PRESS CTRL+C TO EXIT"
gcloud beta run services proxy spring-java-cloud-run --port=8080
