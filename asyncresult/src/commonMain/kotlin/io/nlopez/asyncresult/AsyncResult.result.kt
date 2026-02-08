// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

/**
 * Scope for the [result] DSL block. Provides [bind], [error], [loading], [ensure], and
 * [ensureNotNull] for short-circuit evaluation of [AsyncResult] values.
 */
public interface ResultScope {
  /** Extracts the [Success] value, or short-circuits with the non-success state. */
  public fun <T> AsyncResult<T>.bind(): T

  /** Short-circuits with [Error] wrapping the given [error]. */
  public fun error(error: Throwable): Nothing

  /** Short-circuits with [Loading]. */
  public fun loading(): Nothing

  /** Ensures [condition] is true, or short-circuits with [Error] from [lazyError]. */
  public fun ensure(condition: Boolean, lazyError: () -> Throwable)

  /** Ensures [value] is not null, or short-circuits with [Error] from [lazyError]. */
  public fun <T> ensureNotNull(value: T?, lazyError: () -> Throwable): T
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

  override fun loading(): Nothing {
    shortCircuitResult = Loading
    throw ResultShortCircuit()
  }

  override fun ensure(condition: Boolean, lazyError: () -> Throwable) {
    if (!condition) {
      error(lazyError())
    }
  }

  override fun <T> ensureNotNull(value: T?, lazyError: () -> Throwable): T =
      value ?: error(lazyError())
}

@PublishedApi internal class ResultShortCircuit : Exception()
