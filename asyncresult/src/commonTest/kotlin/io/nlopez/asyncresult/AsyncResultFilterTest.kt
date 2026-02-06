// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

class AsyncResultFilterTest {
  // toErrorIf tests

  @Test
  fun `toErrorIf converts Success to Error when predicate is true`() {
    val success: AsyncResult<Int> = Success(5)
    val result = success.toErrorIf(predicate = { it < 10 })

    assertThat(result).isInstanceOf<Error>()
  }

  @Test
  fun `toErrorIf keeps Success when predicate is false`() {
    val success: AsyncResult<Int> = Success(15)
    val result = success.toErrorIf(predicate = { it < 10 })

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(15)
  }

  @Test
  fun `toErrorIf does not affect Error`() {
    val error: AsyncResult<Int> = Error(Exception("Failed"))
    val result = error.toErrorIf(predicate = { it < 10 })

    assertThat(result).isInstanceOf<Error>()
  }

  @Test
  fun `toErrorIf does not affect Loading`() {
    val loading: AsyncResult<Int> = Loading
    val result = loading.toErrorIf(predicate = { it < 10 })

    assertThat(result).isInstanceOf<Loading>()
  }

  @Test
  fun `toErrorIf does not affect NotStarted`() {
    val notStarted: AsyncResult<Int> = NotStarted
    val result = notStarted.toErrorIf(predicate = { it < 10 })

    assertThat(result).isInstanceOf<NotStarted>()
  }

  @Test
  fun `toErrorIf uses custom error function`() {
    val success: AsyncResult<Int> = Success(5)
    val result =
        success.toErrorIf(
            error = { value -> ErrorWithMetadata("Value $value is too small") },
            predicate = { it < 10 },
        )

    assertThat(result).isInstanceOf<Error>()
    assertThat((result as Error).metadataOrNull<String>()).isEqualTo("Value 5 is too small")
  }

  @Test
  fun `toErrorIf uses default error when not provided`() {
    val success: AsyncResult<Int> = Success(5)
    val result = success.toErrorIf(predicate = { it < 10 })

    assertThat(result).isInstanceOf<Error>()
    assertThat((result as Error).throwable).isEqualTo(null)
    assertThat(result.metadataOrNull<Any>()).isEqualTo(null)
  }

  @Test
  fun `toErrorIf with complex predicate`() {
    data class User(val name: String, val age: Int)
    val user: AsyncResult<User> = Success(User("Alice", 16))

    val result =
        user.toErrorIf(
            error = { ErrorWithMetadata("User must be 18 or older") },
            predicate = { it.age < 18 },
        )

    assertThat(result).isInstanceOf<Error>()
  }

  @Test
  fun `toErrorIf can validate ranges`() {
    val value: AsyncResult<Int> = Success(150)
    val result =
        value.toErrorIf(
            error = { ErrorWithMetadata("Value out of range") },
            predicate = { it < 0 || it > 100 },
        )

    assertThat(result).isInstanceOf<Error>()
  }

  // toErrorUnless tests

  @Test
  fun `toErrorUnless converts Success to Error when predicate is false`() {
    val success: AsyncResult<Int> = Success(5)
    val result = success.toErrorUnless(predicate = { it >= 10 })

    assertThat(result).isInstanceOf<Error>()
  }

  @Test
  fun `toErrorUnless keeps Success when predicate is true`() {
    val success: AsyncResult<Int> = Success(15)
    val result = success.toErrorUnless(predicate = { it >= 10 })

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(15)
  }

  @Test
  fun `toErrorUnless does not affect Error`() {
    val error: AsyncResult<Int> = Error(Exception("Failed"))
    val result = error.toErrorUnless(predicate = { it >= 10 })

    assertThat(result).isInstanceOf<Error>()
  }

  @Test
  fun `toErrorUnless does not affect Loading`() {
    val loading: AsyncResult<Int> = Loading
    val result = loading.toErrorUnless(predicate = { it >= 10 })

    assertThat(result).isInstanceOf<Loading>()
  }

  @Test
  fun `toErrorUnless does not affect NotStarted`() {
    val notStarted: AsyncResult<Int> = NotStarted
    val result = notStarted.toErrorUnless(predicate = { it >= 10 })

    assertThat(result).isInstanceOf<NotStarted>()
  }

  @Test
  fun `toErrorUnless uses custom error function`() {
    val success: AsyncResult<Int> = Success(5)
    val result =
        success.toErrorUnless(
            error = { value -> ErrorWithMetadata("Value $value is too small") },
            predicate = { it >= 10 },
        )

    assertThat(result).isInstanceOf<Error>()
    assertThat((result as Error).metadataOrNull<String>()).isEqualTo("Value 5 is too small")
  }

  @Test
  fun `toErrorUnless uses default error when not provided`() {
    val success: AsyncResult<Int> = Success(5)
    val result = success.toErrorUnless(predicate = { it >= 10 })

    assertThat(result).isInstanceOf<Error>()
    assertThat((result as Error).throwable).isEqualTo(null)
    assertThat(result.metadataOrNull<Any>()).isEqualTo(null)
  }

  @Test
  fun `toErrorUnless is inverse of toErrorIf`() {
    val success: AsyncResult<Int> = Success(15)

    val resultIf = success.toErrorIf(predicate = { it < 10 })
    val resultUnless = success.toErrorUnless(predicate = { it >= 10 })

    assertThat(resultIf).isInstanceOf<Success<Int>>()
    assertThat(resultUnless).isInstanceOf<Success<Int>>()

    val success2: AsyncResult<Int> = Success(5)

    val resultIf2 = success2.toErrorIf(predicate = { it < 10 })
    val resultUnless2 = success2.toErrorUnless(predicate = { it >= 10 })

    assertThat(resultIf2).isInstanceOf<Error>()
    assertThat(resultUnless2).isInstanceOf<Error>()
  }

  @Test
  fun `toErrorUnless with complex validation`() {
    data class User(val name: String, val age: Int, val email: String)
    val user: AsyncResult<User> = Success(User("Alice", 25, ""))

    val result =
        user.toErrorUnless(
            error = { ErrorWithMetadata("Email is required") },
            predicate = { it.email.isNotEmpty() },
        )

    assertThat(result).isInstanceOf<Error>()
    assertThat((result as Error).metadataOrNull<String>()).isEqualTo("Email is required")
  }

  @Test
  fun `toErrorUnless can validate non-null values`() {
    val value: AsyncResult<String?> = Success(null)
    val result =
        value.toErrorUnless(
            error = { ErrorWithMetadata("Value must not be null") },
            predicate = { it != null },
        )

    assertThat(result).isInstanceOf<Error>()
  }

  // Chaining and real-world tests

  @Test
  fun `toErrorIf can chain multiple validations`() {
    val value: AsyncResult<Int> = Success(50)
    val result =
        value
            .toErrorIf(
                error = { ErrorWithMetadata("Too small") },
                predicate = { it < 10 },
            )
            .toErrorIf(
                error = { ErrorWithMetadata("Too large") },
                predicate = { it > 100 },
            )

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(50)
  }

  @Test
  fun `toErrorIf stops at first validation failure`() {
    val value: AsyncResult<Int> = Success(5)
    val result =
        value
            .toErrorIf(
                error = { ErrorWithMetadata("Too small") },
                predicate = { it < 10 },
            )
            .toErrorIf(
                error = { ErrorWithMetadata("Too large") },
                predicate = { it > 100 },
            )

    assertThat(result).isInstanceOf<Error>()
    assertThat((result as Error).metadataOrNull<String>()).isEqualTo("Too small")
  }

  @Test
  fun `toErrorUnless can validate business rules`() {
    data class Order(val items: List<String>, val total: Double)
    val order: AsyncResult<Order> = Success(Order(items = listOf("item1"), total = 150.0))

    val result =
        order
            .toErrorUnless(
                error = { ErrorWithMetadata("Order must have items") },
                predicate = { it.items.isNotEmpty() },
            )
            .toErrorUnless(
                error = { ErrorWithMetadata("Order total too high") },
                predicate = { it.total <= 1000.0 },
            )

    assertThat(result).isInstanceOf<Success<Order>>()
  }

  @Test
  fun `toErrorIf and toErrorUnless can be combined`() {
    val value: AsyncResult<Int> = Success(50)
    val result =
        value
            .toErrorIf(
                error = { ErrorWithMetadata("Value is negative") },
                predicate = { it < 0 },
            )
            .toErrorUnless(
                error = { ErrorWithMetadata("Value must be in range 1-100") },
                predicate = { it in 1..100 },
            )

    assertThat(result).isInstanceOf<Success<Int>>()
    assertThat((result as Success).value).isEqualTo(50)
  }
}
