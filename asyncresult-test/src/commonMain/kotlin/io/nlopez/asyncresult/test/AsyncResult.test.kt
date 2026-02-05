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

/** Asserts the [AsyncResult] is an [Error] with a non-null throwable and returns it. */
public fun <T> Assert<AsyncResult<T>>.isErrorWithThrowable(): Assert<Throwable> =
    transform { actual ->
      when (actual) {
        is Error ->
            actual.throwable
                ?: expected("AsyncResult to be Error with throwable, but throwable was null")

        else -> expected("AsyncResult to be Error, but was $actual")
      }
    }

/** Asserts the [AsyncResult] is an [Error] with a throwable of the specified type. */
public inline fun <T, reified E : Throwable> Assert<AsyncResult<T>>.isErrorWithThrowableOfType():
    Assert<E> = transform { actual ->
  when (actual) {
    is Error -> {
      val throwable =
          actual.throwable
              ?: expected("AsyncResult to be Error with throwable, but throwable was null")
      if (throwable is E) {
        throwable
      } else {
        expected(
            "AsyncResult to be Error with throwable of type ${E::class.simpleName}, " +
                "but was ${throwable::class.simpleName}")
      }
    }

    else -> expected("AsyncResult to be Error, but was $actual")
  }
}

/** Asserts the [AsyncResult] is an [Error] with a throwable message matching [expected]. */
public fun <T> Assert<AsyncResult<T>>.isErrorWithThrowableMessage(expected: String): Unit =
    given { actual ->
      when (actual) {
        is Error -> {
          val throwable =
              actual.throwable
                  ?: throw AssertionError(
                      "Expected Error to have throwable, but throwable was null")
          val actualMessage = throwable.message
          if (actualMessage != expected) {
            throw AssertionError(
                "Expected throwable message to be \"$expected\", but was \"$actualMessage\"")
          }
        }

        else -> throw AssertionError("Expected AsyncResult to be Error, but was $actual")
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
public suspend inline fun <T, reified E : Throwable> Flow<AsyncResult<T>>
    .assertErrorWithThrowableOfType(): E {
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

/** Asserts the first terminal emission from the flow is [Error] with the given [ErrorId]. */
public suspend fun <T> Flow<AsyncResult<T>>.assertErrorWithId(expected: ErrorId) {
  val error = assertError()
  val actualId = error.errorId
  if (actualId != expected) {
    throw AssertionError("Expected Error to have errorId $expected, but was $actualId")
  }
}
