// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Returns [other] if this is an [Error], otherwise returns this unchanged.
 *
 * The [other] result is evaluated eagerly regardless of whether this is an error.
 * For lazy evaluation, use [orElse].
 *
 * Example:
 * ```kotlin
 * val primary: AsyncResult<Int> = Error(Exception("Failed"))
 * val fallback: AsyncResult<Int> = Success(42)
 * val result = primary.or(fallback) // Success(42)
 * ```
 *
 * @param other The fallback [AsyncResult] to use if this is an error
 * @return This [AsyncResult] if successful, [other] if this is an error
 */
public inline fun <R> AsyncResult<R>.or(other: AsyncResult<R>): AsyncResult<R> =
    when (this) {
      is Error -> other
      is Success -> this
      Loading -> this
      NotStarted -> this
    }

/**
 * Returns the result of [transform] if this is an [Error], otherwise returns this unchanged.
 *
 * The [transform] function is only called if this is an error (lazy evaluation).
 * For eager evaluation, use [or].
 *
 * Example:
 * ```kotlin
 * val result: AsyncResult<Int> = Error(Exception("Failed"))
 * val recovered = result.orElse { error ->
 *     if (error.throwable is NetworkException) {
 *         fetchFromCache()
 *     } else {
 *         Success(0)
 *     }
 * }
 * ```
 *
 * @param transform Function to generate a fallback [AsyncResult] from the error
 * @return This [AsyncResult] if successful, the result of [transform] if this is an error
 */
public inline fun <R> AsyncResult<R>.orElse(
    transform: (Error) -> AsyncResult<R>
): AsyncResult<R> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }

  return when (this) {
    is Error -> transform(this)
    is Success -> this
    Loading -> this
    NotStarted -> this
  }
}
