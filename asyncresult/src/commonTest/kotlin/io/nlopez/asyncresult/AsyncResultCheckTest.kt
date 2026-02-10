// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class AsyncResultCheckTest {
  @Test
  fun `isSuccessAnd returns true when Success and predicate matches`() {
    assertThat((Success(10) as AsyncResult<Int>).isSuccessAnd { it > 5 }).isTrue()
  }

  @Test
  fun `isSuccessAnd returns false when Success and predicate does not match`() {
    assertThat((Success(2) as AsyncResult<Int>).isSuccessAnd { it > 5 }).isFalse()
  }

  @Test
  fun `isSuccessAnd returns false for non-Success states`() {
    assertThat((Error(Exception("boom")) as AsyncResult<Int>).isSuccessAnd { true }).isFalse()
    assertThat((Loading as AsyncResult<Int>).isSuccessAnd { true }).isFalse()
    assertThat((NotStarted as AsyncResult<Int>).isSuccessAnd { true }).isFalse()
  }

  @Test
  fun `isErrorAnd returns true when Error and predicate matches`() {
    val result: AsyncResult<Int> = Error(IllegalArgumentException("boom"))
    assertThat(result.isErrorAnd { it is IllegalArgumentException }).isTrue()
  }

  @Test
  fun `isErrorAnd returns false when Error and predicate does not match`() {
    val result: AsyncResult<Int> = Error(IllegalArgumentException("boom"))
    assertThat(result.isErrorAnd { it is IllegalStateException }).isFalse()
  }

  @Test
  fun `isErrorAnd returns false for non-Error states`() {
    assertThat((Success(1) as AsyncResult<Int>).isErrorAnd { true }).isFalse()
    assertThat((Loading as AsyncResult<Int>).isErrorAnd { true }).isFalse()
    assertThat((NotStarted as AsyncResult<Int>).isErrorAnd { true }).isFalse()
  }

  @Test
  fun `isErrorWithMetadataAnd returns true when Error and predicate matches`() {
    val result: AsyncResult<Int> = ErrorWithMetadata("not found")
    assertThat(
            result.isErrorWithMetadataAnd<Int, String> { _, metadata -> metadata == "not found" })
        .isTrue()
  }

  @Test
  fun `isErrorWithMetadataAnd returns false when Error and predicate does not match`() {
    val result: AsyncResult<Int> = ErrorWithMetadata("not found")
    assertThat(
            result.isErrorWithMetadataAnd<Int, String> { _, metadata -> metadata == "forbidden" })
        .isFalse()
  }

  @Test
  fun `isErrorWithMetadataAnd returns false for non-Error states`() {
    assertThat(
            (Success(1) as AsyncResult<Int>).isErrorWithMetadataAnd<Int, String> { _, _ -> true })
        .isFalse()
    assertThat((Loading as AsyncResult<Int>).isErrorWithMetadataAnd<Int, String> { _, _ -> true })
        .isFalse()
    assertThat(
            (NotStarted as AsyncResult<Int>).isErrorWithMetadataAnd<Int, String> { _, _ -> true })
        .isFalse()
  }
}
