// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Converts an [Error] to a [Success] by applying [transform] to the error.
 *
 * If this is a [Success], [Loading], or [NotStarted], returns this unchanged.
 * If this is an [Error], applies [transform] and returns [Success] with the result.
 *
 * Example:
 * ```kotlin
 * val result: AsyncResult<Int> = Error(Exception("Failed"))
 * val recovered = result.recover { 0 } // Success(0)
 * ```
 *
 * @param transform Function to convert an [Error] to a success value
 * @return [AsyncResult] with the error recovered to success, or unchanged if already successful
 */
public inline fun <R> AsyncResult<R>.recover(transform: (Error) -> R): AsyncResult<R> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }

  return when (this) {
    is Error -> Success(transform(this))
    is Success -> this
    Loading -> Loading
    NotStarted -> NotStarted
  }
}

/**
 * Converts an [Error] to a [Success] only if the error metadata matches type [E].
 *
 * If this is a [Success], [Loading], or [NotStarted], returns this unchanged.
 * If this is an [Error] with metadata of type [E], applies [transform] and returns [Success].
 * If this is an [Error] with metadata of a different type, returns the [Error] unchanged.
 *
 * Example:
 * ```kotlin
 * sealed interface ApiError
 * data class NotFoundError(val id: String) : ApiError
 * data class UnauthorizedError(val reason: String) : ApiError
 *
 * val result: AsyncResult<User> = ErrorWithMetadata(NotFoundError("123"))
 * val recovered = result.recoverIf<User, NotFoundError> { error ->
 *     User.guest() // only recovers from NotFoundError
 * }
 * ```
 *
 * @param transform Function to convert the typed error metadata to a success value
 * @return [AsyncResult] with the error recovered to success if metadata matches, unchanged otherwise
 */
public inline fun <R, reified E : Any> AsyncResult<R>.recoverIf(
    transform: (E) -> R
): AsyncResult<R> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }

  return when (this) {
    is Error -> {
      val metadata = metadataOrNull<E>()
      if (metadata != null) {
        Success(transform(metadata))
      } else {
        this
      }
    }

    is Success -> this
    Loading -> Loading
    NotStarted -> NotStarted
  }
}
