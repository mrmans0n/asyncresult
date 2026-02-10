// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

/**
 * Returns true if this [AsyncResult] is a [Success] containing the given [value].
 *
 * Supports the `in` operator: `value in result`.
 */
public inline operator fun <T> AsyncResult<T>.contains(value: T): Boolean = isSuccessAnd {
  it == value
}
