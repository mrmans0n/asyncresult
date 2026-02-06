// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Converts a [Success] to an [Error] if the [predicate] returns true.
 *
 * If this is [Loading], [NotStarted], or already an [Error], returns this unchanged.
 * If this is [Success] and [predicate] returns true, converts to [Error] using [error] function.
 * If this is [Success] and [predicate] returns false, returns this unchanged.
 *
 * Example:
 * ```kotlin
 * val result: AsyncResult<Int> = Success(5)
 * val validated = result.toErrorIf(
 *     error = { ErrorWithMetadata("Value too small") },
 *     predicate = { it < 10 }
 * ) // Error with metadata "Value too small"
 * ```
 *
 * @param error Function to generate an [Error] from the success value (default creates empty Error)
 * @param predicate Function to test the success value
 * @return This [AsyncResult] converted to error if predicate is true, unchanged otherwise
 */
public inline fun <R> AsyncResult<R>.toErrorIf(
    noinline error: (R) -> Error = { Error() },
    predicate: (R) -> Boolean
): AsyncResult<R> {
  contract {
    callsInPlace(predicate, InvocationKind.AT_MOST_ONCE)
  }

  return when (this) {
    is Success -> {
      if (predicate(value)) {
        error(value)
      } else {
        this
      }
    }

    is Error -> this
    Loading -> Loading
    NotStarted -> NotStarted
  }
}

/**
 * Converts a [Success] to an [Error] unless the [predicate] returns true.
 *
 * If this is [Loading], [NotStarted], or already an [Error], returns this unchanged.
 * If this is [Success] and [predicate] returns false, converts to [Error] using [error] function.
 * If this is [Success] and [predicate] returns true, returns this unchanged.
 *
 * This is the inverse of [toErrorIf].
 *
 * Example:
 * ```kotlin
 * val result: AsyncResult<Int> = Success(15)
 * val validated = result.toErrorUnless(
 *     error = { ErrorWithMetadata("Value too small") },
 *     predicate = { it >= 10 }
 * ) // Success(15) - predicate is true
 *
 * val invalid: AsyncResult<Int> = Success(5)
 * val validated = invalid.toErrorUnless(
 *     error = { ErrorWithMetadata("Value too small") },
 *     predicate = { it >= 10 }
 * ) // Error with metadata "Value too small" - predicate is false
 * ```
 *
 * @param error Function to generate an [Error] from the success value (default creates empty Error)
 * @param predicate Function to test the success value
 * @return This [AsyncResult] converted to error if predicate is false, unchanged otherwise
 */
public inline fun <R> AsyncResult<R>.toErrorUnless(
    noinline error: (R) -> Error = { Error() },
    predicate: (R) -> Boolean
): AsyncResult<R> {
  contract {
    callsInPlace(predicate, InvocationKind.AT_MOST_ONCE)
  }

  return when (this) {
    is Success -> {
      if (!predicate(value)) {
        error(value)
      } else {
        this
      }
    }

    is Error -> this
    Loading -> Loading
    NotStarted -> NotStarted
  }
}
