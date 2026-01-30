// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Extracts the value from the [AsyncResult] if it's a [Success], otherwise throws an
 * [UnwrapException].
 */
public inline fun <R> AsyncResult<R>.unwrap(): R {
  contract { returns() implies (this@unwrap is Success) }

  return getOrElse { throw UnwrapException("Tried to unwrap a non-`Success` value") }
}

/**
 * Extracts the value from the [AsyncResult] if it's an [Error], otherwise throws an
 * [UnwrapException].
 */
public inline fun AsyncResult<*>.unwrapError(): Error {
  contract { returns() implies (this@unwrapError is Error) }

  return errorOrNull() ?: throw UnwrapException("Tried to unwrap a non-`Error` value")
}

/**
 * Extracts the value from the [AsyncResult] if it's an [Error] with a [Throwable], otherwise throws
 * an [UnwrapException].
 */
public inline fun AsyncResult<*>.unwrapThrowable(): Throwable {
  contract { returns() implies (this@unwrapThrowable is Error) }

  val error = errorOrNull() ?: throw UnwrapException("Tried to unwrap a non-`Error` value")
  return error.throwable
      ?: throw UnwrapException("Tried to unwrap an `Error` that had no Throwable")
}

/**
 * Extracts the metadata from the [AsyncResult] if it's an [Error] with metadata of type [M],
 * otherwise throws an [UnwrapException].
 */
public inline fun <reified M> AsyncResult<*>.unwrapMetadata(): M {
  contract { returns() implies (this@unwrapMetadata is Error) }

  return errorWithMetadataOrNull<M>()
      ?: throw UnwrapException("Tried to unwrap a non-`Error` value")
}

/**
 * Extracts the value from the [AsyncResult] if it's a [Success], otherwise throws an
 * [UnwrapException] with the computed [message] in it.
 */
public inline fun <R> AsyncResult<R>.expect(crossinline message: () -> Any): R {
  contract {
    callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    returns() implies (this@expect is Success)
  }

  return getOrElse { throw UnwrapException("${message()}") }
}

/**
 * Extracts the value from the [AsyncResult] if it's an [Error], otherwise throws an
 * [UnwrapException] with the computed [message] in it.
 */
public inline fun AsyncResult<*>.expectError(crossinline message: () -> Any): Error {
  contract {
    callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    returns() implies (this@expectError is Error)
  }

  return errorOrNull() ?: throw UnwrapException("${message()}")
}

/**
 * Extracts the value from the [AsyncResult] if it's an [Error] with a [Throwable] in it, otherwise
 * throws an [UnwrapException] with the computed [message] in it.
 */
public inline fun AsyncResult<*>.expectThrowable(crossinline message: () -> Any): Throwable {
  contract {
    callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    returns() implies (this@expectThrowable is Error)
  }

  return throwableOrNull() ?: throw UnwrapException("${message()}")
}

/**
 * Extracts the metadata from the [AsyncResult] if it's an [Error] with the metadata of type [M],
 * otherwise throws an [UnwrapException] with the computed [message] in it.
 */
public inline fun <reified M> AsyncResult<*>.expectMetadata(crossinline message: () -> Any): M {
  contract {
    callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    returns() implies (this@expectMetadata is Error)
  }

  return this.errorWithMetadataOrNull<M>() ?: throw UnwrapException("${message()}")
}

public class UnwrapException(message: String) : Exception(message)
