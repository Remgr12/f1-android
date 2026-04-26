package dev.remgr.f1.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

// Retrofit URL-encodes query param keys, turning "date>" into "date%3E".
// OpenF1 requires the literal ">", so we restore it here.
class OpenF1FilterInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val fixedUrl = original.url.toString()
            .replace("%3E", ">")
            .replace("%3C", "<")
        val request = original.newBuilder().url(fixedUrl).build()
        return chain.proceed(request)
    }
}
