// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult.test

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import io.nlopez.asyncresult.AsyncResult
import io.nlopez.asyncresult.Error
import io.nlopez.asyncresult.ErrorId
import io.nlopez.asyncresult.Incomplete
import io.nlopez.asyncresult.Loading
import io.nlopez.asyncresult.NotStarted
import io.nlopez.asyncresult.Success
import io.nlopez.asyncresult.errorIdOrNull
import io.nlopez.asyncresult.getOrThrow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

public fun <T> Assert<AsyncResult<T>>.isNotStarted(): Assert<NotStarted> = transform { actual ->
  when (actual) {
    is NotStarted -> actual
    else -> expected("AsyncResult to be NotStarted, but was $actual")
  }
}

public fun <T> Assert<AsyncResult<T>>.isLoading(): Assert<Loading> = transform { actual ->
  when (actual) {
    is Loading -> actual
    else -> expected("AsyncResult to be Loading, but was $actual")
  }
}

public fun <T> Assert<AsyncResult<T>>.isIncomplete(): Assert<Incomplete> = transform { actual ->
  when (actual) {
    is Incomplete -> actual
    else -> expected("AsyncResult to be Incomplete, but was $actual")
  }
}

public fun <T> Assert<AsyncResult<T>>.isSuccess(): Assert<T> = transform { actual ->
  when (actual) {
    is Success -> actual.value
    else -> expected("AsyncResult to be Success, but was $actual")
  }
}

public fun <T> Assert<AsyncResult<T>>.isSuccessEqualTo(expected: T): Unit = given { actual ->
  assertThat(actual).isSuccess().isEqualTo(expected)
}

public fun <T> Assert<AsyncResult<T>>.isError(): Assert<Error> = transform { actual ->
  when (actual) {
    is Error -> actual
    else -> expected("AsyncResult to be Error, but was $actual")
  }
}

public inline fun <T, reified M> Assert<AsyncResult<T>>.isErrorWithMetadata(): Assert<M> =
    transform { actual ->
      when (actual) {
        is Error ->
            actual.metadataOrNull<M>()
                ?: expected("AsyncResult to be Error with metadata of type ${M::class.simpleName}")

        else -> expected("AsyncResult to be Error, but was $actual")
      }
    }

public inline fun <T, reified E> Assert<Error>.isThrowableEqualTo(expected: E): Unit =
    given { actual ->
      assertThat(actual.throwable).isEqualTo(expected)
    }

public inline fun <T, reified M> Assert<Error>.isMetadataEqualTo(value: M): Unit = given { actual ->
  assertThat(actual.metadataOrNull<M>()).isEqualTo(value)
}

public inline fun <T, reified M> Assert<AsyncResult<T>>.isErrorWithMetadataEqualTo(value: M): Unit =
    given { actual ->
      assertThat(actual).isErrorWithMetadata<T, M>().isEqualTo(value)
    }

/** Asserts the [AsyncResult] is an [Error] with an [ErrorId] and returns the [ErrorId]. */
public fun <T> Assert<AsyncResult<T>>.isErrorWithId(): Assert<ErrorId> = transform { actual ->
  when (actual) {
    is Error ->
        actual.errorId ?: expected("AsyncResult to be Error with errorId, but errorId was null")

    else -> expected("AsyncResult to be Error, but was $actual")
  }
}

/** Asserts the [Error] has the given [ErrorId]. */
public fun Assert<Error>.isErrorIdEqualTo(expected: ErrorId): Unit = given { actual ->
  assertThat(actual.errorId).isEqualTo(expected)
}

/** Asserts the [AsyncResult] is an [Error] with the given [ErrorId]. */
public fun <T> Assert<AsyncResult<T>>.isErrorWithIdEqualTo(expected: ErrorId): Unit =
    given { actual ->
      assertThat(actual).isErrorWithId().isEqualTo(expected)
    }

/** Asserts the [AsyncResult] has the given [ErrorId] if it's an [Error], checking via extension. */
public fun <T> Assert<AsyncResult<T>>.hasErrorId(expected: ErrorId): Unit = given { actual ->
  assertThat(actual.errorIdOrNull()).isEqualTo(expected)
}

// ============================================================================
// Error + Throwable helpers
// ============================================================================

/** Asserts the [AsyncResult] is an [Error] with a [Throwable] and returns it for further checks. */
public fun <T> Assert<AsyncResult<T>>.isErrorWithThrowable(): Assert<Throwable> =
    transform { actual ->
      when (actual) {
        is Error ->
            actual.throwable
                ?: expected("AsyncResult to be Error with throwable, but throwable was null")

        else -> expected("AsyncResult to be Error, but was $actual")
      }
    }

/**
 * Asserts the [AsyncResult] is an [Error] with a [Throwable] of the given type and returns it for
 * further checks.
 */
public inline fun <T, reified E : Throwable> Assert<AsyncResult<T>>.isErrorWithThrowableOfType():
    Assert<E> = transform { actual ->
  when (actual) {
    is Error -> {
      val throwable = actual.throwable
      when {
        throwable == null ->
            expected("AsyncResult to be Error with throwable, but throwable was null")

        throwable !is E ->
            expected(
                "AsyncResult to be Error with throwable of type ${E::class.simpleName}, " +
                    "but was ${throwable::class.simpleName}")

        else -> throwable
      }
    }
    else -> expected("AsyncResult to be Error, but was $actual")
  }
}

/**
 * Asserts the [AsyncResult] is an [Error] with a [Throwable] whose message matches [expected].
 */
public fun <T> Assert<AsyncResult<T>>.isErrorWithThrowableMessage(expected: String): Unit =
    given { actual ->
      val error =
          when (actual) {
            is Error -> actual
            else -> throw AssertionError("Expected AsyncResult to be Error, but was $actual")
          }
      val throwable =
          error.throwable
              ?: throw AssertionError("Expected Error to have throwable, but throwable was null")
      val actualMessage = throwable.message
      if (actualMessage != expected) {
        throw AssertionError(
            "Expected throwable message to be \"$expected\", but was \"$actualMessage\"")
      }
    }

// ============================================================================
// Flow: Terminal emission asserts
// ============================================================================

/**
 * Asserts the first terminal emission from the flow is [Success] with the given [expected] value.
 */
public suspend fun <T> Flow<AsyncResult<T>>.assertSuccess(expected: T) {
  val result = getOrThrow()
  assertThat(result).isEqualTo(expected)
}

/**
 * Asserts the first terminal emission from the flow is [Error] and returns it for further checks.
 */
public suspend fun <T> Flow<AsyncResult<T>>.assertError(): Error {
  val terminal = first { it is Success || it is Error }
  return (terminal as? Error)
      ?: throw AssertionError("AsyncResult flow to emit Error, but was $terminal")
}

/**
 * Asserts the first terminal emission from the flow is [Error] with metadata of type [M] and
 * returns it.
 */
public suspend inline fun <T, reified M> Flow<AsyncResult<T>>.assertErrorWithMetadata(): M {
  val error = assertError()
  return error.metadataOrNull<M>()
      ?: throw AssertionError(
          "Expected Error to have metadata of type ${M::class.simpleName}, but was null")
}

/**
 * Asserts the first terminal emission from the flow is [Error] with a [Throwable] of type [E] and
 * returns it.
 */
public suspend inline fun <T, reified E : Throwable>
    Flow<AsyncResult<T>>.assertErrorWithThrowableOfType(): E {
  val error = assertError()
  val throwable = error.throwable
  return when {
    throwable == null ->
        throw AssertionError("Expected Error to have throwable, but throwable was null")

    throwable !is E ->
        throw AssertionError(
            "Expected Error to have throwable of type ${E::class.simpleName}, " +
                "but was ${throwable::class.simpleName}")

    else -> throwable
  }
}

/**
 * Asserts the first terminal emission from the flow is [Error] with the given [ErrorId].
 */
public suspend fun <T> Flow<AsyncResult<T>>.assertErrorWithId(expected: ErrorId) {
  val error = assertError()
  val actualId = error.errorId
  if (actualId != expected) {
    throw AssertionError("Expected Error to have errorId $expected, but was $actualId")
  }
}
<<<<<<< Updated upstream
=======

// ============================================================================
// Flow: Non-terminal emission asserts
// ============================================================================

/** Asserts the first emission from the flow is [Loading]. */
public suspend fun <T> Flow<AsyncResult<T>>.assertFirstIsLoading() {
  val firstEmission = first()
  if (firstEmission !is Loading) {
    throw AssertionError("Expected first emission to be Loading, but was $firstEmission")
  }
}

/** Asserts the first emission from the flow is [NotStarted]. */
public suspend fun <T> Flow<AsyncResult<T>>.assertFirstIsNotStarted() {
  val firstEmission = first()
  if (firstEmission !is NotStarted) {
    throw AssertionError("Expected first emission to be NotStarted, but was $firstEmission")
  }
}

/** Asserts the first emission from the flow is [Incomplete]. */
public suspend fun <T> Flow<AsyncResult<T>>.assertFirstIsIncomplete() {
  val firstEmission = first()
  if (firstEmission !is Incomplete) {
    throw AssertionError("Expected first emission to be Incomplete, but was $firstEmission")
  }
}

// ============================================================================
// Collection helpers
// ============================================================================

/** Asserts the collection has at least one [Loading] result. */
public fun Assert<Iterable<AsyncResult<*>>>.hasAnyLoading(): Unit = given { actual ->
  if (!actual.any { it is Loading }) {
    expected("collection to have at least one Loading, but none found")
  }
}

/** Asserts the collection has at least one [Incomplete] result. */
public fun Assert<Iterable<AsyncResult<*>>>.hasAnyIncomplete(): Unit = given { actual ->
  if (!actual.any { it is Incomplete }) {
    expected("collection to have at least one Incomplete, but none found")
  }
}

/** Returns all [Error] instances from the collection for further assertions. */
public fun Assert<Iterable<AsyncResult<*>>>.allErrors(): Assert<List<Error>> = transform { actual ->
  actual.filterIsInstance<Error>()
}

/** Returns all error metadata instances of type [M] from the collection for further assertions. */
public inline fun <reified M> Assert<Iterable<AsyncResult<*>>>.allErrorMetadata(): Assert<List<M>> =
    transform { actual ->
      actual.filterIsInstance<Error>().mapNotNull { it.metadataOrNull<M>() }
    }

// ============================================================================
// Spread/zip helpers
// ============================================================================

/**
 * Asserts an [AsyncResult] containing a [Pair] by spreading it into two separate assertions.
 *
 * Example:
 * ```
 * assertThat(result).spreadsTo(
 *   first = { isSuccess().isEqualTo(42) },
 *   second = { isSuccess().isEqualTo("hello") }
 * )
 * ```
 */
public fun <A, B> Assert<AsyncResult<Pair<A, B>>>.spreadsTo(
    first: Assert<AsyncResult<A>>.() -> Unit,
    second: Assert<AsyncResult<B>>.() -> Unit
): Unit = given { actual ->
  val pair = when (actual) {
    is Success -> actual.value
    is Error -> {
      assertThat(AsyncResult.error<A>(actual.throwable, actual.errorId, actual.metadata)).first()
      assertThat(AsyncResult.error<B>(actual.throwable, actual.errorId, actual.metadata)).second()
      return@given
    }
    else -> expected("AsyncResult<Pair> to be terminal (Success or Error), but was $actual")
  }
  assertThat(AsyncResult.success(pair.first)).first()
  assertThat(AsyncResult.success(pair.second)).second()
}

/**
 * Asserts an [AsyncResult] containing a [Triple] by spreading it into three separate assertions.
 *
 * Example:
 * ```
 * assertThat(result).spreadsTo(
 *   first = { isSuccess().isEqualTo(42) },
 *   second = { isSuccess().isEqualTo("hello") },
 *   third = { isSuccess().isEqualTo(true) }
 * )
 * ```
 */
public fun <A, B, C> Assert<AsyncResult<Triple<A, B, C>>>.spreadsTo(
    first: Assert<AsyncResult<A>>.() -> Unit,
    second: Assert<AsyncResult<B>>.() -> Unit,
    third: Assert<AsyncResult<C>>.() -> Unit
): Unit = given { actual ->
  val triple = when (actual) {
    is Success -> actual.value
    is Error -> {
      assertThat(AsyncResult.error<A>(actual.throwable, actual.errorId, actual.metadata)).first()
      assertThat(AsyncResult.error<B>(actual.throwable, actual.errorId, actual.metadata)).second()
      assertThat(AsyncResult.error<C>(actual.throwable, actual.errorId, actual.metadata)).third()
      return@given
    }
    else -> expected("AsyncResult<Triple> to be terminal (Success or Error), but was $actual")
  }
  assertThat(AsyncResult.success(triple.first)).first()
  assertThat(AsyncResult.success(triple.second)).second()
  assertThat(AsyncResult.success(triple.third)).third()
}

// ============================================================================
// Unwrap/expect helpers
// ============================================================================

/**
 * Unwraps a [Success] result and returns its value for further assertions.
 * Throws if the result is not [Success].
 */
public fun <T> Assert<AsyncResult<T>>.unwrapSucceeds(): Assert<T> = transform { actual ->
  when (actual) {
    is Success -> actual.value
    else -> expected("AsyncResult to be Success for unwrap, but was $actual")
  }
}

/**
 * Asserts the [AsyncResult] is an [Error] with a throwable message matching [expected].
 */
public fun <T> Assert<AsyncResult<T>>.unwrapFailsWithMessage(expected: String): Unit =
    given { actual ->
      val error = when (actual) {
        is Error -> actual
        else -> throw AssertionError("Expected AsyncResult to be Error, but was $actual")
      }
      val actualMessage = error.throwable.message
      if (actualMessage != expected) {
        throw AssertionError(
            "Expected error message to be \"$expected\", but was \"$actualMessage\""
        )
      }
    }
>>>>>>> Stashed changes
