@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

import kotlin.jvm.JvmName

/**
 * Returns a list of al the [Error]s from a list of [AsyncResult] items.
 */
inline fun Iterable<AsyncResult<*>>.getAllErrors(): List<Error> = filterIsInstance<Error>()

/**
 * Returns a list of all the [Error]s from the given [AsyncResult] items.
 */
inline fun errorsFrom(vararg lcrs: AsyncResult<*>): List<Error> = lcrs.filterIsInstance<Error>()

/**
 * Returns true if any of the given [AsyncResult] items is an [Error].
 */
inline fun anyError(vararg lcrs: AsyncResult<*>): Boolean = lcrs.any { it is Error }

/**
 * Returns a list of metadata objects of type [T] from [Error]s in a list of [AsyncResult] items.
 */
@JvmName("iterableMetadata")
inline fun <reified T> Iterable<AsyncResult<Error>>.metadata(): List<T> = getAllErrors().metadata<T>()

/**
 * Returns a list of metadata objects of type [T] from a list of [Error] items.
 */
inline fun <reified T> Iterable<Error>.metadata(): List<T> = mapNotNull { it.metadataOrNull<T>() }

/**
 * Returns a list of al the [Throwable]s inside of [Error]s from a list of [AsyncResult] items.
 */
inline fun Iterable<AsyncResult<*>>.getAllThrowables(): List<Throwable> = getAllErrors()
    .mapNotNull { it.throwable }

/**
 * Returns true if any of the [AsyncResult] instances are [Loading].
 */
inline fun Iterable<AsyncResult<*>>.anyLoading() = any { it is Loading }

/**
 * Returns true if any of the given [AsyncResult] items is [Loading].
 */
inline fun anyLoading(vararg lcrs: AsyncResult<*>): Boolean = lcrs.any { it is Loading }

/**
 * Returns true if any of the [AsyncResult] instances are [Incomplete].
 */
inline fun Iterable<AsyncResult<*>>.anyIncomplete() = any { it is Incomplete }

/**
 * Returns true if any of the given [AsyncResult] items is [Incomplete].
 */
inline fun anyIncomplete(vararg lcrs: AsyncResult<*>): Boolean = lcrs.any { it is Incomplete }
