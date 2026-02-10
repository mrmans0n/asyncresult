// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class AsyncResultContainsTest {
  @Test
  fun `contains returns true for Success with matching value`() {
    val result: AsyncResult<Int> = Success(42)
    assertThat(result.contains(42)).isTrue()
  }

  @Test
  fun `contains returns false for Success with different value`() {
    val result: AsyncResult<Int> = Success(42)
    assertThat(result.contains(99)).isFalse()
  }

  @Test
  fun `contains returns false for Error`() {
    val result: AsyncResult<Int> = Error()
    assertThat(result.contains(42)).isFalse()
  }

  @Test
  fun `contains returns false for Loading`() {
    val result: AsyncResult<Int> = Loading
    assertThat(result.contains(42)).isFalse()
  }

  @Test
  fun `contains returns false for NotStarted`() {
    val result: AsyncResult<Int> = NotStarted
    assertThat(result.contains(42)).isFalse()
  }

  @Test
  fun `contains works with nullable values`() {
    val result: AsyncResult<Int?> = Success(null)
    assertThat(result.contains(null)).isTrue()
    assertThat(result.contains(42)).isFalse()
  }

  @Test
  fun `in operator works for Success with matching value`() {
    val result: AsyncResult<Int> = Success(42)
    assertThat(42 in result).isTrue()
    assertThat(99 in result).isFalse()
  }

  @Test
  fun `in operator returns false for non-Success states`() {
    assertThat(42 in (Error() as AsyncResult<Int>)).isFalse()
    assertThat(42 in (Loading as AsyncResult<Int>)).isFalse()
    assertThat(42 in (NotStarted as AsyncResult<Int>)).isFalse()
  }
}
