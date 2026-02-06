// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

class AsyncResultRecoverTest {
  @Test
  fun `recover converts Error to Success`() {
    val error: AsyncResult<Int> = Error(Exception("Failed"))
    val result = error.recover { 42 }

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(42)
  }

  @Test
  fun `recover does not affect Success`() {
    val success: AsyncResult<Int> = Success(10)
    val result = success.recover { 42 }

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(10)
  }

  @Test
  fun `recover does not affect Loading`() {
    val loading: AsyncResult<Int> = Loading
    val result = loading.recover { 42 }

    assertThat(result).isInstanceOf<Loading>()
  }

  @Test
  fun `recover does not affect NotStarted`() {
    val notStarted: AsyncResult<Int> = NotStarted
    val result = notStarted.recover { 42 }

    assertThat(result).isInstanceOf<NotStarted>()
  }

  @Test
  fun `recover receives the error`() {
    val error = Error(Exception("Test"), metadata = "test-metadata")
    val result: AsyncResult<String> =
        error.recover { err ->
          "Error: ${err.throwable?.message}, metadata: ${err.metadataOrNull<String>()}"
        }

    assertThat(result).isInstanceOf<Success<String>>()
    assertThat((result as Success).value).isEqualTo("Error: Test, metadata: test-metadata")
  }

  // recoverIf tests

  sealed interface TestError

  data class NetworkError(val code: Int) : TestError

  data class ValidationError(val field: String) : TestError

  @Test
  fun `recoverIf converts Error to Success when metadata matches`() {
    val error: AsyncResult<Int> = ErrorWithMetadata(NetworkError(404))
    val result = error.recoverIf<Int, NetworkError> { -1 }

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(-1)
  }

  @Test
  fun `recoverIf does not convert Error when metadata does not match`() {
    val error: AsyncResult<Int> = ErrorWithMetadata(NetworkError(404))
    val result = error.recoverIf<Int, ValidationError> { -1 }

    assertThat(result).isInstanceOf<Error>()
  }

  @Test
  fun `recoverIf does not affect Error without metadata`() {
    val error: AsyncResult<Int> = Error(Exception("Failed"))
    val result = error.recoverIf<Int, NetworkError> { -1 }

    assertThat(result).isInstanceOf<Error>()
  }

  @Test
  fun `recoverIf does not affect Success`() {
    val success: AsyncResult<Int> = Success(10)
    val result = success.recoverIf<Int, NetworkError> { -1 }

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(10)
  }

  @Test
  fun `recoverIf does not affect Loading`() {
    val loading: AsyncResult<Int> = Loading
    val result = loading.recoverIf<Int, NetworkError> { -1 }

    assertThat(result).isInstanceOf<Loading>()
  }

  @Test
  fun `recoverIf does not affect NotStarted`() {
    val notStarted: AsyncResult<Int> = NotStarted
    val result = notStarted.recoverIf<Int, NetworkError> { -1 }

    assertThat(result).isInstanceOf<NotStarted>()
  }

  @Test
  fun `recoverIf receives the typed metadata`() {
    val error: AsyncResult<String> = ErrorWithMetadata(NetworkError(404))
    val result =
        error.recoverIf<String, NetworkError> { err -> "Recovered from network error: ${err.code}" }

    assertThat(result).isInstanceOf<Success<String>>()
    assertThat((result as Success).value).isEqualTo("Recovered from network error: 404")
  }

  @Test
  fun `recoverIf with multiple error types`() {
    val networkError: AsyncResult<Int> = ErrorWithMetadata(NetworkError(500))
    val validationError: AsyncResult<Int> = ErrorWithMetadata(ValidationError("email"))

    val recovered1 = networkError.recoverIf<Int, NetworkError> { 0 }
    val recovered2 = validationError.recoverIf<Int, NetworkError> { 0 }

    assertThat(recovered1).isInstanceOf<Success<Int>>()
    assertThat((recovered1 as Success).value).isEqualTo(0)
    assertThat(recovered2).isInstanceOf<Error>()
  }
}
