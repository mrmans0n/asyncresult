// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import kotlin.test.Test
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class AsyncResultResultFlowTest {

  @Test
  fun `success emits Success value`() = runTest {
    val results = resultFlow<Int> { success(42) }.toList()
    assertThat(results).containsExactly(Success(42))
  }

  @Test
  fun `error emits Error instance`() = runTest {
    val err = Error(Throwable("boom"))
    val results = resultFlow<Int> { error(err) }.toList()
    assertThat(results).containsExactly(err)
  }

  @Test
  fun `loading emits Loading`() = runTest {
    val results = resultFlow<Int> { loading() }.toList()
    assertThat(results).containsExactly(Loading)
  }

  @Test
  fun `notStarted emits NotStarted`() = runTest {
    val results = resultFlow<Int> { notStarted() }.toList()
    assertThat(results).containsExactly(NotStarted)
  }

  @Test
  fun `multiple emissions in sequence`() = runTest {
    val results =
        resultFlow<Int> {
              loading()
              success(1)
              success(2)
            }
            .toList()
    assertThat(results).containsExactly(Loading, Success(1), Success(2))
  }

  @Test
  fun `error with Throwable and errorId`() = runTest {
    val throwable = RuntimeException("fail")
    val errorId = ErrorId("err-1")
    val results = resultFlow<Int> { error(throwable, errorId) }.toList()

    assertThat(results.size).isEqualTo(1)
    val err = results[0] as Error
    assertThat(err.throwable).isEqualTo(throwable)
    assertThat(err.errorId).isEqualTo(errorId)
  }

  @Test
  fun `error with Throwable only`() = runTest {
    val throwable = RuntimeException("fail")
    val results = resultFlow<Int> { error(throwable) }.toList()

    assertThat(results).containsExactly(Error(throwable))
  }

  @Test
  fun `errorWithMetadata emits Error with typed metadata`() = runTest {
    data class MyMeta(val code: Int)
    val results = resultFlow<Int> { errorWithMetadata(MyMeta(404)) }.toList()

    assertThat(results.size).isEqualTo(1)
    val err = results[0] as Error
    assertThat(err.metadataOrNull<MyMeta>()).isEqualTo(MyMeta(404))
  }

  @Test
  fun `errorWithMetadata with errorId`() = runTest {
    data class MyMeta(val code: Int)
    val errorId = ErrorId("not-found")
    val results = resultFlow<Int> { errorWithMetadata(MyMeta(404), errorId) }.toList()

    assertThat(results.size).isEqualTo(1)
    val err = results[0] as Error
    assertThat(err.metadataOrNull<MyMeta>()).isEqualTo(MyMeta(404))
    assertThat(err.errorId).isEqualTo(errorId)
  }

  @Test
  fun `uncaught exception emitted as Error`() = runTest {
    val exception = IllegalStateException("unexpected")
    val results = resultFlow<Int> { throw exception }.toList()

    assertThat(results.size).isEqualTo(1)
    val err = results[0] as Error
    assertThat(err.throwable).isNotNull()
    assertThat(err.throwable).isEqualTo(exception)
  }

  @Test
  fun `CancellationException propagates and is not caught`() = runTest {
    var caught = false
    try {
      resultFlow<Int> { throw kotlinx.coroutines.CancellationException("cancelled") }.toList()
    } catch (e: kotlinx.coroutines.CancellationException) {
      caught = true
      assertThat(e.message).isEqualTo("cancelled")
    }
    assertThat(caught).isEqualTo(true)
  }

  @Test
  fun `empty block emits nothing`() = runTest {
    val results = resultFlow<Int> {}.toList()
    assertThat(results).containsExactly()
  }

  @Test
  fun `uncaught exception after partial emissions preserves earlier values`() = runTest {
    val exception = IllegalStateException("mid-stream failure")
    val results =
        resultFlow<Int> {
              loading()
              success(42)
              throw exception
            }
            .toList()

    assertThat(results.size).isEqualTo(3)
    assertThat(results[0]).isEqualTo(Loading)
    assertThat(results[1]).isEqualTo(Success(42))
    val err = results[2] as Error
    assertThat(err.throwable).isEqualTo(exception)
  }

  @Test
  fun `mixed emissions loading then error then success`() = runTest {
    val results =
        resultFlow<Int> {
              loading()
              error(Error(Throwable("oops")))
              success(99)
            }
            .toList()
    assertThat(results.size).isEqualTo(3)
    assertThat(results[0]).isEqualTo(Loading)
    assertThat(results[1]).isInstanceOf<Error>()
    assertThat(results[2]).isEqualTo(Success(99))
  }
}
