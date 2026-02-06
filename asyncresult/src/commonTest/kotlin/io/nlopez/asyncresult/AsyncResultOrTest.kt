// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

private sealed interface ApiError

private data class NotFoundError(val id: String) : ApiError

private data class ServerError(val code: Int) : ApiError

class AsyncResultOrTest {
  @Test
  fun `or returns other when this is Error`() {
    val error: AsyncResult<Int> = Error(Exception("Failed"))
    val fallback: AsyncResult<Int> = Success(42)
    val result = error.or(fallback)

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(42)
  }

  @Test
  fun `or returns this when this is Success`() {
    val success: AsyncResult<Int> = Success(10)
    val fallback: AsyncResult<Int> = Success(42)
    val result = success.or(fallback)

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(10)
  }

  @Test
  fun `or returns this when this is Loading`() {
    val loading: AsyncResult<Int> = Loading
    val fallback: AsyncResult<Int> = Success(42)
    val result = loading.or(fallback)

    assertThat(result).isInstanceOf<Loading>()
  }

  @Test
  fun `or returns this when this is NotStarted`() {
    val notStarted: AsyncResult<Int> = NotStarted
    val fallback: AsyncResult<Int> = Success(42)
    val result = notStarted.or(fallback)

    assertThat(result).isInstanceOf<NotStarted>()
  }

  @Test
  fun `or evaluates other eagerly`() {
    var evaluated = false
    val success: AsyncResult<Int> = Success(10)
    val fallback: AsyncResult<Int> =
        Success(42).also { evaluated = true } // This gets evaluated immediately

    success.or(fallback)

    assertThat(evaluated).isEqualTo(true)
  }

  @Test
  fun `or can chain multiple fallbacks`() {
    val error1: AsyncResult<Int> = Error(Exception("First"))
    val error2: AsyncResult<Int> = Error(Exception("Second"))
    val success: AsyncResult<Int> = Success(42)

    val result = error1.or(error2).or(success)

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(42)
  }

  // orElse tests

  @Test
  fun `orElse returns transform result when this is Error`() {
    val error: AsyncResult<Int> = Error(Exception("Failed"))
    val result = error.orElse { Success(42) }

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(42)
  }

  @Test
  fun `orElse returns this when this is Success`() {
    val success: AsyncResult<Int> = Success(10)
    val result = success.orElse { Success(42) }

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(10)
  }

  @Test
  fun `orElse returns this when this is Loading`() {
    val loading: AsyncResult<Int> = Loading
    val result = loading.orElse { Success(42) }

    assertThat(result).isInstanceOf<Loading>()
  }

  @Test
  fun `orElse returns this when this is NotStarted`() {
    val notStarted: AsyncResult<Int> = NotStarted
    val result = notStarted.orElse { Success(42) }

    assertThat(result).isInstanceOf<NotStarted>()
  }

  @Test
  fun `orElse evaluates transform lazily`() {
    var evaluated = false
    val success: AsyncResult<Int> = Success(10)

    success.orElse {
      evaluated = true
      Success(42)
    }

    assertThat(evaluated).isEqualTo(false)
  }

  @Test
  fun `orElse receives the error`() {
    val error = Error(Exception("Test"), metadata = "test-metadata")
    val result: AsyncResult<String> =
        error.orElse { err -> Success("Fallback for: ${err.throwable?.message}") }

    assertThat(result).isInstanceOf<Success<String>>()
    assertThat((result as Success).value).isEqualTo("Fallback for: Test")
  }

  @Test
  fun `orElse can return another Error`() {
    val error: AsyncResult<Int> = Error(Exception("First"))
    val result = error.orElse { Error(Exception("Fallback also failed")) }

    assertThat(result).isInstanceOf<Error>()
    assertThat((result as Error).throwable?.message).isEqualTo("Fallback also failed")
  }

  @Test
  fun `orElse can chain multiple fallbacks`() {
    val error1: AsyncResult<Int> = Error(Exception("First"))

    val result =
        error1
            .orElse { Error(Exception("Second")) }
            .orElse { Error(Exception("Third")) }
            .orElse { Success(42) }

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(42)
  }

  @Test
  fun `orElse can return different result types based on error`() {
    val notFoundError: AsyncResult<String> = ErrorWithMetadata(NotFoundError("123"))
    val serverError: AsyncResult<String> = ErrorWithMetadata(ServerError(500))

    val result1 =
        notFoundError.orElse { err ->
          when (val metadata = err.metadataOrNull<ApiError>()) {
            is NotFoundError -> Success("Default for ${metadata.id}")
            is ServerError -> Error(Exception("Server error"))
            null -> Error(Exception("Unknown error"))
          }
        }

    val result2 =
        serverError.orElse { err ->
          when (val metadata = err.metadataOrNull<ApiError>()) {
            is NotFoundError -> Success("Default")
            is ServerError -> Error(Exception("Server error: ${metadata.code}"))
            null -> Error(Exception("Unknown error"))
          }
        }

    assertThat(result1).isInstanceOf<Success<String>>()
    assertThat((result1 as Success).value).isEqualTo("Default for 123")

    assertThat(result2).isInstanceOf<Error>()
    assertThat((result2 as Error).throwable?.message).isEqualTo("Server error: 500")
  }
}
