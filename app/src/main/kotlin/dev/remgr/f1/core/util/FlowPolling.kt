package dev.remgr.f1.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun <T> pollFlow(
    interval: Duration = 1.seconds,
    fetch: suspend () -> T,
): Flow<T> = flow {
    while (true) {
        emit(fetch())
        delay(interval)
    }
}.flowOn(Dispatchers.IO)
