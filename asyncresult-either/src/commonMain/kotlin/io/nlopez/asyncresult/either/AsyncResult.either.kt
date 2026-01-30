// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult.either

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import io.nlopez.asyncresult.AsyncResult
import io.nlopez.asyncresult.Error
import io.nlopez.asyncresult.Loading
import io.nlopez.asyncresult.NotStarted
import io.nlopez.asyncresult.Success
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Binds the [Either] inside this [AsyncResult]. Returns [Success] if the [Either] was
 * [Either.Right], and [Error] if the [Either] was [Either.Left]. The [Either.Left] contents, if
 * any, are stored in [Error.metadata].
 */
public inline fun <L, R> AsyncResult<Either<L, R>>.bind(): AsyncResult<R> =
    when (this) {
      NotStarted -> NotStarted
      Loading -> Loading
      is Success ->
          when (val either = value) {
            is Left -> Error().withMetadata(either.value)
            is Right -> Success(either.value)
          }

      is Error -> this
    }

/**
 * Binds the [Either] inside this [AsyncResult]. Returns [Success] if the [Either] was
 * [Either.Right], and [Error] if the [Either] was [Either.Left]. The [Throwable] from
 * [Either.Left] is stored in [Error.throwable].
 */
@JvmName("bindWithLeftThrowable")
public inline fun <R> AsyncResult<Either<Throwable, R>>.bind(): AsyncResult<R> =
    when (this) {
      NotStarted -> NotStarted
      Loading -> Loading
      is Success ->
          when (val either = value) {
            is Left -> Error(throwable = either.value)
            is Right -> Success(either.value)
          }

      is Error -> this
    }

/**
 * Converts the [Either] to an [AsyncResult]. Returns [Success] if the [Either] is [Either.Right],
 * and [Error] if the [Either] is [Either.Left]. It will store the [Either.Left] contents, if any,
 * in the [Error.metadata] value.
 */
public inline fun <L, R> Either<L, R>.toAsyncResult(): AsyncResult<R> =
    when (this) {
      is Left -> Error().withMetadata(value)
      is Right -> Success(value)
    }

/**
 * Converts the receiver [AsyncResult] to an [Either]. It will use the [Error] as the value on the
 * left side of the [Either] and the [Success] as the value on the right side of the [Either].
 */
public suspend inline fun <R> Flow<AsyncResult<R>>.toEither(): Either<Error, R> =
    first { it is Success<R> || it is Error }
        .let { lcr ->
          when (lcr) {
            is Success -> Right(lcr.value)
            is Error -> Left(lcr)
            else -> error("Unexpected result type: ${lcr::class.simpleName}")
          }
        }

/**
 * Converts the receiver [AsyncResult] to an [Either]. By default, it will try to use the error
 * from the [Error.metadata] value. In case you want to specify your own error type, you can use
 * the [errorTransform] function.
 */
public suspend inline fun <reified E, R> Flow<AsyncResult<R>>.toEither(
    errorTransform: (Error) -> E = {
      it.metadataOrNull<E>()
          ?: error(
              "Unexpected error type: $it. " +
                  "Expected ${E::class.simpleName}. Provide your own errorTransform function to handle this case.",
          )
    },
): Either<E, R> = toEither().mapLeft(errorTransform)
