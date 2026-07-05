package com.flashidea.app.ai

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class VivoRequestInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `adds vivo credentials and a unique request id`() {
        val ids = ArrayDeque(listOf("request-one", "request-two"))
        val client = OkHttpClient.Builder()
            .addInterceptor(
                VivoRequestInterceptor(
                    appKey = "test-app-key",
                    appId = "test-app-id"
                ) { ids.removeFirst() }
            )
            .build()

        repeat(2) {
            client.newCall(Request.Builder().url(server.url("/v1/chat/completions")).build())
                .execute()
                .close()
        }

        val first = server.takeRequest()
        val second = server.takeRequest()
        assertEquals("Bearer test-app-key", first.getHeader("Authorization"))
        assertEquals("test-app-id", first.getHeader("X-Vivo-AppId"))
        assertEquals("test-app-id", first.getHeader("X-App-ID"))
        assertEquals("request-one", first.requestUrl?.queryParameter("request_id"))
        assertEquals("request-two", second.requestUrl?.queryParameter("request_id"))
        assertNotEquals(
            first.requestUrl?.queryParameter("request_id"),
            second.requestUrl?.queryParameter("request_id")
        )
    }
}
