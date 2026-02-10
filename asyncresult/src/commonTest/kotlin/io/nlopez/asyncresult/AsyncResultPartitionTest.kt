// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test

class AsyncResultPartitionTest {
  @Test
  fun `partition separates successes and errors`() {
    val error = Error()
    val items: List<AsyncResult<Int>> = listOf(Success(1), error, Success(3))
    val (successes, errors) = items.partition()

    assertThat(successes).containsExactly(1, 3)
    assertThat(errors).containsExactly(error)
  }

  @Test
  fun `partition ignores Loading and NotStarted`() {
    val items: List<AsyncResult<Int>> = listOf(Success(1), Loading, NotStarted, Success(2))
    val (successes, errors) = items.partition()

    assertThat(successes).containsExactly(1, 2)
    assertThat(errors).isEmpty()
  }

  @Test
  fun `partition on empty list returns empty pairs`() {
    val items: List<AsyncResult<Int>> = emptyList()
    val (successes, errors) = items.partition()

    assertThat(successes).isEmpty()
    assertThat(errors).isEmpty()
  }

  @Test
  fun `partition works on sequences`() {
    val error = Error()
    val items = sequenceOf<AsyncResult<Int>>(Success(1), error, Loading)
    val (successes, errors) = items.partition()

    assertThat(successes).containsExactly(1)
    assertThat(errors).containsExactly(error)
  }

  @Test
  fun `partition works on arrays`() {
    val error = Error()
    val items: Array<AsyncResult<Int>> = arrayOf(Success(1), error, Success(3))
    val (successes, errors) = items.partition()

    assertThat(successes).containsExactly(1, 3)
    assertThat(errors).containsExactly(error)
  }
}
