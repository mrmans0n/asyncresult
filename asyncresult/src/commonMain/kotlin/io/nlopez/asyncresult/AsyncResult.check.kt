// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

/**
 * Returns true if the [AsyncResult] is [Success] and the [predicate] returns true for the success
 * value.
 *
 * Inspired by Rust's `is_ok_and`.
 *
 * @param predicate Function that receives the success value and returns true when it matches.
 * @return true if this is [Success] and [predicate] returns true, false otherwise.
 */
public inline fun <T> AsyncResult<T>.isSuccessAnd(predicate: (T) -> Boolean): Boolean =
    this is Success && predicate(value)

/**
 * Returns true if the [AsyncResult] is [Error] and the [predicate] returns true for the throwable.
 *
 * Inspired by Rust's `is_err_and`.
 *
 * @param predicate Function that receives the throwable and returns true when it matches.
 * @return true if this is [Error] and [predicate] returns true, false otherwise.
 */
public inline fun <T> AsyncResult<T>.isErrorAnd(predicate: (Throwable?) -> Boolean): Boolean =
    this is Error && predicate(throwable)

/**
 * Returns true if the [AsyncResult] is [Error] and the [predicate] returns true for the throwable
 * and metadata.
 *
 * @param E The expected metadata type.
 * @param predicate Function that receives the throwable and typed metadata and returns true when
 *   they match.
 * @return true if this is [Error] and [predicate] returns true, false otherwise.
 */
public inline fun <T, reified E> AsyncResult<T>.isErrorWithMetadataAnd(
    predicate: (Throwable?, E?) -> Boolean,
): Boolean = this is Error && predicate(throwable, metadataOrNull<E>())
