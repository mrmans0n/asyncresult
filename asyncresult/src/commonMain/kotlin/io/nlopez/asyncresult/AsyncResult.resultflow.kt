// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

/**
 * Scope for the [resultFlow] builder. Provides explicit emission functions for each [AsyncResult]
 * state: [success], [error], [loading], and [notStarted].
 *
 * All functions return [Unit] and can be called multiple times to emit multiple values.
 */
public interface ResultFlowScope<in T> {
  /** Emits a [Success] wrapping [value]. */
  public suspend fun success(value: T)

  /** Emits the given [Error]. */
  public suspend fun error(error: Error)

  /** Emits [Loading]. */
  public suspend fun loading()

  /** Emits [NotStarted]. */
  public suspend fun notStarted()
}

/** Emits an [Error] wrapping the given [throwable] and optional [errorId]. */
public suspend fun <T> ResultFlowScope<T>.error(
    throwable: Throwable,
    errorId: ErrorId? = null,
): Unit = error(Error(throwable, errorId = errorId))

/** Emits an [Error] carrying typed [metadata] and optional [errorId]. */
public suspend fun <T> ResultFlowScope<T>.errorWithMetadata(
    metadata: Any,
    errorId: ErrorId? = null,
): Unit = error(ErrorWithMetadata(metadata, errorId))

/**
 * Creates a [Flow] of [AsyncResult] with a DSL that provides explicit
 * [success][ResultFlowScope.success], [error][ResultFlowScope.error],
 * [loading][ResultFlowScope.loading], and [notStarted][ResultFlowScope.notStarted] emissions.
 *
 * Unlike the [result] DSL which produces a single [AsyncResult], this builder produces a [Flow]
 * that can emit multiple [AsyncResult] values over time.
 *
 * Uncaught exceptions (except [CancellationException]) are caught and emitted as [Error].
 *
 * Example:
 * ```kotlin
 * val userFlow: Flow<AsyncResult<User>> = resultFlow {
 *     loading()
 *     val user = fetchUser(id)
 *     success(user)
 * }
 * ```
 *
 * @param block The builder block that emits [AsyncResult] values via the [ResultFlowScope].
 * @return A [Flow] of [AsyncResult] values.
 */
public fun <T> resultFlow(
    block: suspend ResultFlowScope<T>.() -> Unit,
): Flow<AsyncResult<T>> = flow {
  try {
    ResultFlowScopeImpl(this).block()
  } catch (e: CancellationException) {
    throw e
  } catch (e: Throwable) {
    emit(Error(e))
  }
}

internal class ResultFlowScopeImpl<T>(
    private val collector: FlowCollector<AsyncResult<T>>,
) : ResultFlowScope<T> {
  override suspend fun success(value: T) {
    collector.emit(Success(value))
  }

  override suspend fun error(error: Error) {
    collector.emit(error)
  }

  override suspend fun loading() {
    collector.emit(Loading)
  }

  override suspend fun notStarted() {
    collector.emit(NotStarted)
  }
}
