// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public interface ResultScope {
  public fun <T> AsyncResult<T>.bind(): T

  public fun error(error: Throwable): Nothing

  public fun loading(): Nothing

  public fun ensure(condition: Boolean, lazyError: () -> Throwable)

  public fun <T> ensureNotNull(value: T?, lazyError: () -> Throwable): T
}

@OptIn(ExperimentalContracts::class)
public inline fun <T> result(crossinline block: ResultScope.() -> T): AsyncResult<T> {
  contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

  val scope = ResultScopeImpl()
  return try {
    Success(scope.block())
  } catch (_: ResultShortCircuit) {
    scope.shortCircuitResult!!
  }
}

@PublishedApi
internal class ResultScopeImpl : ResultScope {
  internal var shortCircuitResult: AsyncResult<Nothing>? = null

  override fun <T> AsyncResult<T>.bind(): T =
      when (this) {
        is Success -> value
        is Error -> {
          shortCircuitResult = this
          throw ResultShortCircuit()
        }

        is Loading -> {
          shortCircuitResult = Loading
          throw ResultShortCircuit()
        }

        is NotStarted -> {
          shortCircuitResult = NotStarted
          throw ResultShortCircuit()
        }
      }

  override fun error(error: Throwable): Nothing {
    shortCircuitResult = Error(error)
    throw ResultShortCircuit()
  }

  override fun loading(): Nothing {
    shortCircuitResult = Loading
    throw ResultShortCircuit()
  }

  override fun ensure(condition: Boolean, lazyError: () -> Throwable) {
    if (!condition) {
      error(lazyError())
    }
  }

  override fun <T> ensureNotNull(value: T?, lazyError: () -> Throwable): T = value ?: error(lazyError())
}

@PublishedApi internal class ResultShortCircuit : Exception()
