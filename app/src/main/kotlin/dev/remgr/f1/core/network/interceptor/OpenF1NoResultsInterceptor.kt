package dev.remgr.f1.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenF1NoResultsInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code != 404) return response

        val request = response.request
        if (request.url.host != "f1api.remgr.dev") return response

        val bodyString = response.body?.string().orEmpty()
        val isNoResults = bodyString.contains("No results found", ignoreCase = true)
        if (!isNoResults) {
            val originalContentType = response.body?.contentType()
            response.close()
            return response.newBuilder()
                .body(bodyString.toResponseBody(originalContentType))
                .build()
        }

        response.close()
        return Response.Builder()
            .request(request)
            .protocol(response.protocol)
            .code(200)
            .message("OK")
            .body("[]".toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()
    }
}
