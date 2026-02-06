// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:OptIn(ExperimentalContracts::class)

package io.nlopez.asyncresult

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.withTimeoutOrNull

/**
 * It invokes the given [action] **before** each value of the upstream flow is emitted downstream,
 * **IF** the emitted value is [Loading].
 */
public fun <R> Flow<AsyncResult<R>>.onLoading(action: suspend () -> Unit): Flow<AsyncResult<R>> =
    onEach {
      if (it is Loading) action()
    }

/**
 * It invokes the given [action] **before** each value of the upstream flow is emitted downstream,
 * **IF** the emitted value is [Success].
 */
public fun <R> Flow<AsyncResult<R>>.onSuccess(action: suspend (R) -> Unit): Flow<AsyncResult<R>> =
    onEach {
      if (it is Success) action(it.value)
    }

/**
 * It invokes the given [action] **before** each value of the upstream flow is emitted downstream,
 * **IF** the emitted value is [Error].
 */
public fun <R> Flow<AsyncResult<R>>.onError(action: suspend (Error) -> Unit): Flow<AsyncResult<R>> =
    onEach {
      if (it is Error) action(it)
    }

@PublishedApi
internal suspend inline fun <R> Flow<AsyncResult<R>>.firstTerminalResult(): AsyncResult<R> = first {
  it is Success || it is Error
}

/**
 * Obtains the first terminal value of the flow, and if it is a [Success], it returns the
 * encapsulated value. Otherwise, it throws an exception.
 */
public suspend inline fun <R> Flow<AsyncResult<R>>.getOrThrow(): R =
    firstTerminalResult().let { lcr ->
      if (lcr is Success) lcr.value else error("Flow did not emit a Success value: $lcr")
    }

/**
 * Obtains the first terminal value of the flow, and if it is a [Success], it returns the
 * encapsulated value. Otherwise, it returns null.
 */
public suspend inline fun <R> Flow<AsyncResult<R>>.getOrNull(): R? =
    firstTerminalResult().let { lcr -> if (lcr is Success) lcr.value else null }

/**
 * Obtains the first terminal value of the flow, and if it is a [Success], it returns the
 * encapsulated value. Otherwise, it returns the result of the given [transform] function.
 */
public suspend inline fun <R> Flow<AsyncResult<R>>.getOrElse(noinline transform: (Error) -> R): R {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }
  return firstTerminalResult().let { lcr ->
    when (lcr) {
      is Success -> lcr.value
      is Error -> transform(lcr)
      else -> error("Flow did not emit a terminal value")
    }
  }
}

/**
 * Filters out all [Loading] emissions from this flow, only emitting [NotStarted], [Success], or
 * [Error] values.
 *
 * This is useful when you want to ignore intermediate loading states and only react to terminal or
 * idle states.
 *
 * Example:
 * ```kotlin
 * flowOf(Loading, Loading, Success(42))
 *     .skipWhileLoading()
 *     .collect { result ->
 *         // Only receives Success(42)
 *     }
 * ```
 */
public fun <R> Flow<AsyncResult<R>>.skipWhileLoading(): Flow<AsyncResult<R>> = filter {
  it !is Loading
}

/**
 * Alias for [skipWhileLoading]. Filters out all [Loading] emissions from this flow.
 *
 * @see skipWhileLoading
 */
public fun <R> Flow<AsyncResult<R>>.filterNotLoading(): Flow<AsyncResult<R>> = skipWhileLoading()

/**
 * Caches the latest [Success] value and keeps emitting it instead of [Loading] during reloads.
 *
 * This is useful for scenarios where you want to show stale data while refreshing, rather than
 * showing a loading indicator.
 *
 * Behavior:
 * - On first [Success], caches the value and emits it
 * - On subsequent [Loading], keeps emitting the cached [Success] (not [Loading])
 * - On new [Success], updates the cache and emits the new [Success]
 * - [Error] and [NotStarted] are always emitted as-is
 *
 * Example:
 * ```kotlin
 * // Flow: Loading -> Success(1) -> Loading -> Success(2)
 * // Output: Loading -> Success(1) -> Success(1) -> Success(2)
 * dataFlow.cacheLatestSuccess().collect { result ->
 *     // Shows cached data during reloads
 * }
 * ```
 */
public fun <R> Flow<AsyncResult<R>>.cacheLatestSuccess(): Flow<AsyncResult<R>> = flow {
  var cachedSuccess: Success<R>? = null
  collect { result ->
    when (result) {
      is Success -> {
        cachedSuccess = result
        emit(result)
      }
      is Loading -> {
        val cached = cachedSuccess
        if (cached != null) {
          emit(cached)
        } else {
          emit(result)
        }
      }
      else -> emit(result)
    }
  }
}

/**
 * Emits an [Error] if no terminal state ([Success] or [Error]) is received within the specified
 * [timeout] duration.
 *
 * The timeout is measured from the start of collection. If a terminal result is not emitted within
 * the timeout period, an [Error] is emitted using the provided [error] factory.
 *
 * Non-terminal emissions ([Loading], [NotStarted]) are passed through, but do not reset the
 * timeout.
 *
 * Example:
 * ```kotlin
 * slowOperation()
 *     .timeoutToError(5.seconds) { TimeoutException("Operation timed out") }
 *     .collect { result ->
 *         // Either the actual result or an Error with TimeoutException
 *     }
 * ```
 *
 * @param timeout The maximum duration to wait for a terminal result.
 * @param error Factory function to create the error (typically a Throwable) when timeout occurs.
 */
public fun <R> Flow<AsyncResult<R>>.timeoutToError(
    timeout: Duration,
    error: () -> Throwable
): Flow<AsyncResult<R>> = flow {
  var hasTerminal = false
  withTimeoutOrNull(timeout) {
    collect { result ->
      emit(result)
      if (result is Success || result is Error) {
        hasTerminal = true
        return@withTimeoutOrNull
      }
    }
  }
  if (!hasTerminal) {
    emit(Error(error()))
  }
}

/**
 * Retries collecting from the upstream flow when an [Error] is emitted.
 *
 * When an [Error] is encountered that matches the [predicate], the flow waits for [delay] and then
 * restarts collection from the upstream. This continues until either:
 * - A [Success] is emitted
 * - The maximum number of retries ([maxRetries]) is reached
 * - An [Error] that doesn't match the predicate is encountered
 *
 * Note: This operator restarts the upstream flow on retry. For stateful flows, this means the
 * entire flow will be re-executed.
 *
 * Example:
 * ```kotlin
 * fetchData()
 *     .retryOnError(
 *         maxRetries = 3,
 *         delay = 1.seconds,
 *         predicate = { it.throwable is IOException }
 *     )
 *     .collect { result ->
 *         // Automatically retries on IOException up to 3 times
 *     }
 * ```
 *
 * @param maxRetries Maximum number of retry attempts. Default is 3.
 * @param delay Duration to wait before each retry. Default is [Duration.ZERO].
 * @param predicate Function to determine if a retry should occur for a given [Error]. Default
 *   retries on all errors.
 */
public fun <R> Flow<AsyncResult<R>>.retryOnError(
    maxRetries: Int = 3,
    delay: Duration = Duration.ZERO,
    predicate: (Error) -> Boolean = { true }
): Flow<AsyncResult<R>> = flow {
  var retryCount = 0
  var lastError: Error? = null

  val retryableFlow = flow {
    collect { result ->
      when {
        result is Error && retryCount < maxRetries && predicate(result) -> {
          lastError = result
          retryCount++
          if (delay > Duration.ZERO) {
            delay(delay)
          }
          throw RetryTriggerException()
        }
        else -> emit(result)
      }
    }
  }

  try {
    retryableFlow.retry(maxRetries.toLong()) { it is RetryTriggerException }.collect { emit(it) }
  } catch (_: RetryTriggerException) {
    // Max retries exhausted, emit the last error
    lastError?.let { emit(it) }
  }
}

/** Internal exception used to trigger retry in [retryOnError]. */
private class RetryTriggerException : Exception()
