// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

/**
 * Converts a [Success] to an [Error] if the [predicate] returns true for the success value. Other
 * states ([Loading], [NotStarted], [Error]) are returned unchanged.
 *
 * @param predicate Function that receives the success value and returns true if it should become an
 *   error.
 * @param error Optional function to create a custom [Error] from the success value. Defaults to
 *   [Error.Empty].
 * @return The original [AsyncResult] if predicate is false or not [Success], otherwise an [Error].
 */
public inline fun <T> AsyncResult<T>.toErrorIf(
    predicate: (T) -> Boolean,
    error: (T) -> Error = { Error.Empty },
): AsyncResult<T> =
    when (this) {
      is Success -> if (predicate(value)) error(value) else this
      else -> this
    }

/**
 * Converts a [Success] to an [Error] if the [predicate] returns false for the success value. This
 * is the inverse of [toErrorIf]. Other states ([Loading], [NotStarted], [Error]) are returned
 * unchanged.
 *
 * @param predicate Function that receives the success value and returns true to keep Success.
 * @param error Optional function to create a custom [Error] from the success value. Defaults to
 *   [Error.Empty].
 * @return The original [AsyncResult] if predicate is true or not [Success], otherwise an [Error].
 */
public inline fun <T> AsyncResult<T>.toErrorUnless(
    predicate: (T) -> Boolean,
    error: (T) -> Error = { Error.Empty },
): AsyncResult<T> =
    when (this) {
      is Success -> if (predicate(value)) this else error(value)
      else -> this
    }
