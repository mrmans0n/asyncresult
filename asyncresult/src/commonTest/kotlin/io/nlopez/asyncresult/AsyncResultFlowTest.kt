// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
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
    val results =
        flowOf<AsyncResult<Int>>(Loading, Loading, Success(42), Loading, Error())
            .skipWhileLoading()
            .toList()

    assertThat(results).containsExactly(Success(42), Error())
  }

  @Test
  fun `skipWhileLoading passes through NotStarted and Success and Error`() = runTest {
    val results =
        flowOf<AsyncResult<Int>>(NotStarted, Success(1), Error(), Success(2))
            .skipWhileLoading()
            .toList()

    assertThat(results).containsExactly(NotStarted, Success(1), Error(), Success(2))
  }

  @Test
  fun `skipWhileLoading returns empty flow when all Loading`() = runTest {
    val results = flowOf<AsyncResult<Int>>(Loading, Loading, Loading).skipWhileLoading().toList()

    assertThat(results).containsExactly()
  }

  @Test
  fun `filterNotLoading is alias for skipWhileLoading`() = runTest {
    val results =
        flowOf<AsyncResult<Int>>(Loading, Success(42), Loading).filterNotLoading().toList()

    assertThat(results).containsExactly(Success(42))
  }

  // ==============================
  // cacheLatestSuccess
  // ==============================

  @Test
  fun `cacheLatestSuccess passes through Loading when no Success cached`() = runTest {
    val results = flowOf<AsyncResult<Int>>(Loading, Loading).cacheLatestSuccess().toList()

    assertThat(results).containsExactly(Loading, Loading)
  }

  @Test
  fun `cacheLatestSuccess caches Success and emits it on subsequent Loading`() = runTest {
    val results =
        flowOf<AsyncResult<Int>>(Loading, Success(42), Loading, Loading)
            .cacheLatestSuccess()
            .toList()

    assertThat(results).containsExactly(Loading, Success(42), Success(42), Success(42))
  }

  @Test
  fun `cacheLatestSuccess updates cache on new Success`() = runTest {
    val results =
        flowOf<AsyncResult<Int>>(Success(1), Loading, Success(2), Loading)
            .cacheLatestSuccess()
            .toList()

    assertThat(results).containsExactly(Success(1), Success(1), Success(2), Success(2))
  }

  @Test
  fun `cacheLatestSuccess passes through Error without affecting cache`() = runTest {
    val error = Error(Throwable("test"))
    val results =
        flowOf<AsyncResult<Int>>(Success(42), Error(), Loading).cacheLatestSuccess().toList()

    assertThat(results).containsExactly(Success(42), Error(), Success(42))
  }

  @Test
  fun `cacheLatestSuccess passes through NotStarted without affecting cache`() = runTest {
    val results =
        flowOf<AsyncResult<Int>>(Success(42), NotStarted, Loading).cacheLatestSuccess().toList()

    assertThat(results).containsExactly(Success(42), NotStarted, Success(42))
  }

  @Test
  fun `cacheLatestSuccess handles empty flow`() = runTest {
    val results = flowOf<AsyncResult<Int>>().cacheLatestSuccess().toList()

    assertThat(results).containsExactly()
  }

  // ==============================
  // timeoutToError
  // ==============================

  @Test
  fun `timeoutToError emits Error when no terminal within timeout`() = runTest {
    val results =
        flow<AsyncResult<Int>> {
              emit(Loading)
              delay(500.milliseconds)
              emit(Loading)
            }
            .timeoutToError(100.milliseconds) { Error(RuntimeException("Timeout!")) }
            .toList()

    assertThat(results.size).isEqualTo(2)
    assertThat(results[0]).isEqualTo(Loading)
    val error = results[1] as Error
    assertThat(error.throwable).isNotNull()
    assertThat(error.throwable!!.message).isEqualTo("Timeout!")
  }

  @Test
  fun `timeoutToError does not emit Error when Success within timeout`() = runTest {
    val results =
        flow<AsyncResult<Int>> {
              emit(Loading)
              delay(10.milliseconds)
              emit(Success(42))
            }
            .timeoutToError(1.seconds) { Error(RuntimeException("Timeout!")) }
            .toList()

    assertThat(results).containsExactly(Loading, Success(42))
  }

  @Test
  fun `timeoutToError does not emit Error when Error within timeout`() = runTest {
    val originalError = Error(Throwable("original"))
    val results =
        flow<AsyncResult<Int>> {
              emit(Loading)
              delay(10.milliseconds)
              emit(originalError)
            }
            .timeoutToError(1.seconds) { Error(RuntimeException("Timeout!")) }
            .toList()

    assertThat(results).containsExactly(Loading, originalError)
  }

  @Test
  fun `timeoutToError emits timeout error for slow Success`() = runTest {
    val results =
        flow<AsyncResult<Int>> {
              emit(Loading)
              delay(500.milliseconds)
              emit(Success(42)) // This won't be collected due to timeout
            }
            .timeoutToError(100.milliseconds) { Error(RuntimeException("Timed out")) }
            .toList()

    assertThat(results.size).isEqualTo(2)
    assertThat(results[0]).isEqualTo(Loading)
    assertThat(results[1]).isInstanceOf<Error>()
  }

  @Test
  fun `timeoutToError with immediate Success`() = runTest {
    val results =
        flowOf<AsyncResult<Int>>(Success(42))
            .timeoutToError(100.milliseconds) { Error(RuntimeException("Timeout!")) }
            .toList()

    assertThat(results).containsExactly(Success(42))
  }

  // ==============================
  // retryOnError
  // ==============================

  @Test
  fun `retryOnError does not retry on Success`() = runTest {
    var attempts = 0
    val results =
        flow<AsyncResult<Int>> {
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
    val results =
        flow<AsyncResult<Int>> {
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
    val results =
        flow<AsyncResult<Int>> {
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
    val results =
        flow<AsyncResult<Int>> {
              attempts++
              emit(Error(Throwable("NonRetryable")))
            }
            .retryOnError(maxRetries = 3, predicate = { it.throwable?.message == "Retryable" })
            .toList()

    assertThat(attempts).isEqualTo(1)
    assertThat(results.size).isEqualTo(1)
    assertThat(results[0]).isInstanceOf<Error>()
  }

  @Test
  fun `retryOnError retries when predicate matches`() = runTest {
    var attempts = 0
    val results =
        flow<AsyncResult<Int>> {
              attempts++
              if (attempts < 3) {
                emit(Error(Throwable("Retryable")))
              } else {
                emit(Success(42))
              }
            }
            .retryOnError(maxRetries = 5, predicate = { it.throwable?.message == "Retryable" })
            .toList()

    assertThat(attempts).isEqualTo(3)
    assertThat(results.last()).isEqualTo(Success(42))
  }

  @Test
  fun `retryOnError applies delay between retries`() = runTest {
    var attempts = 0
    var lastAttemptTime = 0L
    val delays = mutableListOf<Long>()

    val results =
        flow<AsyncResult<Int>> {
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
    val results =
        flow<AsyncResult<Int>> {
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
    val results =
        flowOf<AsyncResult<Int>>(NotStarted, Success(42)).retryOnError(maxRetries = 3).toList()

    assertThat(results).containsExactly(NotStarted, Success(42))
  }

  @Test
  fun `retryOnError passes through Loading`() = runTest {
    val results =
        flowOf<AsyncResult<Int>>(Loading, Success(42)).retryOnError(maxRetries = 3).toList()

    assertThat(results).containsExactly(Loading, Success(42))
  }

  // ==============================
  // retryOnErrorWithMetadata
  // ==============================

  data class RetryableError(val shouldRetry: Boolean)

  @Test
  fun `retryOnErrorWithMetadata retries when metadata matches and predicate true`() = runTest {
    var attempts = 0
    val results =
        flow<AsyncResult<Int>> {
              attempts++
              if (attempts < 3) {
                emit(Error(Throwable("Error"), RetryableError(true)))
              } else {
                emit(Success(42))
              }
            }
            .retryOnErrorWithMetadata<Int, RetryableError>(maxRetries = 3) { it.shouldRetry }
            .toList()

    assertThat(attempts).isEqualTo(3)
    assertThat(results).containsExactly(Success(42))
  }

  @Test
  fun `retryOnErrorWithMetadata does not retry when metadata matches but predicate false`() =
      runTest {
        var attempts = 0
        val results =
            flow<AsyncResult<Int>> {
                  attempts++
                  emit(Error(Throwable("Error"), RetryableError(false)))
                }
                .retryOnErrorWithMetadata<Int, RetryableError>(maxRetries = 3) { it.shouldRetry }
                .toList()

        assertThat(attempts).isEqualTo(1)
        assertThat(results.size).isEqualTo(1)
        assertThat(results[0]).isInstanceOf<Error>()
      }

  @Test
  fun `retryOnErrorWithMetadata does not retry when metadata type does not match`() = runTest {
    data class OtherError(val code: Int)

    var attempts = 0
    val results =
        flow<AsyncResult<Int>> {
              attempts++
              emit(Error(Throwable("Error"), OtherError(500)))
            }
            .retryOnErrorWithMetadata<Int, RetryableError>(maxRetries = 3) { true }
            .toList()

    assertThat(attempts).isEqualTo(1)
    assertThat(results.size).isEqualTo(1)
    assertThat(results[0]).isInstanceOf<Error>()
  }

  @Test
  fun `retryOnErrorWithMetadata does not retry plain Error without metadata`() = runTest {
    var attempts = 0
    val results =
        flow<AsyncResult<Int>> {
              attempts++
              emit(Error(Throwable("Error")))
            }
            .retryOnErrorWithMetadata<Int, RetryableError>(maxRetries = 3) { true }
            .toList()

    assertThat(attempts).isEqualTo(1)
    assertThat(results.size).isEqualTo(1)
    assertThat(results[0]).isInstanceOf<Error>()
  }

  // ==============================
  // asAsyncResult
  // ==============================

  @Test
  fun `asAsyncResult wraps emissions in Success with Loading start`() = runTest {
    val results = flowOf(1, 2, 3).asAsyncResult().toList()

    assertThat(results).containsExactly(Loading, Success(1), Success(2), Success(3))
  }

  @Test
  fun `asAsyncResult wraps emissions in Success without Loading start`() = runTest {
    val results = flowOf(1, 2, 3).asAsyncResult(startWithLoading = false).toList()

    assertThat(results).containsExactly(Success(1), Success(2), Success(3))
  }

  @Test
  fun `asAsyncResult catches exceptions as Error`() = runTest {
    val exception = RuntimeException("Test error")
    val results =
        flow {
              emit(42)
              throw exception
            }
            .asAsyncResult()
            .toList()

    assertThat(results.size).isEqualTo(3)
    assertThat(results[0]).isEqualTo(Loading)
    assertThat(results[1]).isEqualTo(Success(42))
    val error = results[2] as Error
    assertThat(error.throwable).isEqualTo(exception)
  }

  @Test
  fun `asAsyncResult catches exceptions without Loading when disabled`() = runTest {
    val exception = RuntimeException("Test error")
    val results =
        flow {
              emit(42)
              throw exception
            }
            .asAsyncResult(startWithLoading = false)
            .toList()

    assertThat(results.size).isEqualTo(2)
    assertThat(results[0]).isEqualTo(Success(42))
    val error = results[1] as Error
    assertThat(error.throwable).isEqualTo(exception)
  }

  @Test
  fun `asAsyncResult rethrows CancellationException`() = runTest {
    var caught = false
    try {
      flow {
            emit(1)
            throw kotlinx.coroutines.CancellationException("Cancelled")
          }
          .asAsyncResult()
          .toList()
    } catch (e: kotlinx.coroutines.CancellationException) {
      caught = true
      assertThat(e.message).isEqualTo("Cancelled")
    }

    assertThat(caught).isEqualTo(true)
  }

  @Test
  fun `asAsyncResult handles empty flow with Loading`() = runTest {
    val results = flowOf<Int>().asAsyncResult().toList()

    assertThat(results).containsExactly(Loading)
  }

  @Test
  fun `asAsyncResult handles empty flow without Loading`() = runTest {
    val results = flowOf<Int>().asAsyncResult(startWithLoading = false).toList()

    assertThat(results).containsExactly()
  }

  @Test
  fun `asAsyncResult with single value`() = runTest {
    val results = flowOf(42).asAsyncResult().toList()

    assertThat(results).containsExactly(Loading, Success(42))
  }

  @Test
  fun `asAsyncResult with immediate exception`() = runTest {
    val exception = IllegalStateException("Immediate failure")
    val results = flow<Int> { throw exception }.asAsyncResult().toList()

    assertThat(results.size).isEqualTo(2)
    assertThat(results[0]).isEqualTo(Loading)
    val error = results[1] as Error
    assertThat(error.throwable).isEqualTo(exception)
  }

  @Test
  fun `asAsyncResult preserves flow context and works with delay`() = runTest {
    val results =
        flow {
              emit(1)
              delay(10.milliseconds)
              emit(2)
              delay(10.milliseconds)
              emit(3)
            }
            .asAsyncResult()
            .toList()

    assertThat(results).containsExactly(Loading, Success(1), Success(2), Success(3))
  }

  @Test
  fun `asAsyncResult works with different types`() = runTest {
    val stringResults = flowOf("hello", "world").asAsyncResult().toList()
    assertThat(stringResults).containsExactly(Loading, Success("hello"), Success("world"))

    data class User(val name: String)
    val userResults = flowOf(User("Alice"), User("Bob")).asAsyncResult().toList()
    assertThat(userResults).containsExactly(Loading, Success(User("Alice")), Success(User("Bob")))
  }

  @Test
  fun `asAsyncResult can be chained with other AsyncResult operators`() = runTest {
    val results = flowOf(1, 2, 3).asAsyncResult().skipWhileLoading().toList()

    assertThat(results).containsExactly(Success(1), Success(2), Success(3))
  }

  @Test
  fun `asAsyncResult with mapSuccess chaining`() = runTest {
    val results =
        flowOf(1, 2, 3).asAsyncResult().toList().map { result -> result.mapSuccess { it * 2 } }

    assertThat(results).containsExactly(Loading, Success(2), Success(4), Success(6))
  }
}
