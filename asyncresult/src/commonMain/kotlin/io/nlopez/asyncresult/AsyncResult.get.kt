// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Returns the value if the [AsyncResult] is [Success] else returns null. */
public inline fun <R> AsyncResult<R>.getOrNull(): R? = value

/**
 * Returns the value if the [AsyncResult] is [Success] else returns the result of the [transform]
 * function.
 */
public inline fun <R> AsyncResult<R>.getOrElse(transform: (AsyncResult<R>) -> R): R {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is Success -> value
    else -> transform(this)
  }
}

/** Returns the value if the [AsyncResult] is [Success] else returns the [default] value. */
public inline fun <R> AsyncResult<R>.getOrDefault(default: R): R = value ?: default

/** Returns the value if the [AsyncResult] is [Success] else returns an empty list. */
public inline fun <R> AsyncResult<List<R>>.getOrEmpty(): List<R> = getOrElse { emptyList() }

/** Returns the value if the [AsyncResult] is [Success] else returns an empty list. */
public inline fun <R> AsyncResult<Sequence<R>>.getOrEmpty(): Sequence<R> = getOrElse {
  emptySequence()
}

/** Returns the value if the [AsyncResult] is [Success] else returns an empty map. */
public inline fun <K, V> AsyncResult<Map<K, V>>.getOrEmpty(): Map<K, V> = getOrElse { emptyMap() }

/** Returns the [Error] itself if the [AsyncResult] is [Error] else returns null. */
public inline fun <R> AsyncResult<R>.errorOrNull(): Error? {
  contract { returns() implies (this@errorOrNull is Error) }
  return this as? Error
}

/**
 * Returns the metadata value from the [Error] if the [AsyncResult] is [Error], and has metadata or
 * the requested type.
 */
public inline fun <reified M> AsyncResult<*>.errorWithMetadataOrNull(): M? =
    errorOrNull()?.metadataOrNull<M>()

/** Returns the throwable if the [AsyncResult] is [Error] else returns null. */
public inline fun <R> AsyncResult<R>.throwableOrNull(): Throwable? = errorOrNull()?.throwable

/** Returns the [ErrorId] if the [AsyncResult] is [Error] and has an errorId, else returns null. */
public inline fun <R> AsyncResult<R>.errorIdOrNull(): ErrorId? = errorOrNull()?.errorId

/** Returns the value if the [AsyncResult] is [Success] else returns null. */
public inline fun <R> AsyncResult<R>.getOrThrow(): R =
    when (this) {
      is Success -> value
      else ->
          throw throwableOrNull() ?: IllegalArgumentException("AsyncResult does not contain value")
    }
