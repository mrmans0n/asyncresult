// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

import kotlin.jvm.JvmInline

/** Models the state of an asynchronous operation. */
public sealed class AsyncResult<out S>(public open val value: S? = null)

/**
 * Used to model idle states. For example, if an operation is user initiated, it'll remain in this
 * state until the user starts it (when it'll likely transition to [Loading]).
 */
public data object NotStarted : AsyncResult<Nothing>(), Incomplete

/**
 * Used to model in-flight operations. For example, if a request is user initiated, it'll be in this
 * state since the user initiated it up until it's either [Success] or [Error].
 */
public data object Loading : AsyncResult<Nothing>(), Incomplete

/** A successful request, containing the resulting [value]. */
public data class Success<out S>(override val value: S) : AsyncResult<S>(value = value)

/** A unique identifier for error tracking and correlation. */
@JvmInline public value class ErrorId(public val value: String)

/**
 * The modeled request failed. If there was anything thrown to cause this, it'll be included in
 * [throwable]. It might also contain some extra [metadata], typically used to provide context about
 * the failure, to help make it actionable (either for the user or the developer). An optional
 * [errorId] can be provided for error tracking and correlation purposes.
 */
public data class Error(
    public val throwable: Throwable? = null,
    @PublishedApi internal val metadata: Any? = null,
    public val errorId: ErrorId? = null
) : AsyncResult<Nothing>() {
  /** Returns the [metadata] as the given type [T]. */
  public inline fun <reified T> metadataOrNull(): T? = metadata as? T

  /** Creates a copy of the [Error] object adding the given [metadata], preserving [errorId]. */
  public inline fun <T> withMetadata(metadata: T): Error = Error(throwable, metadata, errorId)

  /** Creates a copy of the [Error] object adding the given [errorId], preserving [metadata]. */
  public inline fun withErrorId(errorId: ErrorId): Error = Error(throwable, metadata, errorId)

  public companion object {
    public val Empty: Error = Error()
  }
}

/** Creates an [Error] object with the given [metadata] and optional [errorId]. */
@Suppress("FunctionName")
public inline fun ErrorWithMetadata(metadata: Any, errorId: ErrorId? = null): Error =
    Error(metadata = metadata, errorId = errorId)

/** Creates an [Error] object with the given [errorId] and optional [metadata]. */
@Suppress("FunctionName")
public inline fun ErrorWithId(errorId: ErrorId, metadata: Any? = null): Error =
    Error(metadata = metadata, errorId = errorId)

/** Adds the given [metadata] to the [Error] object, preserving [errorId]. */
public inline infix operator fun Error.plus(metadata: Any): Error = withMetadata(metadata)

/** Adds the given [errorId] to the [Error] object, preserving [metadata]. */
public inline infix operator fun Error.plus(errorId: ErrorId): Error = withErrorId(errorId)

/**
 * Marks any [AsyncResult] that hasn't yet delivered a result ([Success] or [Error]). Useful to
 * bundle [NotStarted] and [Loading] states in exhaustive when statements.
 */
public sealed interface Incomplete

/** Returns true if the [AsyncResult] is [Incomplete], false otherwise. */
public inline val <R> AsyncResult<R>.isIncomplete: Boolean
  get() = this is Incomplete

/** Returns true if the [AsyncResult] is [Success], false otherwise. */
public inline val <R> AsyncResult<R>.isSuccess: Boolean
  get() = this is Success
