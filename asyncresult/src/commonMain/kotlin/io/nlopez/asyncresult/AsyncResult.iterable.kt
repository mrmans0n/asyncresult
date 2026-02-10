// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

import kotlin.jvm.JvmName

/** Returns a list of all the [Error]s from a list of [AsyncResult] items. */
public inline fun Iterable<AsyncResult<*>>.errors(): List<Error> = filterIsInstance<Error>()

/** Returns a list of all the [Error]s from a sequence of [AsyncResult] items. */
public inline fun Sequence<AsyncResult<*>>.errors(): List<Error> =
    filterIsInstance<Error>().toList()

/** Returns a list of all the [Error]s from an array of [AsyncResult] items. */
public inline fun Array<out AsyncResult<*>>.errors(): List<Error> = filterIsInstance<Error>()

/** Returns a list of all the [Error]s from a list of [AsyncResult] items. */
@Deprecated("Use errors()", ReplaceWith("errors()"))
public inline fun Iterable<AsyncResult<*>>.getAllErrors(): List<Error> = errors()

/** Returns a list of all the [Error]s from the given [AsyncResult] items. */
public inline fun errorsFrom(vararg lcrs: AsyncResult<*>): List<Error> = lcrs.errors()

/** Returns all [Success] values from a list of [AsyncResult] items. */
public inline fun <T> Iterable<AsyncResult<T>>.successes(): List<T> =
    filterIsInstance<Success<T>>().map { it.value }

/** Returns all [Success] values from a sequence of [AsyncResult] items. */
public inline fun <T> Sequence<AsyncResult<T>>.successes(): List<T> =
    filterIsInstance<Success<T>>().map { it.value }.toList()

/** Returns all [Success] values from an array of [AsyncResult] items. */
public inline fun <T> Array<out AsyncResult<T>>.successes(): List<T> =
    filterIsInstance<Success<T>>().map { it.value }

/** Returns true if any of the given [AsyncResult] items is an [Error]. */
public inline fun anyError(vararg lcrs: AsyncResult<*>): Boolean = lcrs.any { it is Error }

/**
 * Returns a list of metadata objects of type [T] from [Error]s in a list of [AsyncResult] items.
 */
@JvmName("iterableMetadata")
public inline fun <reified T> Iterable<AsyncResult<Error>>.metadata(): List<T> =
    errors().metadata<T>()

/** Returns a list of metadata objects of type [T] from a list of [Error] items. */
public inline fun <reified T> Iterable<Error>.metadata(): List<T> = mapNotNull {
  it.metadataOrNull<T>()
}

/** Returns a list of all the [Throwable]s inside of [Error]s from a list of [AsyncResult] items. */
public inline fun Iterable<AsyncResult<*>>.throwables(): List<Throwable> =
    errors().mapNotNull { it.throwable }

/**
 * Returns a list of all the [Throwable]s inside of [Error]s from a [Sequence] of [AsyncResult]
 * items.
 */
public inline fun Sequence<AsyncResult<*>>.throwables(): List<Throwable> =
    filterIsInstance<Error>().mapNotNull { it.throwable }.toList()

/**
 * Returns a list of all the [Throwable]s inside of [Error]s from an [Array] of [AsyncResult] items.
 */
public inline fun Array<out AsyncResult<*>>.throwables(): List<Throwable> =
    filterIsInstance<Error>().mapNotNull { it.throwable }

/** Returns a list of all the [Throwable]s inside of [Error]s from a list of [AsyncResult] items. */
@Deprecated("Use throwables() instead", ReplaceWith("throwables()"))
public inline fun Iterable<AsyncResult<*>>.getAllThrowables(): List<Throwable> = throwables()

/**
 * Returns a list of [Incomplete] items ([Loading] and [NotStarted]) from a list of [AsyncResult]
 * items.
 */
public inline fun Iterable<AsyncResult<*>>.incompletes(): List<Incomplete> =
    filterIsInstance<Incomplete>()

/**
 * Returns a list of [Incomplete] items ([Loading] and [NotStarted]) from a [Sequence] of
 * [AsyncResult] items.
 */
public inline fun Sequence<AsyncResult<*>>.incompletes(): List<Incomplete> =
    filterIsInstance<Incomplete>().toList()

/**
 * Returns a list of [Incomplete] items ([Loading] and [NotStarted]) from an [Array] of
 * [AsyncResult] items.
 */
public inline fun Array<out AsyncResult<*>>.incompletes(): List<Incomplete> =
    filterIsInstance<Incomplete>()

/** Returns true if any of the [AsyncResult] instances are [Loading]. */
public inline fun Iterable<AsyncResult<*>>.anyLoading(): Boolean = any { it is Loading }

/** Returns true if any of the given [AsyncResult] items is [Loading]. */
public inline fun anyLoading(vararg lcrs: AsyncResult<*>): Boolean = lcrs.any { it is Loading }

/** Returns true if any of the [AsyncResult] instances are [Incomplete]. */
public inline fun Iterable<AsyncResult<*>>.anyIncomplete(): Boolean = any { it is Incomplete }

/** Returns true if any of the given [AsyncResult] items is [Incomplete]. */
public inline fun anyIncomplete(vararg lcrs: AsyncResult<*>): Boolean =
    lcrs.any { it is Incomplete }

/**
 * Combines a [List] of [AsyncResult] items into a single [AsyncResult] containing a [List].
 *
 * The behavior is:
 * - If any item is an [Error], returns the first error
 * - If any item is [Loading], returns [Loading]
 * - If any item is [NotStarted], returns [NotStarted]
 * - If all items are [Success], returns [Success] with a list of all values
 *
 * Example:
 * ```kotlin
 * val results = listOf(Success(1), Success(2), Success(3))
 * val combined = results.combine() // Success(listOf(1, 2, 3))
 *
 * val withError = listOf(Success(1), Error(), Success(3))
 * val combined = withError.combine() // Error()
 * ```
 *
 * @return A single [AsyncResult] containing the list of all success values, or the first error
 */
public fun <T> List<AsyncResult<T>>.combine(): AsyncResult<List<T>> {
  // Check for errors first
  val firstError = firstOrNull { it is Error }
  if (firstError != null) return firstError as Error

  // Check for incomplete states
  if (any { it is Loading }) return Loading
  if (any { it is NotStarted }) return NotStarted

  // All are Success
  return Success(map { (it as Success).value })
}

/**
 * Combines an [Iterable] of [AsyncResult] items into a single [AsyncResult] containing a [List].
 *
 * The behavior is:
 * - If any item is an [Error], returns the first error
 * - If any item is [Loading], returns [Loading]
 * - If any item is [NotStarted], returns [NotStarted]
 * - If all items are [Success], returns [Success] with a list of all values
 *
 * Example:
 * ```kotlin
 * val results = listOf(Success(1), Success(2), Success(3))
 * val combined = results.combine() // Success(listOf(1, 2, 3))
 *
 * val withLoading = listOf(Success(1), Loading, Success(3))
 * val combined = withLoading.combine() // Loading
 * ```
 *
 * @return A single [AsyncResult] containing the list of all success values, or the first error
 */
public fun <T> Iterable<AsyncResult<T>>.combine(): AsyncResult<List<T>> {
  val list = toList()

  // Check for errors first
  val firstError = list.firstOrNull { it is Error }
  if (firstError != null) return firstError as Error

  // Check for incomplete states
  if (list.any { it is Loading }) return Loading
  if (list.any { it is NotStarted }) return NotStarted

  // All are Success
  return Success(list.map { (it as Success).value })
}

/**
 * Alias for [combine]. Converts a [List] of [AsyncResult] items into a single [AsyncResult]
 * containing a [List].
 *
 * @see combine
 */
public inline fun <T> List<AsyncResult<T>>.sequence(): AsyncResult<List<T>> = combine()

/**
 * Combines a [Sequence] of [AsyncResult] items into a single [AsyncResult] containing a [List].
 *
 * The behavior is:
 * - If any item is an [Error], returns the first error
 * - If any item is [Loading], returns [Loading]
 * - If any item is [NotStarted], returns [NotStarted]
 * - If all items are [Success], returns [Success] with a list of all values
 *
 * @return A single [AsyncResult] containing the list of all success values, or the first error
 */
public fun <T> Sequence<AsyncResult<T>>.combine(): AsyncResult<List<T>> = toList().combine()

/**
 * Partitions a collection of [AsyncResult] items into successful values and errors.
 *
 * Incomplete items ([Loading] and [NotStarted]) are ignored.
 *
 * @return A [Pair] where the first element is a list of success values and the second is a list of
 *   errors
 */
public inline fun <T> Iterable<AsyncResult<T>>.partition(): Pair<List<T>, List<Error>> {
  val successes = mutableListOf<T>()
  val errors = mutableListOf<Error>()
  for (item in this) {
    when (item) {
      is Success -> successes.add(item.value)
      is Error -> errors.add(item)
      else -> Unit
    }
  }
  return successes to errors
}

/**
 * Partitions a sequence of [AsyncResult] items into successful values and errors.
 *
 * Incomplete items ([Loading] and [NotStarted]) are ignored.
 *
 * @return A [Pair] where the first element is a list of success values and the second is a list of
 *   errors
 */
public inline fun <T> Sequence<AsyncResult<T>>.partition(): Pair<List<T>, List<Error>> {
  val successes = mutableListOf<T>()
  val errors = mutableListOf<Error>()
  for (item in this) {
    when (item) {
      is Success -> successes.add(item.value)
      is Error -> errors.add(item)
      else -> Unit
    }
  }
  return successes to errors
}

/**
 * Partitions an array of [AsyncResult] items into successful values and errors.
 *
 * Incomplete items ([Loading] and [NotStarted]) are ignored.
 *
 * @return A [Pair] where the first element is a list of success values and the second is a list of
 *   errors
 */
public inline fun <T> Array<out AsyncResult<T>>.partition(): Pair<List<T>, List<Error>> {
  val successes = mutableListOf<T>()
  val errors = mutableListOf<Error>()
  for (item in this) {
    when (item) {
      is Success -> successes.add(item.value)
      is Error -> errors.add(item)
      else -> Unit
    }
  }
  return successes to errors
}
