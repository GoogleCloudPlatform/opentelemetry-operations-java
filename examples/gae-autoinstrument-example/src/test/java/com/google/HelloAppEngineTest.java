package com.google;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Unit tests for {@link HelloAppEngine}.
 * These are the default unit tests that come with the auto-generated project.
 * Skip running tests if using Opentelemetry Manual instrumentation with auto-configure module.
 */

@RunWith(JUnit4.class)
public class HelloAppEngineTest {
  private static final String FAKE_URL = "fake.fk/hello";
  // Set up a helper so that the ApiProxy returns a valid environment for local testing.
  private final LocalServiceTestHelper helper = new LocalServiceTestHelper();

  @Mock private HttpServletRequest mockRequest;
  @Mock private HttpServletResponse mockResponse;
  private StringWriter responseWriter;
  private HelloAppEngine servletUnderTest;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    helper.setUp();

    //  Set up some fake HTTP requests
    when(mockRequest.getRequestURI()).thenReturn(FAKE_URL);

    // Set up a fake HTTP response.
    responseWriter = new StringWriter();
    when(mockResponse.getWriter()).thenReturn(new PrintWriter(responseWriter));

    servletUnderTest = new HelloAppEngine();
  }

  @After public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void doGet_writesResponse() throws Exception {
    servletUnderTest.doGet(mockRequest, mockResponse);

    // We expect our hello world response.
    assertThat(responseWriter.toString())
        .named("HelloAppEngine response")
        .contains("Hello App Engine - Standard ");
  }

  @Test
  public void HelloInfo_test() {
    String result = HelloAppEngine.getInfo();
    assertThat(result)
      .named("HelloAppEngine.getInfo")
      .containsMatch("^Version:\\s+.+OS:\\s+.+User:\\s");
  }
}
