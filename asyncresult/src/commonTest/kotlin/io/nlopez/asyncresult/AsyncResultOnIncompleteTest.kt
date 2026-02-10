// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class AsyncResultOnIncompleteTest {
  @Test
  fun `onIncomplete executes block when result is Loading`() {
    var executed = false
    val result: AsyncResult<Int> = Loading

    result.onIncomplete { executed = true }

    assertThat(executed).isEqualTo(true)
  }

  @Test
  fun `onIncomplete executes block when result is NotStarted`() {
    var executed = false
    val result: AsyncResult<Int> = NotStarted

    result.onIncomplete { executed = true }

    assertThat(executed).isEqualTo(true)
  }

  @Test
  fun `onIncomplete does not execute block when result is Success`() {
    var executed = false
    val result: AsyncResult<Int> = Success(42)

    result.onIncomplete { executed = true }

    assertThat(executed).isEqualTo(false)
  }

  @Test
  fun `onIncomplete does not execute block when result is Error`() {
    var executed = false
    val result: AsyncResult<Int> = Error(Exception("Failed"))

    result.onIncomplete { executed = true }

    assertThat(executed).isEqualTo(false)
  }

  @Test
  fun `onIncomplete returns the same AsyncResult for chaining`() {
    val loading: AsyncResult<Int> = Loading
    val notStarted: AsyncResult<Int> = NotStarted
    val success: AsyncResult<Int> = Success(42)
    val error: AsyncResult<Int> = Error(Exception("Failed"))

    val loadingResult = loading.onIncomplete {}
    val notStartedResult = notStarted.onIncomplete {}
    val successResult = success.onIncomplete {}
    val errorResult = error.onIncomplete {}

    assertThat(loadingResult).isInstanceOf<Loading>()
    assertThat(notStartedResult).isInstanceOf<NotStarted>()
    assertThat(successResult).isInstanceOf<Success<Int>>()
    assertThat(errorResult).isInstanceOf<Error>()
  }

  @Test
  fun `onIncomplete can be chained with other callbacks`() {
    var incompleteExecuted = false
    var successExecuted = false
    val result: AsyncResult<Int> = Loading

    result.onIncomplete { incompleteExecuted = true }.onSuccess { successExecuted = true }

    assertThat(incompleteExecuted).isEqualTo(true)
    assertThat(successExecuted).isEqualTo(false)
  }

  @Test
  fun `onIncomplete chain for Success state`() {
    var incompleteExecuted = false
    var successExecuted = false
    val result: AsyncResult<Int> = Success(42)

    result.onIncomplete { incompleteExecuted = true }.onSuccess { successExecuted = true }

    assertThat(incompleteExecuted).isEqualTo(false)
    assertThat(successExecuted).isEqualTo(true)
  }

  // Flow variant tests

  @Test
  fun `Flow onIncomplete executes action when Loading is emitted`() = runTest {
    var executionCount = 0
    val flow = flowOf<AsyncResult<Int>>(Loading, Success(42))

    flow.onIncomplete { executionCount++ }.toList()

    assertThat(executionCount).isEqualTo(1)
  }

  @Test
  fun `Flow onIncomplete executes action when NotStarted is emitted`() = runTest {
    var executionCount = 0
    val flow = flowOf<AsyncResult<Int>>(NotStarted, Success(42))

    flow.onIncomplete { executionCount++ }.toList()

    assertThat(executionCount).isEqualTo(1)
  }

  @Test
  fun `Flow onIncomplete does not execute action when Success is emitted`() = runTest {
    var executionCount = 0
    val flow = flowOf<AsyncResult<Int>>(Success(42))

    flow.onIncomplete { executionCount++ }.toList()

    assertThat(executionCount).isEqualTo(0)
  }

  @Test
  fun `Flow onIncomplete does not execute action when Error is emitted`() = runTest {
    var executionCount = 0
    val flow = flowOf<AsyncResult<Int>>(Error(Exception("Failed")))

    flow.onIncomplete { executionCount++ }.toList()

    assertThat(executionCount).isEqualTo(0)
  }

  @Test
  fun `Flow onIncomplete executes multiple times for multiple incomplete states`() = runTest {
    var executionCount = 0
    val flow = flowOf<AsyncResult<Int>>(Loading, NotStarted, Loading, Success(42))

    flow.onIncomplete { executionCount++ }.toList()

    assertThat(executionCount).isEqualTo(3)
  }

  @Test
  fun `Flow onIncomplete can be chained with other flow operators`() = runTest {
    var incompleteCount = 0
    var successCount = 0
    val flow = flowOf<AsyncResult<Int>>(Loading, Success(42), NotStarted)

    flow.onIncomplete { incompleteCount++ }.onSuccess { successCount++ }.toList()

    assertThat(incompleteCount).isEqualTo(2)
    assertThat(successCount).isEqualTo(1)
  }

  @Test
  fun `Flow onIncomplete emits all values downstream`() = runTest {
    val flow = flowOf<AsyncResult<Int>>(Loading, Success(42), NotStarted, Error(Exception("Test")))

    val results = flow.onIncomplete {}.toList()

    assertThat(results.size).isEqualTo(4)
    assertThat(results[0]).isInstanceOf<Loading>()
    assertThat(results[1]).isInstanceOf<Success<Int>>()
    assertThat(results[2]).isInstanceOf<NotStarted>()
    assertThat(results[3]).isInstanceOf<Error>()
  }
}
