// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

/**
 * Scope for the [result] DSL block. Provides [bind], [error], [loading], [notStarted],
 * [errorWithMetadata], [ensure], and [ensureNotNull] for short-circuit evaluation of
 * [AsyncResult] values.
 */
public interface ResultScope {
  /** Extracts the [Success] value, or short-circuits with the non-success state. */
  public fun <T> AsyncResult<T>.bind(): T

  /** Short-circuits with [Error] wrapping the given [error]. */
  public fun error(error: Throwable): Nothing

  /** Short-circuits with the given [Error] instance. */
  public fun error(error: Error): Nothing

  /** Short-circuits with [Loading]. */
  public fun loading(): Nothing

  /** Short-circuits with [NotStarted]. */
  public fun notStarted(): Nothing

  /** Short-circuits with an [Error] carrying typed [metadata]. */
  public fun <T> errorWithMetadata(metadata: T, errorId: ErrorId? = null): Nothing

  /** Ensures [condition] is true, or short-circuits with [Error] from [lazyError]. */
  public fun ensure(condition: Boolean, lazyError: () -> Throwable)

  /** Ensures [condition] is true, or short-circuits with [Error] from [lazyError]. */
  public fun ensure(condition: Boolean, lazyError: () -> Error)

  /** Ensures [value] is not null, or short-circuits with [Error] from [lazyError]. */
  public fun <T> ensureNotNull(value: T?, lazyError: () -> Throwable): T

  /** Ensures [value] is not null, or short-circuits with [Error] from [lazyError]. */
  public fun <T> ensureNotNull(value: T?, lazyError: () -> Error): T
}

/**
 * Monad comprehension DSL for [AsyncResult].
 *
 * Allows sequential, imperative-style composition of [AsyncResult] values using [ResultScope.bind].
 * Short-circuits on the first non-[Success] state (Error, Loading, or NotStarted).
 *
 * Works in both suspend and non-suspend contexts thanks to being an `inline` function.
 *
 * ```
 * val result = result {
 *     val user = fetchUser(id).bind()
 *     val posts = fetchPosts(user.id).bind()
 *     UserWithPosts(user, posts)
 * }
 * ```
 */
public inline fun <T> result(block: ResultScope.() -> T): AsyncResult<T> {
  val scope = ResultScopeImpl()
  return try {
    Success(scope.block())
  } catch (_: ResultShortCircuit) {
    scope.shortCircuitResult!!
  }
}

@PublishedApi
internal class ResultScopeImpl : ResultScope {
  @PublishedApi internal var shortCircuitResult: AsyncResult<Nothing>? = null

  override fun <T> AsyncResult<T>.bind(): T =
      when (this) {
        is Success -> value
        is Error -> {
          shortCircuitResult = this
          throw ResultShortCircuit()
        }
        is Loading -> {
          shortCircuitResult = Loading
          throw ResultShortCircuit()
        }
        is NotStarted -> {
          shortCircuitResult = NotStarted
          throw ResultShortCircuit()
        }
      }

  override fun error(error: Throwable): Nothing {
    shortCircuitResult = Error(error)
    throw ResultShortCircuit()
  }

  override fun error(error: Error): Nothing {
    shortCircuitResult = error
    throw ResultShortCircuit()
  }

  override fun loading(): Nothing {
    shortCircuitResult = Loading
    throw ResultShortCircuit()
  }

  override fun notStarted(): Nothing {
    shortCircuitResult = NotStarted
    throw ResultShortCircuit()
  }

  override fun <T> errorWithMetadata(metadata: T, errorId: ErrorId?): Nothing {
    shortCircuitResult = ErrorWithMetadata(metadata as Any, errorId)
    throw ResultShortCircuit()
  }

  override fun ensure(condition: Boolean, lazyError: () -> Throwable) {
    if (!condition) {
      error(lazyError())
    }
  }

  override fun ensure(condition: Boolean, lazyError: () -> Error) {
    if (!condition) {
      shortCircuitResult = lazyError()
      throw ResultShortCircuit()
    }
  }

  override fun <T> ensureNotNull(value: T?, lazyError: () -> Throwable): T =
      value ?: error(lazyError())

  override fun <T> ensureNotNull(value: T?, lazyError: () -> Error): T {
    if (value == null) {
      shortCircuitResult = lazyError()
      throw ResultShortCircuit()
    }
    return value
  }
}

@PublishedApi internal class ResultShortCircuit : Exception()
