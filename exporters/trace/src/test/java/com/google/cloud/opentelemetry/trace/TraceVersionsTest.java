/*
 * Copyright 2023 Google
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
package com.google.cloud.opentelemetry.trace;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraceVersionsTest {

  @Test
  public void findsExporterVersion() {
    assertNotNull(TraceVersions.EXPORTER_VERSION);
    assertNotEquals("unknown", TraceVersions.EXPORTER_VERSION);
  }

  @Test
  public void findsSdkVersion() {
    assertNotNull(TraceVersions.SDK_VERSION);
    assertNotEquals("unknown", TraceVersions.SDK_VERSION);
  }
}
