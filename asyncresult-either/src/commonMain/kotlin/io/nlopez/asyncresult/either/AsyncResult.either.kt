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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Based on the either values inside of the [AsyncResult], convert it to a [Success] value if the
 * [Either] was [Either.Right], and a [Error] value if the [Either] was [Either.Left]. It will store
 * the [Either.Left] contents, if any, in the [Error.metadata] value.
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
 * Based on the either values ([Throwable] at the left side, [R] at the right side) inside of the
 * [AsyncResult], convert it to a [Success] value if the [Either] was [Either.Right], and a [Error]
 * value if the [Either] was [Either.Left] while also storing the [Throwable] in the error in the
 * [Error.throwable] property.
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
 * Based on the either values inside of the [Either], convert it to a [Success] value it is
 * [Either.Right], and a [Error] value it is [Either.Left]. It will store the [Either.Left]
 * contents, if any, in the [Error.metadata] value.
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
 * Converts the receiver [AsyncResult] to an [Either]. By default, it will try to use the error in
 * the from the [Error.metadata] value. In case you want to specify your own error type, you can use
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

/**
 * Transforms a [Flow] of [Either] into a [Flow] of [AsyncResult], converting each [Either.Left] to
 * an [Error] with metadata, and each [Either.Right] to a [Success].
 *
 * This is useful for converting flows from Arrow-based APIs into AsyncResult flows, enabling
 * seamless integration with AsyncResult operators and UI patterns.
 *
 * Behavior:
 * - When [startWithLoading] is `true` (default), emits [Loading] before the first value
 * - [Either.Right] values are wrapped in [Success]
 * - [Either.Left] values are converted to [Error] with the left value stored in [Error.metadata]
 *
 * Example:
 * ```kotlin
 * sealed class UserError {
 *     object NotFound : UserError()
 *     data class NetworkError(val code: Int) : UserError()
 * }
 *
 * val userFlow: Flow<Either<UserError, User>> = userRepository.observeUser()
 *
 * userFlow.asAsyncResult()
 *     .collect { result ->
 *         when (result) {
 *             is Loading -> showLoading()
 *             is Success -> showUser(result.value)
 *             is Error -> {
 *                 val userError = result.metadataOrNull<UserError>()
 *                 showError(userError)
 *             }
 *             is NotStarted -> { }
 *         }
 *     }
 * ```
 *
 * @param startWithLoading Whether to emit [Loading] before the first value. Defaults to `true`.
 * @return A [Flow] of [AsyncResult] wrapping the Either values.
 */
public fun <L, R> Flow<Either<L, R>>.asAsyncResult(
    startWithLoading: Boolean = true,
): Flow<AsyncResult<R>> =
    map { either -> either.toAsyncResult() }
        .run { if (startWithLoading) onStart { emit(Loading) } else this }

/**
 * Transforms a [Flow] of [Either] with a [Throwable] on the left side into a [Flow] of
 * [AsyncResult], converting each [Either.Left] to an [Error] with the throwable, and each
 * [Either.Right] to a [Success].
 *
 * This is a specialized version of [asAsyncResult] for the common case where errors are represented
 * as [Throwable]. The throwable is stored in [Error.throwable] instead of [Error.metadata].
 *
 * Behavior:
 * - When [startWithLoading] is `true` (default), emits [Loading] before the first value
 * - [Either.Right] values are wrapped in [Success]
 * - [Either.Left] throwables are converted to [Error] with [Error.throwable] set
 *
 * Example:
 * ```kotlin
 * val dataFlow: Flow<Either<IOException, Data>> = dataRepository.observeData()
 *
 * dataFlow.asAsyncResult()
 *     .collect { result ->
 *         when (result) {
 *             is Loading -> showLoading()
 *             is Success -> showData(result.value)
 *             is Error -> showError(result.throwable) // IOException is in throwable
 *             is NotStarted -> { }
 *         }
 *     }
 * ```
 *
 * @param startWithLoading Whether to emit [Loading] before the first value. Defaults to `true`.
 * @return A [Flow] of [AsyncResult] wrapping the Either values.
 */
@JvmName("asAsyncResultWithLeftThrowable")
public fun <R> Flow<Either<Throwable, R>>.asAsyncResult(
    startWithLoading: Boolean = true,
): Flow<AsyncResult<R>> =
    map { either ->
          when (either) {
            is Left -> Error(throwable = either.value)
            is Right -> Success(either.value)
          }
        }
        .run { if (startWithLoading) onStart { emit(Loading) } else this }
