// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult.test

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.nlopez.asyncresult.Error
import io.nlopez.asyncresult.ErrorId
import io.nlopez.asyncresult.Loading
import io.nlopez.asyncresult.NotStarted
import io.nlopez.asyncresult.Success
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class AsyncResultTestExtensionsTest {

  @Test
  fun `isErrorWithThrowable returns throwable when present`() {
    val throwable = IllegalStateException("test error")
    val result = Error(throwable)
    assertThat(result).isErrorWithThrowable().isEqualTo(throwable)
  }

  @Test
  fun `isErrorWithThrowable fails when throwable is null`() {
    val result = Error()
    val ex = assertFailsWith<AssertionError> { assertThat(result).isErrorWithThrowable() }
    assertTrue(ex.message?.contains("throwable") == true)
  }

  @Test
  fun `isErrorWithThrowable fails when not Error`() {
    val result = Success(42)
    val ex = assertFailsWith<AssertionError> { assertThat(result).isErrorWithThrowable() }
    assertTrue(ex.message?.contains("Error") == true)
  }

  @Test
  fun `isErrorWithThrowableOfType returns typed throwable when type matches`() {
    val throwable = IllegalArgumentException("test error")
    val result = Error(throwable)
    assertThat(result).isErrorWithThrowableOfType<IllegalArgumentException>().isEqualTo(throwable)
  }

  @Test
  fun `isErrorWithThrowableOfType fails when type does not match`() {
    val throwable = IllegalStateException("test error")
    val result = Error(throwable)
    val ex =
        assertFailsWith<AssertionError> {
          assertThat(result).isErrorWithThrowableOfType<IllegalArgumentException>()
        }
    assertTrue(ex.message?.contains("IllegalArgumentException") == true)
  }

  @Test
  fun `isErrorWithThrowableOfType fails when throwable is null`() {
    val result = Error()
    val ex =
        assertFailsWith<AssertionError> {
          assertThat(result).isErrorWithThrowableOfType<IllegalArgumentException>()
        }
    assertTrue(ex.message?.contains("throwable") == true)
  }

  @Test
  fun `isErrorWithThrowableOfType fails when not Error`() {
    val result = Success(42)
    val ex =
        assertFailsWith<AssertionError> {
          assertThat(result).isErrorWithThrowableOfType<IllegalArgumentException>()
        }
    assertTrue(ex.message?.contains("Error") == true)
  }

  @Test
  fun `isErrorWithThrowableMessage succeeds when message matches`() {
    val throwable = RuntimeException("expected message")
    val result = Error(throwable)
    assertThat(result).isErrorWithThrowableMessage("expected message")
  }

  @Test
  fun `isErrorWithThrowableMessage fails when message does not match`() {
    val throwable = RuntimeException("actual message")
    val result = Error(throwable)
    val ex =
        assertFailsWith<AssertionError> {
          assertThat(result).isErrorWithThrowableMessage("expected message")
        }
    assertTrue(ex.message?.contains("expected message") == true)
  }

  @Test
  fun `isErrorWithThrowableMessage fails when throwable is null`() {
    val result = Error()
    val ex =
        assertFailsWith<AssertionError> {
          assertThat(result).isErrorWithThrowableMessage("any message")
        }
    assertTrue(ex.message?.contains("throwable") == true)
  }

  @Test
  fun `isErrorWithThrowableMessage fails when not Error`() {
    val result = Success(42)
    val ex =
        assertFailsWith<AssertionError> {
          assertThat(result).isErrorWithThrowableMessage("any message")
        }
    assertTrue(ex.message?.contains("Error") == true)
  }

  @Test
  fun `assertFirstIsLoading succeeds when first emission is Loading`() = runTest {
    val flow = flowOf(Loading, Success(42))
    flow.assertFirstIsLoading()
  }

  @Test
  fun `assertFirstIsLoading fails when first emission is not Loading`() = runTest {
    val flow = flowOf(Success(42))
    val ex = assertFailsWith<AssertionError> { flow.assertFirstIsLoading() }
    assertTrue(ex.message?.contains("Loading") == true)
  }

  @Test
  fun `assertFirstIsNotStarted succeeds when first emission is NotStarted`() = runTest {
    val flow = flowOf(NotStarted, Loading, Success(42))
    flow.assertFirstIsNotStarted()
  }

  @Test
  fun `assertFirstIsNotStarted fails when first emission is not NotStarted`() = runTest {
    val flow = flowOf(Loading, Success(42))
    val ex = assertFailsWith<AssertionError> { flow.assertFirstIsNotStarted() }
    assertTrue(ex.message?.contains("NotStarted") == true)
  }

  @Test
  fun `assertFirstIsIncomplete succeeds when first emission is Incomplete`() = runTest {
    val flow = flowOf(Loading, Success(42))
    flow.assertFirstIsIncomplete()
  }

  @Test
  fun `assertFirstIsIncomplete fails when first emission is not Incomplete`() = runTest {
    val flow = flowOf(Success(42))
    val ex = assertFailsWith<AssertionError> { flow.assertFirstIsIncomplete() }
    assertTrue(ex.message?.contains("Incomplete") == true)
  }

  data class TestMetadata(val code: Int)

  @Test
  fun `assertErrorWithMetadata returns metadata when present`() = runTest {
    val metadata = TestMetadata(100)
    val flow = flowOf(Loading, Error(Throwable("error"), metadata))
    val result = flow.assertErrorWithMetadata<TestMetadata>()
    assertThat(result).isEqualTo(metadata)
  }

  @Test
  fun `assertErrorWithMetadata fails when metadata is null`() = runTest {
    val flow = flowOf(Loading, Error(Throwable("error")))
    val ex = assertFailsWith<AssertionError> { flow.assertErrorWithMetadata<TestMetadata>() }
    assertTrue(ex.message?.contains("metadata") == true)
  }

  @Test
  fun `assertErrorWithMetadata fails when terminal is Success`() = runTest {
    val flow = flowOf(Loading, Success(42))
    val ex = assertFailsWith<AssertionError> { flow.assertErrorWithMetadata<TestMetadata>() }
    assertTrue(ex.message?.contains("Error") == true)
  }

  @Test
  fun `assertErrorWithThrowableOfType returns typed throwable when type matches`() = runTest {
    val throwable = IllegalArgumentException("test error")
    val flow = flowOf(Loading, Error(throwable))
    val result = flow.assertErrorWithThrowableOfType<IllegalArgumentException>()
    assertThat(result).isEqualTo(throwable)
  }

  @Test
  fun `assertErrorWithThrowableOfType fails when type does not match`() = runTest {
    val throwable = IllegalStateException("test error")
    val flow = flowOf(Loading, Error(throwable))
    val ex =
        assertFailsWith<AssertionError> {
          flow.assertErrorWithThrowableOfType<IllegalArgumentException>()
        }
    assertTrue(ex.message?.contains("IllegalArgumentException") == true)
  }

  @Test
  fun `assertErrorWithThrowableOfType fails when throwable is null`() = runTest {
    val flow = flowOf(Loading, Error())
    val ex =
        assertFailsWith<AssertionError> {
          flow.assertErrorWithThrowableOfType<IllegalArgumentException>()
        }
    assertTrue(ex.message?.contains("throwable") == true)
  }

  @Test
  fun `assertErrorWithThrowableOfType fails when terminal is Success`() = runTest {
    val flow = flowOf(Loading, Success(42))
    val ex =
        assertFailsWith<AssertionError> {
          flow.assertErrorWithThrowableOfType<IllegalArgumentException>()
        }
    assertTrue(ex.message?.contains("Error") == true)
  }

  @Test
  fun `assertErrorWithId succeeds when errorId matches`() = runTest {
    val errorId = ErrorId("TEST-123")
    val flow = flowOf(Loading, Error(Throwable("error"), errorId = errorId))
    flow.assertErrorWithId(errorId)
  }

  @Test
  fun `assertErrorWithId fails when errorId does not match`() = runTest {
    val flow = flowOf(Loading, Error(Throwable("error"), errorId = ErrorId("ACTUAL")))
    val ex = assertFailsWith<AssertionError> { flow.assertErrorWithId(ErrorId("EXPECTED")) }
    assertTrue(ex.message?.contains("EXPECTED") == true)
  }

  @Test
  fun `assertErrorWithId fails when errorId is null`() = runTest {
    val flow = flowOf(Loading, Error(Throwable("error")))
    val ex = assertFailsWith<AssertionError> { flow.assertErrorWithId(ErrorId("TEST")) }
    assertTrue(ex.message?.contains("TEST") == true)
  }

  @Test
  fun `assertErrorWithId fails when terminal is Success`() = runTest {
    val flow = flowOf(Loading, Success(42))
    val ex = assertFailsWith<AssertionError> { flow.assertErrorWithId(ErrorId("TEST")) }
    assertTrue(ex.message?.contains("Error") == true)
  }

  @Test
  fun `hasAnyLoading succeeds when collection contains Loading`() {
    val results = listOf(NotStarted, Loading, Success(42))
    assertThat(results).hasAnyLoading()
  }

  @Test
  fun `hasAnyLoading fails when collection has no Loading`() {
    val results = listOf(NotStarted, Success(42))
    val ex = assertFailsWith<AssertionError> { assertThat(results).hasAnyLoading() }
    assertTrue(ex.message?.contains("Loading") == true)
  }

  @Test
  fun `hasAnyIncomplete succeeds when collection contains Incomplete`() {
    val results = listOf(Success(1), NotStarted, Success(42))
    assertThat(results).hasAnyIncomplete()
  }

  @Test
  fun `hasAnyIncomplete fails when collection has no Incomplete`() {
    val results = listOf(Success(1), Success(42), Error(Throwable("err")))
    val ex = assertFailsWith<AssertionError> { assertThat(results).hasAnyIncomplete() }
    assertTrue(ex.message?.contains("Incomplete") == true)
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

  @Test
  fun `allErrorMetadata returns all metadata instances of given type`() {
    val meta1 = TestMetadata(100)
    val meta2 = TestMetadata(200)
    val error1 = Error(throwable = Throwable("error1"), metadata = meta1)
    val error2 = Error(throwable = Throwable("error2"), metadata = meta2)
    val error3 = Error(throwable = Throwable("error3"))

    val results = listOf(NotStarted, error1, Success(42), error2, error3)

    assertThat(results).allErrorMetadata<TestMetadata>().hasSize(2)
    assertThat(results).allErrorMetadata<TestMetadata>().containsExactly(meta1, meta2)
  }

  @Test
  fun `allErrorMetadata returns empty list when no matching metadata`() {
    val results = listOf(NotStarted, Loading, Success(42), Error(Throwable("error")))
    assertThat(results).allErrorMetadata<TestMetadata>().hasSize(0)
  }
}
