// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

/** Runs the [block] when the value is [Success]. */
public inline fun <R> AsyncResult<R>.onSuccess(block: (R) -> Unit): AsyncResult<R> {
  if (this is Success) {
    block(value)
  }
  return this
}

/** Runs the [block] when the result is [Loading]. */
public inline fun <R> AsyncResult<R>.onLoading(block: () -> Unit): AsyncResult<R> {
  if (this is Loading) {
    block()
  }
  return this
}

/** Runs the [block] when the result is [Error]. */
public inline fun <R> AsyncResult<R>.onError(block: (Throwable?) -> Unit): AsyncResult<R> {
  if (this is Error) {
    block(throwable)
  }
  return this
}

/** Runs the [block] when the result is [Error] and has metadata of type [M]. */
public inline fun <R, reified M> AsyncResult<R>.onErrorWithMetadata(
    block: (throwable: Throwable?, metadata: M?) -> Unit,
): AsyncResult<R> {
  if (this is Error) {
    block(throwable, metadataOrNull<M>())
  }
  return this
}

/** Runs the [block] when the result is [NotStarted]. */
public inline fun <R> AsyncResult<R>.onNotStarted(block: () -> Unit): AsyncResult<R> {
  if (this is NotStarted) {
    block()
  }
  return this
}

/** Runs the [block] when the result is [Incomplete] ([Loading] or [NotStarted]). */
public inline fun <R> AsyncResult<R>.onIncomplete(block: () -> Unit): AsyncResult<R> {
  if (this is Incomplete) {
    block()
  }
  return this
}
