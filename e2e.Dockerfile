
# Copyright 2021 Google
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Build relative to root of repository i.e. `docker build --file e2e.Dockerfile --tag=$tag ..`
FROM gradle:6.9-jdk11-hotspot as builder

COPY --chown=gradle:gradle . /app/src
WORKDIR /app/src
RUN gradle :e2e-test-server:build

FROM openjdk:11-jre-slim
COPY --from=builder /app/src/e2e-test-server/build/libs/*-all.jar /app/app.jar
WORKDIR /app
CMD java -jar app.jar