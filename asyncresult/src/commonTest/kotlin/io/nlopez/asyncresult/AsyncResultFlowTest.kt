// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class AsyncResultFlowTest {

  // ==============================
  // skipWhileLoading / filterNotLoading
  // ==============================

  @Test
  fun `skipWhileLoading filters out Loading emissions`() = runTest {
    val results = flowOf<AsyncResult<Int>>(Loading, Loading, Success(42), Loading, Error())
        .skipWhileLoading()
        .toList()

    assertThat(results).containsExactly(Success(42), Error())
  }

  @Test
  fun `skipWhileLoading passes through NotStarted, Success, and Error`() = runTest {
    val results = flowOf<AsyncResult<Int>>(NotStarted, Success(1), Error(), Success(2))
        .skipWhileLoading()
        .toList()

    assertThat(results).containsExactly(NotStarted, Success(1), Error(), Success(2))
  }

  @Test
  fun `skipWhileLoading returns empty flow when all Loading`() = runTest {
    val results = flowOf<AsyncResult<Int>>(Loading, Loading, Loading)
        .skipWhileLoading()
        .toList()

    assertThat(results).containsExactly()
  }

  @Test
  fun `filterNotLoading is alias for skipWhileLoading`() = runTest {
    val results = flowOf<AsyncResult<Int>>(Loading, Success(42), Loading)
        .filterNotLoading()
        .toList()

    assertThat(results).containsExactly(Success(42))
  }

  // ==============================
  // cacheLatestSuccess
  // ==============================

  @Test
  fun `cacheLatestSuccess passes through Loading when no Success cached`() = runTest {
    val results = flowOf<AsyncResult<Int>>(Loading, Loading)
        .cacheLatestSuccess()
        .toList()

    assertThat(results).containsExactly(Loading, Loading)
  }

  @Test
  fun `cacheLatestSuccess caches Success and emits it on subsequent Loading`() = runTest {
    val results = flowOf<AsyncResult<Int>>(Loading, Success(42), Loading, Loading)
        .cacheLatestSuccess()
        .toList()

    assertThat(results).containsExactly(Loading, Success(42), Success(42), Success(42))
  }

  @Test
  fun `cacheLatestSuccess updates cache on new Success`() = runTest {
    val results = flowOf<AsyncResult<Int>>(Success(1), Loading, Success(2), Loading)
        .cacheLatestSuccess()
        .toList()

    assertThat(results).containsExactly(Success(1), Success(1), Success(2), Success(2))
  }

  @Test
  fun `cacheLatestSuccess passes through Error without affecting cache`() = runTest {
    val error = Error(Throwable("test"))
    val results = flowOf<AsyncResult<Int>>(Success(42), Error(), Loading)
        .cacheLatestSuccess()
        .toList()

    assertThat(results).containsExactly(Success(42), Error(), Success(42))
  }

  @Test
  fun `cacheLatestSuccess passes through NotStarted without affecting cache`() = runTest {
    val results = flowOf<AsyncResult<Int>>(Success(42), NotStarted, Loading)
        .cacheLatestSuccess()
        .toList()

    assertThat(results).containsExactly(Success(42), NotStarted, Success(42))
  }

  @Test
  fun `cacheLatestSuccess handles empty flow`() = runTest {
    val results = flowOf<AsyncResult<Int>>()
        .cacheLatestSuccess()
        .toList()

    assertThat(results).containsExactly()
  }

  // ==============================
  // timeoutToError
  // ==============================

  @Test
  fun `timeoutToError emits Error when no terminal within timeout`() = runTest {
    val results = flow<AsyncResult<Int>> {
      emit(Loading)
      delay(500.milliseconds)
      emit(Loading)
    }
        .timeoutToError(100.milliseconds) { RuntimeException("Timeout!") }
        .toList()

    assertThat(results.size).isEqualTo(2)
    assertThat(results[0]).isEqualTo(Loading)
    assertThat(results[1]).isInstanceOf<Error>()
    assertThat((results[1] as Error).throwable).isInstanceOf<RuntimeException>()
  }

  @Test
  fun `timeoutToError does not emit Error when Success within timeout`() = runTest {
    val results = flow<AsyncResult<Int>> {
      emit(Loading)
      delay(10.milliseconds)
      emit(Success(42))
    }
        .timeoutToError(1.seconds) { RuntimeException("Timeout!") }
        .toList()

    assertThat(results).containsExactly(Loading, Success(42))
  }

  @Test
  fun `timeoutToError does not emit Error when Error within timeout`() = runTest {
    val originalError = Error(Throwable("original"))
    val results = flow<AsyncResult<Int>> {
      emit(Loading)
      delay(10.milliseconds)
      emit(originalError)
    }
        .timeoutToError(1.seconds) { RuntimeException("Timeout!") }
        .toList()

    assertThat(results).containsExactly(Loading, originalError)
  }

  @Test
  fun `timeoutToError emits timeout error for slow Success`() = runTest {
    val results = flow<AsyncResult<Int>> {
      emit(Loading)
      delay(500.milliseconds)
      emit(Success(42)) // This won't be collected due to timeout
    }
        .timeoutToError(100.milliseconds) { RuntimeException("Timed out") }
        .toList()

    assertThat(results.size).isEqualTo(2)
    assertThat(results[0]).isEqualTo(Loading)
    assertThat(results[1]).isInstanceOf<Error>()
  }

  @Test
  fun `timeoutToError with immediate Success`() = runTest {
    val results = flowOf<AsyncResult<Int>>(Success(42))
        .timeoutToError(100.milliseconds) { RuntimeException("Timeout!") }
        .toList()

    assertThat(results).containsExactly(Success(42))
  }

  // ==============================
  // retryOnError
  // ==============================

  @Test
  fun `retryOnError does not retry on Success`() = runTest {
    var attempts = 0
    val results = flow<AsyncResult<Int>> {
      attempts++
      emit(Success(42))
    }
        .retryOnError(maxRetries = 3)
        .toList()

    assertThat(attempts).isEqualTo(1)
    assertThat(results).containsExactly(Success(42))
  }

  @Test
  fun `retryOnError retries on Error up to maxRetries`() = runTest {
    var attempts = 0
    val results = flow<AsyncResult<Int>> {
      attempts++
      emit(Loading)
      emit(Error(Throwable("Error $attempts")))
    }
        .retryOnError(maxRetries = 3, delay = 0.milliseconds)
        .toList()

    assertThat(attempts).isEqualTo(4) // Initial + 3 retries
    // Each attempt emits Loading, plus the final Error
    assertThat(results.filter { it is Loading }.size).isEqualTo(4)
    assertThat(results.last()).isInstanceOf<Error>()
  }

  @Test
  fun `retryOnError succeeds after retry`() = runTest {
    var attempts = 0
    val results = flow<AsyncResult<Int>> {
      attempts++
      emit(Loading)
      if (attempts < 3) {
        emit(Error(Throwable("Error $attempts")))
      } else {
        emit(Success(42))
      }
    }
        .retryOnError(maxRetries = 5)
        .toList()

    assertThat(attempts).isEqualTo(3)
    assertThat(results.last()).isEqualTo(Success(42))
  }

  @Test
  fun `retryOnError respects predicate`() = runTest {
    var attempts = 0
    val results = flow<AsyncResult<Int>> {
      attempts++
      emit(Error(Throwable("NonRetryable")))
    }
        .retryOnError(
            maxRetries = 3,
            predicate = { it.throwable?.message == "Retryable" }
        )
        .toList()

    assertThat(attempts).isEqualTo(1)
    assertThat(results.size).isEqualTo(1)
    assertThat(results[0]).isInstanceOf<Error>()
  }

  @Test
  fun `retryOnError retries when predicate matches`() = runTest {
    var attempts = 0
    val results = flow<AsyncResult<Int>> {
      attempts++
      if (attempts < 3) {
        emit(Error(Throwable("Retryable")))
      } else {
        emit(Success(42))
      }
    }
        .retryOnError(
            maxRetries = 5,
            predicate = { it.throwable?.message == "Retryable" }
        )
        .toList()

    assertThat(attempts).isEqualTo(3)
    assertThat(results.last()).isEqualTo(Success(42))
  }

  @Test
  fun `retryOnError applies delay between retries`() = runTest {
    var attempts = 0
    var lastAttemptTime = 0L
    val delays = mutableListOf<Long>()

    val results = flow<AsyncResult<Int>> {
      val currentTime = testScheduler.currentTime
      if (attempts > 0) {
        delays.add(currentTime - lastAttemptTime)
      }
      lastAttemptTime = currentTime
      attempts++
      if (attempts < 3) {
        emit(Error(Throwable("Error")))
      } else {
        emit(Success(42))
      }
    }
        .retryOnError(maxRetries = 5, delay = 100.milliseconds)
        .toList()

    assertThat(attempts).isEqualTo(3)
    // Verify delays were applied (should be approximately 100ms each)
    assertThat(delays.all { it >= 100 }).isEqualTo(true)
  }

  @Test
  fun `retryOnError with zero maxRetries does not retry`() = runTest {
    var attempts = 0
    val results = flow<AsyncResult<Int>> {
      attempts++
      emit(Error(Throwable("Error")))
    }
        .retryOnError(maxRetries = 0)
        .toList()

    assertThat(attempts).isEqualTo(1)
    assertThat(results.size).isEqualTo(1)
    assertThat(results[0]).isInstanceOf<Error>()
  }

  @Test
  fun `retryOnError passes through NotStarted`() = runTest {
    val results = flowOf<AsyncResult<Int>>(NotStarted, Success(42))
        .retryOnError(maxRetries = 3)
        .toList()

    assertThat(results).containsExactly(NotStarted, Success(42))
  }

  @Test
  fun `retryOnError passes through Loading`() = runTest {
    val results = flowOf<AsyncResult<Int>>(Loading, Success(42))
        .retryOnError(maxRetries = 3)
        .toList()

    assertThat(results).containsExactly(Loading, Success(42))
  }
}
