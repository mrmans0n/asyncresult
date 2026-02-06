// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

class AsyncResultBimapTest {
  @Test
  fun `bimap transforms Success value`() {
    val success: AsyncResult<Int> = Success(10)
    val result =
        success.bimap(
            success = { it * 2 },
            error = { it.withMetadata("error-metadata") },
        )

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(20)
  }

  @Test
  fun `bimap transforms Error`() {
    val error: AsyncResult<Int> = Error(Exception("Failed"))
    val result =
        error.bimap(
            success = { it * 2 },
            error = { it.withMetadata("enriched-error") },
        )

    assertThat(result).isInstanceOf<Error>()
    assertThat((result as Error).metadataOrNull<String>()).isEqualTo("enriched-error")
  }

  @Test
  fun `bimap does not transform Loading`() {
    val loading: AsyncResult<Int> = Loading
    val result =
        loading.bimap(
            success = { it * 2 },
            error = { it.withMetadata("error") },
        )

    assertThat(result).isInstanceOf<Loading>()
  }

  @Test
  fun `bimap does not transform NotStarted`() {
    val notStarted: AsyncResult<Int> = NotStarted
    val result =
        notStarted.bimap(
            success = { it * 2 },
            error = { it.withMetadata("error") },
        )

    assertThat(result).isInstanceOf<NotStarted>()
  }

  @Test
  fun `bimap can change success type`() {
    val success: AsyncResult<Int> = Success(10)
    val result =
        success.bimap(
            success = { "Value: $it" },
            error = { it },
        )

    assertThat(result).isInstanceOf<Success<String>>()
    assertThat((result as Success).value).isEqualTo("Value: 10")
  }

  @Test
  fun `bimap can add metadata to error`() {
    val error: AsyncResult<Int> = Error(Exception("Failed"))
    val result =
        error.bimap(
            success = { it * 2 },
            error = { it.withMetadata(ErrorContext("network", 500)) },
        )

    assertThat(result).isInstanceOf<Error>()
    val metadata = (result as Error).metadataOrNull<ErrorContext>()
    assertThat(metadata?.type).isEqualTo("network")
    assertThat(metadata?.code).isEqualTo(500)
  }

  @Test
  fun `bimap can transform error throwable`() {
    val error: AsyncResult<Int> = Error(Exception("Original"))
    val result =
        error.bimap(
            success = { it * 2 },
            error = { Error(Exception("Transformed: ${it.throwable?.message}")) },
        )

    assertThat(result).isInstanceOf<Error>()
    assertThat((result as Error).throwable?.message).isEqualTo("Transformed: Original")
  }

  @Test
  fun `bimap allows independent success and error transformations`() {
    val success: AsyncResult<Int> = Success(5)
    val error: AsyncResult<Int> = Error(Exception("Failed"))

    val transformedSuccess =
        success.bimap(
            success = { it * 10 },
            error = { it.withMetadata("error") },
        )

    val transformedError =
        error.bimap(
            success = { it * 10 },
            error = { it.withMetadata("transformed-error") },
        )

    assertThat(transformedSuccess).isInstanceOf<Success<Int>>()
    assertThat((transformedSuccess as Success).value).isEqualTo(50)

    assertThat(transformedError).isInstanceOf<Error>()
    assertThat((transformedError as Error).metadataOrNull<String>()).isEqualTo("transformed-error")
  }

  @Test
  fun `bimap preserves error id when transforming`() {
    val errorId = ErrorId("test-error-123")
    val error: AsyncResult<Int> = ErrorWithId(errorId, "original-metadata")
    val result =
        error.bimap(
            success = { it * 2 },
            error = { it.withMetadata("new-metadata") },
        )

    assertThat(result).isInstanceOf<Error>()
    assertThat((result as Error).errorId).isEqualTo(errorId)
    assertThat(result.metadataOrNull<String>()).isEqualTo("new-metadata")
  }

  @Test
  fun `bimap can chain with other operations`() {
    val success: AsyncResult<Int> = Success(10)
    val result =
        success
            .bimap(
                success = { it * 2 },
                error = { it },
            )
            .mapSuccess { it + 5 }

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(25)
  }

  @Test
  fun `bimap with complex transformations`() {
    data class UserInput(val name: String, val age: Int)
    data class ValidatedUser(val name: String, val age: Int, val isAdult: Boolean)

    val input: AsyncResult<UserInput> = Success(UserInput("Alice", 25))
    val result =
        input.bimap(
            success = { ValidatedUser(it.name, it.age, it.age >= 18) },
            error = { it.withMetadata("validation-failed") },
        )

    assertThat(result).isInstanceOf<Success<ValidatedUser>>()
    val user = (result as Success).value
    assertThat(user.name).isEqualTo("Alice")
    assertThat(user.age).isEqualTo(25)
    assertThat(user.isAdult).isEqualTo(true)
  }

  // Helper class for tests
  data class ErrorContext(val type: String, val code: Int)
}
