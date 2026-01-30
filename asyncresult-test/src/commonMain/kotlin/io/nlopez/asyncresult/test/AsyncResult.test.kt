package io.nlopez.asyncresult.test

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import io.nlopez.asyncresult.AsyncResult
import io.nlopez.asyncresult.Error
import io.nlopez.asyncresult.Incomplete
import io.nlopez.asyncresult.Loading
import io.nlopez.asyncresult.NotStarted
import io.nlopez.asyncresult.Success
import io.nlopez.asyncresult.getOrThrow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

fun <T> Assert<AsyncResult<T>>.isNotStarted(): Assert<NotStarted> = transform { actual ->
    when (actual) {
        is NotStarted -> actual
        else -> expected("AsyncResult to be NotStarted, but was $actual")
    }
}

fun <T> Assert<AsyncResult<T>>.isLoading(): Assert<Loading> = transform { actual ->
    when (actual) {
        is Loading -> actual
        else -> expected("AsyncResult to be Loading, but was $actual")
    }
}

fun <T> Assert<AsyncResult<T>>.isIncomplete(): Assert<Incomplete> = transform { actual ->
    when (actual) {
        is Incomplete -> actual
        else -> expected("AsyncResult to be Incomplete, but was $actual")
    }
}

fun <T> Assert<AsyncResult<T>>.isSuccess(): Assert<T> = transform { actual ->
    when (actual) {
        is Success -> actual.value
        else -> expected("AsyncResult to be Success, but was $actual")
    }
}

fun <T> Assert<AsyncResult<T>>.isSuccessEqualTo(expected: T) = given { actual ->
    assertThat(actual).isSuccess().isEqualTo(expected)
}

fun <T> Assert<AsyncResult<T>>.isError(): Assert<Error> = transform { actual ->
    when (actual) {
        is Error -> actual
        else -> expected("AsyncResult to be Error, but was $actual")
    }
}

inline fun <T, reified M> Assert<AsyncResult<T>>.isErrorWithMetadata(): Assert<M> = transform { actual ->
    when (actual) {
        is Error -> actual.metadataOrNull<M>()
            ?: expected("AsyncResult to be Error with metadata of type ${M::class.qualifiedName}")

        else -> expected("AsyncResult to be Error, but was $actual")
    }
}

inline fun <T, reified E> Assert<Error>.isThrowableEqualTo(expected: E) = given { actual ->
    assertThat(actual.throwable).isEqualTo(expected)
}

inline fun <T, reified M> Assert<Error>.isMetadataEqualTo(value: M) = given { actual ->
    assertThat(actual.metadataOrNull<M>()).isEqualTo(value)
}

inline fun <T, reified M> Assert<AsyncResult<T>>.isErrorWithMetadataEqualTo(value: M) = given { actual ->
    assertThat(actual).isErrorWithMetadata<T, M>().isEqualTo(value)
}

/**
 * Asserts the first terminal emission from the flow is [Success] with the given [expected] value.
 */
suspend fun <T> Flow<AsyncResult<T>>.assertSuccess(expected: T) {
    val result = getOrThrow()
    assertThat(result).isEqualTo(expected)
}

/**
 * Asserts the first terminal emission from the flow is [Error] and returns it for further checks.
 */
suspend fun <T> Flow<AsyncResult<T>>.assertError(): Error {
    val terminal = first { it is Success || it is Error }
    return (terminal as? Error)
        ?: expected("AsyncResult flow to emit Error, but was $terminal")
}
