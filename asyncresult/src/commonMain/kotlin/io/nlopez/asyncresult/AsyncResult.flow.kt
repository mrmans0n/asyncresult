@file:OptIn(ExperimentalContracts::class)

package io.nlopez.asyncresult

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach

/**
 * It invokes the given [action] **before** each value of the upstream flow is emitted downstream,
 * **IF** the emitted value is [Loading].
 */
fun <R> Flow<AsyncResult<R>>.onLoading(action: suspend () -> Unit): Flow<AsyncResult<R>> =
    onEach { if (it is Loading) action() }

/**
 * It invokes the given [action] **before** each value of the upstream flow is emitted downstream,
 * **IF** the emitted value is [Success].
 */
fun <R> Flow<AsyncResult<R>>.onSuccess(action: suspend (R) -> Unit): Flow<AsyncResult<R>> =
    onEach { if (it is Success) action(it.value) }

/**
 * It invokes the given [action] **before** each value of the upstream flow is emitted downstream,
 * **IF** the emitted value is [Error].
 */
fun <R> Flow<AsyncResult<R>>.onError(action: suspend (Error) -> Unit): Flow<AsyncResult<R>> =
    onEach { if (it is Error) action(it) }

@PublishedApi
internal suspend inline fun <R> Flow<AsyncResult<R>>.firstTerminalResult(): AsyncResult<R> =
    first { it is Success || it is Error }

/**
 * Obtains the first terminal value of the flow, and if it is a [Success], it returns the
 * encapsulated value. Otherwise, it throws an exception.
 */
suspend inline fun <R> Flow<AsyncResult<R>>.getOrThrow(): R = firstTerminalResult().let { lcr ->
    if (lcr is Success) lcr.value else error("Flow did not emit a Success value: $lcr")
}

/**
 * Obtains the first terminal value of the flow, and if it is a [Success], it returns the
 * encapsulated value. Otherwise, it returns null.
 */
suspend inline fun <R> Flow<AsyncResult<R>>.getOrNull(): R? = firstTerminalResult().let { lcr ->
    if (lcr is Success) lcr.value else null
}

/**
 * Obtains the first terminal value of the flow, and if it is a [Success], it returns the
 * encapsulated value. Otherwise, it returns the result of the given [transform] function.
 */
suspend inline fun <R> Flow<AsyncResult<R>>.getOrElse(noinline transform: (Error) -> R): R {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return firstTerminalResult().let { lcr ->
        when (lcr) {
            is Success -> lcr.value
            is Error -> transform(lcr)
            else -> error("Flow did not emit a terminal value")
        }
    }
}
