// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

import kotlin.jvm.JvmName

/** Returns a list of all the [Error]s from a list of [AsyncResult] items. */
public inline fun Iterable<AsyncResult<*>>.getAllErrors(): List<Error> = filterIsInstance<Error>()

/** Returns a list of all the [Error]s from the given [AsyncResult] items. */
public inline fun errorsFrom(vararg lcrs: AsyncResult<*>): List<Error> =
    lcrs.filterIsInstance<Error>()

/** Returns true if any of the given [AsyncResult] items is an [Error]. */
public inline fun anyError(vararg lcrs: AsyncResult<*>): Boolean = lcrs.any { it is Error }

/**
 * Returns a list of metadata objects of type [T] from [Error]s in a list of [AsyncResult] items.
 */
@JvmName("iterableMetadata")
public inline fun <reified T> Iterable<AsyncResult<Error>>.metadata(): List<T> =
    getAllErrors().metadata<T>()

/** Returns a list of metadata objects of type [T] from a list of [Error] items. */
public inline fun <reified T> Iterable<Error>.metadata(): List<T> = mapNotNull {
  it.metadataOrNull<T>()
}

/** Returns a list of all the [Throwable]s inside of [Error]s from a list of [AsyncResult] items. */
public inline fun Iterable<AsyncResult<*>>.getAllThrowables(): List<Throwable> =
    getAllErrors().mapNotNull { it.throwable }

/** Returns true if any of the [AsyncResult] instances are [Loading]. */
public inline fun Iterable<AsyncResult<*>>.anyLoading(): Boolean = any { it is Loading }

/** Returns true if any of the given [AsyncResult] items is [Loading]. */
public inline fun anyLoading(vararg lcrs: AsyncResult<*>): Boolean = lcrs.any { it is Loading }

/** Returns true if any of the [AsyncResult] instances are [Incomplete]. */
public inline fun Iterable<AsyncResult<*>>.anyIncomplete(): Boolean = any { it is Incomplete }

/** Returns true if any of the given [AsyncResult] items is [Incomplete]. */
public inline fun anyIncomplete(vararg lcrs: AsyncResult<*>): Boolean =
    lcrs.any { it is Incomplete }
