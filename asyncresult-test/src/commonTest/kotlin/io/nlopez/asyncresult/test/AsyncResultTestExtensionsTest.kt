// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult.test

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.messageContains
import io.nlopez.asyncresult.AsyncResult
import io.nlopez.asyncresult.Error
import io.nlopez.asyncresult.ErrorId
import io.nlopez.asyncresult.Incomplete
import io.nlopez.asyncresult.Loading
import io.nlopez.asyncresult.NotStarted
import io.nlopez.asyncresult.Success
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class AsyncResultTestExtensionsTest {

  // ==========================================================================
  // Flow: Non-terminal emission asserts
  // ==========================================================================

  @Test
  fun `assertFirstIsLoading succeeds when first emission is Loading`() = runTest {
    val flow = flowOf(Loading, Success(42))
    flow.assertFirstIsLoading()
  }

  @Test
  fun `assertFirstIsLoading fails when first emission is not Loading`() = runTest {
    val flow = flowOf(Success(42))
    assertFailure { flow.assertFirstIsLoading() }
        .messageContains("Expected first emission to be Loading")
  }

  @Test
  fun `assertFirstIsNotStarted succeeds when first emission is NotStarted`() = runTest {
    val flow = flowOf(NotStarted, Loading, Success(42))
    flow.assertFirstIsNotStarted()
  }

  @Test
  fun `assertFirstIsNotStarted fails when first emission is not NotStarted`() = runTest {
    val flow = flowOf(Loading, Success(42))
    assertFailure { flow.assertFirstIsNotStarted() }
        .messageContains("Expected first emission to be NotStarted")
  }

  @Test
  fun `assertFirstIsIncomplete succeeds when first emission is Incomplete`() = runTest {
    val flow = flowOf(Incomplete, Success(42))
    flow.assertFirstIsIncomplete()
  }

  @Test
  fun `assertFirstIsIncomplete fails when first emission is not Incomplete`() = runTest {
    val flow = flowOf(Success(42))
    assertFailure { flow.assertFirstIsIncomplete() }
        .messageContains("Expected first emission to be Incomplete")
  }

  // ==========================================================================
  // Collection helpers
  // ==========================================================================

  @Test
  fun `hasAnyLoading succeeds when collection contains Loading`() {
    val results = listOf(NotStarted, Loading, Success(42))
    assertThat(results).hasAnyLoading()
  }

  @Test
  fun `hasAnyLoading fails when collection has no Loading`() {
    val results = listOf(NotStarted, Success(42))
    assertFailure { assertThat(results).hasAnyLoading() }
        .messageContains("collection to have at least one Loading")
  }

  @Test
  fun `hasAnyIncomplete succeeds when collection contains Incomplete`() {
    val results = listOf(NotStarted, Incomplete, Success(42))
    assertThat(results).hasAnyIncomplete()
  }

  @Test
  fun `hasAnyIncomplete fails when collection has no Incomplete`() {
    val results = listOf(NotStarted, Loading, Success(42))
    assertFailure { assertThat(results).hasAnyIncomplete() }
        .messageContains("collection to have at least one Incomplete")
  }

  @Test
  fun `allErrors returns all Error instances`() {
    val error1 = Error(Throwable("error1"))
    val error2 = Error(Throwable("error2"))
    val results = listOf(NotStarted, error1, Success(42), error2, Loading)
    
    assertThat(results).allErrors().hasSize(2)
    assertThat(results).allErrors().containsExactly(error1, error2)
  }

  @Test
  fun `allErrors returns empty list when no errors`() {
    val results = listOf(NotStarted, Loading, Success(42))
    assertThat(results).allErrors().hasSize(0)
  }

  data class TestMetadata(val code: Int)

  @Test
  fun `allErrorMetadata returns all metadata instances of given type`() {
    val meta1 = TestMetadata(100)
    val meta2 = TestMetadata(200)
    val error1 = AsyncResult.error<Int>(Throwable("error1"), metadata = meta1)
    val error2 = AsyncResult.error<Int>(Throwable("error2"), metadata = meta2)
    val error3 = AsyncResult.error<Int>(Throwable("error3")) // No metadata
    
    val results = listOf(NotStarted, error1, Success(42), error2, error3)
    
    assertThat(results).allErrorMetadata<TestMetadata>().hasSize(2)
    assertThat(results).allErrorMetadata<TestMetadata>().containsExactly(meta1, meta2)
  }

  @Test
  fun `allErrorMetadata returns empty list when no matching metadata`() {
    val results = listOf(NotStarted, Loading, Success(42), Error(Throwable("error")))
    assertThat(results).allErrorMetadata<TestMetadata>().hasSize(0)
  }

  // ==========================================================================
  // Spread/zip helpers
  // ==========================================================================

  @Test
  fun `spreadsTo for Pair succeeds when both are Success`() {
    val result = Success(Pair(42, "hello"))
    assertThat(result).spreadsTo(
        first = { isSuccess().isEqualTo(42) },
        second = { isSuccess().isEqualTo("hello") }
    )
  }

  @Test
  fun `spreadsTo for Pair propagates Error to both`() {
    val error = AsyncResult.error<Pair<Int, String>>(
        Throwable("test error"),
        errorId = ErrorId("TEST")
    )
    assertThat(error).spreadsTo(
        first = { isErrorWithIdEqualTo(ErrorId("TEST")) },
        second = { isErrorWithIdEqualTo(ErrorId("TEST")) }
    )
  }

  @Test
  fun `spreadsTo for Pair fails on non-terminal state`() {
    val result: AsyncResult<Pair<Int, String>> = Loading
    assertFailure {
      assertThat(result).spreadsTo(
          first = { isSuccess() },
          second = { isSuccess() }
      )
    }.messageContains("to be terminal")
  }

  @Test
  fun `spreadsTo for Triple succeeds when all are Success`() {
    val result = Success(Triple(42, "hello", true))
    assertThat(result).spreadsTo(
        first = { isSuccess().isEqualTo(42) },
        second = { isSuccess().isEqualTo("hello") },
        third = { isSuccess().isEqualTo(true) }
    )
  }

  @Test
  fun `spreadsTo for Triple propagates Error to all`() {
    val error = AsyncResult.error<Triple<Int, String, Boolean>>(
        Throwable("test error"),
        errorId = ErrorId("TEST")
    )
    assertThat(error).spreadsTo(
        first = { isErrorWithIdEqualTo(ErrorId("TEST")) },
        second = { isErrorWithIdEqualTo(ErrorId("TEST")) },
        third = { isErrorWithIdEqualTo(ErrorId("TEST")) }
    )
  }

  // ==========================================================================
  // Unwrap/expect helpers
  // ==========================================================================

  @Test
  fun `unwrapSucceeds returns value when Success`() {
    val result = Success(42)
    assertThat(result).unwrapSucceeds().isEqualTo(42)
  }

  @Test
  fun `unwrapSucceeds fails when not Success`() {
    val result: AsyncResult<Int> = Loading
    assertFailure { assertThat(result).unwrapSucceeds() }
        .messageContains("AsyncResult to be Success for unwrap")
  }

  @Test
  fun `unwrapFailsWithMessage succeeds when error message matches`() {
    val result = Error(Throwable("expected error message"))
    assertThat(result).unwrapFailsWithMessage("expected error message")
  }

  @Test
  fun `unwrapFailsWithMessage fails when error message does not match`() {
    val result = Error(Throwable("actual message"))
    assertFailure { assertThat(result).unwrapFailsWithMessage("expected message") }
        .messageContains("Expected error message")
  }

  @Test
  fun `unwrapFailsWithMessage fails when not Error`() {
    val result = Success(42)
    assertFailure { assertThat(result).unwrapFailsWithMessage("any message") }
        .messageContains("Expected AsyncResult to be Error")
  }
}
