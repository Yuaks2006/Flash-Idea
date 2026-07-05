package com.flashidea.app.ai

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class VivoRequestInterceptor(
    private val appKey: String,
    private val appId: String = "",
    private val requestIdFactory: () -> String = { UUID.randomUUID().toString() }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .url(
                original.url.newBuilder()
                    .setQueryParameter(REQUEST_ID_PARAMETER, requestIdFactory())
                    .build()
            )
            .header("Authorization", "Bearer $appKey")
            .header("Content-Type", "application/json; charset=utf-8")
        if (appId.isNotBlank()) {
            requestBuilder
                .header("X-Vivo-AppId", appId)
                .header("X-App-ID", appId)
        }
        val request = requestBuilder.build()
        return chain.proceed(request)
    }

    private companion object {
        const val REQUEST_ID_PARAMETER = "request_id"
    }
}
