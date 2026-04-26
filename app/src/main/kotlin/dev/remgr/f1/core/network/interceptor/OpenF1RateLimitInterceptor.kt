package dev.remgr.f1.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class OpenF1RateLimitInterceptor @Inject constructor() : Interceptor {

    private val lock = Any()
    private var lastRequestAtMs: Long = 0L

    private val minRequestIntervalMs = 1200L
    private val maxRetries = 2

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0

        while (true) {
            enforceMinGap()

            val response = chain.proceed(chain.request())
            if (response.code != 429 || attempt >= maxRetries) {
                return response
            }

            val retryAfterMs = response.header("Retry-After")
                ?.toLongOrNull()
                ?.coerceAtLeast(1L)
                ?.times(1000L)
            response.close()

            val backoffMs = (1000L shl attempt).coerceAtMost(8_000L)
            Thread.sleep(retryAfterMs ?: backoffMs)
            attempt++
        }
    }

    private fun enforceMinGap() {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestAtMs
            if (elapsed < minRequestIntervalMs) {
                Thread.sleep(min(minRequestIntervalMs - elapsed, minRequestIntervalMs))
            }
            lastRequestAtMs = System.currentTimeMillis()
        }
    }
}
