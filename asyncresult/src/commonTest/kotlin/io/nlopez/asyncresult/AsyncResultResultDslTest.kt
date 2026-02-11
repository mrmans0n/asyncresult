// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class AsyncResultResultDslTest {
  @Test
  fun `bind with Success extracts value`() {
    val result = result { Success(10).bind() }
    assertThat(result).isEqualTo(Success(10))
  }

  @Test
  fun `bind with Error short-circuits and returns Error`() {
    val throwable = Throwable("boom")
    val result = result { Error(throwable).bind() }
    assertThat(result).isEqualTo(Error(throwable))
  }

  @Test
  fun `bind with Loading short-circuits and returns Loading`() {
    val result = result { Loading.bind() }
    assertThat(result).isEqualTo(Loading)
  }

  @Test
  fun `bind with NotStarted short-circuits and returns NotStarted`() {
    val result = result { NotStarted.bind() }
    assertThat(result).isEqualTo(NotStarted)
  }

  @Test
  fun `loading short-circuits to Loading`() {
    val result = result<Int> { loading() }
    assertThat(result).isEqualTo(Loading)
  }

  @Test
  fun `notStarted short-circuits to NotStarted`() {
    val result = result<Int> { notStarted() }
    assertThat(result).isEqualTo(NotStarted)
  }

  @Test
  fun `error short-circuits to Error with given Throwable`() {
    val throwable = Throwable("boom")
    val result = result<Int> { error(throwable) }
    assertThat(result).isEqualTo(Error(throwable))
  }

  @Test
  fun `error with Throwable and errorId short-circuits with both`() {
    val throwable = Throwable("boom")
    val errorId = ErrorId("err-123")
    val result = result<Int> { error(throwable, errorId) }
    assertThat(result).isEqualTo(Error(throwable, errorId = errorId))
  }

  @Test
  fun `error short-circuits with given Error instance`() {
    val throwable = Throwable("boom")
    val errorId = ErrorId("test-error")
    val errorInstance = Error(throwable, metadata = "metadata", errorId = errorId)
    val result = result<Int> { error(errorInstance) }
    assertThat(result).isEqualTo(errorInstance)
  }

  @Test
  fun `errorWithMetadata short-circuits with Error containing typed metadata`() {
    data class MyMetadata(val code: Int, val message: String)
    val metadata = MyMetadata(404, "Not Found")
    val errorId = ErrorId("not-found")
    val result = result<Int> { errorWithMetadata(metadata, errorId) }
    
    assertThat(result).isEqualTo(ErrorWithMetadata(metadata, errorId))
    assertThat((result as Error).metadataOrNull<MyMetadata>()).isEqualTo(metadata)
    assertThat(result.errorId).isEqualTo(errorId)
  }

  @Test
  fun `errorWithMetadata without errorId works`() {
    val metadata = "Simple error"
    val result = result<Int> { errorWithMetadata(metadata) }
    
    assertThat((result as Error).metadataOrNull<String>()).isEqualTo(metadata)
    assertThat(result.errorId).isEqualTo(null)
  }

  @Test
  fun `ensure true continues and ensure false short-circuits`() {
    val success = result {
      ensure(condition = true) { IllegalArgumentException("should not happen") }
      42
    }
    assertThat(success).isEqualTo(Success(42))

    val throwable = IllegalStateException("invalid")
    val failure =
        result<Int> {
          ensure(condition = false) { throwable }
          42
        }
    assertThat(failure).isEqualTo(Error(throwable))
  }

  @Test
  fun `ensureNotNull returns value and null short-circuits`() {
    val success = result {
      val value = ensureNotNull("hello") { IllegalStateException("null") }
      value.length
    }
    assertThat(success).isEqualTo(Success(5))

    val throwable = IllegalStateException("null")
    val failure = result<Int> { ensureNotNull(null as String?) { throwable }.length }
    assertThat(failure).isEqualTo(Error(throwable))
  }

  @Test
  fun `nested result blocks work correctly`() {
    val result = result {
      val first = Success(1).bind()
      val second = result { Success(2).bind() }.bind()
      first + second
    }

    assertThat(result).isEqualTo(Success(3))
  }

  @Test
  fun `normal exceptions inside block are not caught and propagate`() {
    assertFailsWith<IllegalStateException> { result<Int> { throw IllegalStateException("boom") } }
  }

  @Test
  fun `suspend calls inside result from suspend context work`() = runTest {
    suspend fun plusOne(value: Int): Int = value + 1

    val result = result {
      val value = Success(1).bind()
      plusOne(value)
    }

    assertThat(result).isEqualTo(Success(2))
  }

  @Test
  fun `non-suspend usage compiles and works`() {
    val result = result { Success(2).bind() * 2 }
    assertThat(result).isEqualTo(Success(4))
  }

  @Test
  fun `multiple binds short-circuit on first non-success`() {
    val throwable = Throwable("boom")
    val result = result {
      val first = Success(1).bind()
      Error(throwable).bind()
      val second = Success(2).bind()
      first + second
    }

    assertThat(result).isEqualTo(Error(throwable))
  }

  @Test
  fun `result with all Success binds returns computed Success`() {
    val result = result {
      val first = Success(2).bind()
      val second = Success(3).bind()
      first * second
    }

    assertThat(result).isEqualTo(Success(6))
  }
}
