// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

/** Models the state of an asynchronous operation. */
sealed class AsyncResult<out S>(open val value: S? = null)

/**
 * Used to model idle states. For example, if an operation is user initiated, it'll remain in this
 * state until the user starts it (when it'll likely transition to [Loading]).
 */
data object NotStarted : AsyncResult<Nothing>(), Incomplete

/**
 * Used to model in-flight operations. For example, if a request is user initiated, it'll be in this
 * state since the user initiated it up until it's either [Success] or [Error].
 */
data object Loading : AsyncResult<Nothing>(), Incomplete

/** A successful request, containing the resulting [value]. */
data class Success<out S>(override val value: S) : AsyncResult<S>(value = value)

/**
 * The modeled request failed. If there was anything thrown to cause this, it'll be included in
 * [throwable]. It might also contain some extra [metadata], typically used to provide context about
 * the failure, to help make it actionable (either for the user or the developer).
 */
data class Error(
    val throwable: Throwable? = null,
    @PublishedApi internal val metadata: Any? = null
) : AsyncResult<Nothing>() {
  /** Returns the [metadata] as the given type [T]. */
  inline fun <reified T> metadataOrNull(): T? = metadata as? T

  /** Creates a copy of the [Error] object adding the given [metadata]. */
  inline fun <T> withMetadata(metadata: T): Error = Error(throwable, metadata)

  companion object {
    val Empty = Error()
  }
}

/** Creates an [Error] object with the given [metadata] */
@Suppress("FunctionName")
inline fun ErrorWithMetadata(metadata: Any): Error = Error(metadata = metadata)

/** Adds the given [metadata] to the [Error] object. */
inline infix operator fun Error.plus(metadata: Any): Error = withMetadata(metadata)

/**
 * Marks any [AsyncResult] that hasn't yet delivered a result ([Success] or [Error]). Useful to
 * bundle [NotStarted] and [Loading] states in exhaustive when statements.
 */
sealed interface Incomplete

/** Returns true if the [AsyncResult] is [Incomplete], false otherwise. */
inline val <R> AsyncResult<R>.isIncomplete: Boolean
  get() = this is Incomplete

/** Returns true if the [AsyncResult] is [Success], false otherwise. */
inline val <R> AsyncResult<R>.isSuccess: Boolean
  get() = this is Success
