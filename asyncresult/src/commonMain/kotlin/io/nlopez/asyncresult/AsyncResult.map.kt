// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

/**
 * Transforms the [Success] value via [transform]. The [AsyncResult] will change its containing type
 * accordingly.
 */
public inline fun <R, T> AsyncResult<R>.mapSuccess(transform: (R) -> T): AsyncResult<T> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }

  return when (this) {
    is Incomplete -> cast()
    is Success -> Success(transform(value))
    is Error -> this
  }
}

/** Transforms the [Error] value, if any, via [transform]. */
public inline fun <R> AsyncResult<R>.mapError(transform: (Error) -> Error): AsyncResult<R> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }

  return when (this) {
    is Error -> transform(this)
    Loading -> Loading
    NotStarted -> NotStarted
    is Success -> this
  }
}

/**
 * Converts [Loading] and [NotStarted] to a [AsyncResult] of type [T]. This is to prevent us to
 * having to map everything when we know we don't have a [Success] value, where types would actually
 * matter.
 */
public inline fun <T> Incomplete.cast(): AsyncResult<T> =
    when (this) {
      Loading -> Loading
      NotStarted -> NotStarted
    }

/**
 * Converts the current [AsyncResult] to an [AsyncResult] of type [R] if the current value is of
 * type [R]. If the current value is not of type [R], it will return an [Error].
 */
public inline fun <reified R> AsyncResult<*>.castOrError(
    noinline lazyMetadata: (() -> Any?)? = null,
): AsyncResult<R> = flatMap { value ->
  when (value) {
    is R -> Success(value)
    else ->
        Error(
            throwable = ClassCastException("Value ($value) is not of type ${R::class.simpleName}"),
            metadata = lazyMetadata?.invoke(),
        )
  }
}

/**
 * Returns the current [AsyncResult] if it matches the given [predicate], or an [Error] if it does
 * not.
 */
public inline fun <reified R> AsyncResult<R>.filterOrError(
    noinline lazyMetadata: (() -> Any?)? = null,
    predicate: ((R) -> Boolean),
): AsyncResult<R> = flatMap { value ->
  when (predicate(value)) {
    true -> Success(value)
    false ->
        Error(
            throwable = ClassCastException("Value ($value) did not match predicate"),
            metadata = lazyMetadata?.invoke(),
        )
  }
}

/** Transforms the current [AsyncResult] to the result of [transform]. */
public inline fun <R, T> AsyncResult<R>.map(
    transform: (AsyncResult<R>) -> AsyncResult<T>,
): AsyncResult<T> {
  contract { callsInPlace(transform, InvocationKind.EXACTLY_ONCE) }
  return transform(this)
}

/** Generates a value of type [T] from any state contained in [AsyncResult]. */
public inline fun <R, T> AsyncResult<R>.fold(
    ifNotStarted: () -> T,
    ifLoading: () -> T,
    ifSuccess: (R) -> T,
    ifError: (Error) -> T,
): T {
  contract {
    callsInPlace(ifNotStarted, InvocationKind.AT_MOST_ONCE)
    callsInPlace(ifLoading, InvocationKind.AT_MOST_ONCE)
    callsInPlace(ifSuccess, InvocationKind.AT_MOST_ONCE)
    callsInPlace(ifError, InvocationKind.AT_MOST_ONCE)
  }

  return when (this) {
    NotStarted -> ifNotStarted()
    Loading -> ifLoading()
    is Success -> ifSuccess(value)
    is Error -> ifError(this)
  }
}

/**
 * Transforms the current [AsyncResult] to the result of [transform] if it's a [Success], based on
 * its contained value.
 */
public inline fun <R, reified T> AsyncResult<R>.flatMap(
    transform: (R) -> AsyncResult<T>
): AsyncResult<T> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }

  return when (this) {
    is Incomplete -> cast()
    is Success -> transform(value)
    is Error -> this
  }
}

/** Unwraps the [AsyncResult] inside of the [AsyncResult]. */
public inline fun <reified R> AsyncResult<AsyncResult<R>>.flatten(): AsyncResult<R> = flatMap { it }

/**
 * Transforms the type of [AsyncResult] from nullable to non-nullable. If the value is null, it will
 * return an [Error]. It also allows adding specific metadata to disambiguate errors if necessary,
 * via [lazyMetadata].
 */
public inline fun <reified R> AsyncResult<R?>.orError(
    noinline lazyMetadata: (() -> Any?)? = null,
): AsyncResult<R> =
    when (this) {
      NotStarted -> NotStarted
      Loading -> Loading
      is Success ->
          value?.let { Success(it) }
              ?: Error(
                  throwable = IllegalArgumentException("Required value was null."),
                  metadata = lazyMetadata?.invoke(),
              )

      is Error -> this
    }

/**
 * Spreads a [AsyncResult] of a [Pair] into a [Pair] of [AsyncResult]s.
 * - [Success] containing a pair becomes a pair of [Success] values
 * - [NotStarted] becomes a pair of [NotStarted]
 * - [Loading] becomes a pair of [Loading]
 * - [Error] becomes a pair of the same [Error]
 */
public fun <A, B> AsyncResult<Pair<A, B>>.spread(): Pair<AsyncResult<A>, AsyncResult<B>> =
    when (this) {
      NotStarted -> Pair(NotStarted, NotStarted)
      Loading -> Pair(Loading, Loading)
      is Success -> Pair(Success(value.first), Success(value.second))
      is Error -> Pair(this, this)
    }

/**
 * Spreads a [AsyncResult] of a [Triple] into a [Triple] of [AsyncResult]s.
 * - [Success] containing a triple becomes a triple of [Success] values
 * - [NotStarted] becomes a triple of [NotStarted]
 * - [Loading] becomes a triple of [Loading]
 * - [Error] becomes a triple of the same [Error]
 */
public fun <A, B, C> AsyncResult<Triple<A, B, C>>.spread():
    Triple<AsyncResult<A>, AsyncResult<B>, AsyncResult<C>> =
    when (this) {
      NotStarted -> Triple(NotStarted, NotStarted, NotStarted)
      Loading -> Triple(Loading, Loading, Loading)
      is Success -> Triple(Success(value.first), Success(value.second), Success(value.third))
      is Error -> Triple(this, this, this)
    }

@JvmName("component1Pair")
public inline operator fun <T> AsyncResult<Pair<T, *>>.component1(): AsyncResult<T> = mapSuccess {
  it.first
}

@JvmName("component2Pair")
public inline operator fun <T> AsyncResult<Pair<*, T>>.component2(): AsyncResult<T> = mapSuccess {
  it.second
}

@JvmName("component1Triple")
public inline operator fun <T> AsyncResult<Triple<T, *, *>>.component1(): AsyncResult<T> =
    mapSuccess {
      it.first
    }

@JvmName("component2Triple")
public inline operator fun <T> AsyncResult<Triple<*, T, *>>.component2(): AsyncResult<T> =
    mapSuccess {
      it.second
    }

@JvmName("component3Triple")
public inline operator fun <T> AsyncResult<Triple<*, *, T>>.component3(): AsyncResult<T> =
    mapSuccess {
      it.third
    }
