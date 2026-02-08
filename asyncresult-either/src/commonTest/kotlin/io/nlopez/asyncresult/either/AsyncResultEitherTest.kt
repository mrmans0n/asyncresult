// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult.either

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.nlopez.asyncresult.AsyncResult
import io.nlopez.asyncresult.Error
import io.nlopez.asyncresult.ErrorWithMetadata
import io.nlopez.asyncresult.Loading
import io.nlopez.asyncresult.NotStarted
import io.nlopez.asyncresult.Success
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class AsyncResultEitherTest {
  @Test
  fun `AsyncResult Either bind will populate Error with a throwable if Left is throwable`() {
    val throwable = Throwable()
    assertThat(Success<Either<Throwable, Int>>(Left(throwable)).bind()).isEqualTo(Error(throwable))
    assertThat(Success<Either<Throwable, Int>>(Right(1)).bind()).isEqualTo(Success(1))
    assertThat((NotStarted as AsyncResult<Either<Throwable, Int>>).bind()).isEqualTo(NotStarted)
    assertThat((Loading as AsyncResult<Either<Throwable, Int>>).bind()).isEqualTo(Loading)
  }

  @Test
  fun `AsyncResult Either bind will populate Error with metadata with the contents of Left`() {
    assertThat(Success<Either<String, Int>>(Left("boom")).bind())
        .isEqualTo(ErrorWithMetadata("boom"))
    assertThat(Success<Either<String, Int>>(Right(1)).bind()).isEqualTo(Success(1))
    assertThat((NotStarted as AsyncResult<Either<String, Int>>).bind()).isEqualTo(NotStarted)
    assertThat((Loading as AsyncResult<Either<String, Int>>).bind()).isEqualTo(Loading)
  }

  @Test
  fun `Either toAsyncResult maps Left to Error and Right to Success`() {
    assertThat(Either.Left("boom").toAsyncResult()).isEqualTo(ErrorWithMetadata("boom"))
    assertThat(Either.Right(1).toAsyncResult()).isEqualTo(Success(1))
  }

  @Test
  fun `Flow toEither transforms Success to Right and Error to Left without metadata`() = runTest {
    val flow = flowOf<AsyncResult<Int>>(Success(42))
    val either = flow.toEither<String, Int>()
    assertThat(either).isEqualTo(Right(42))

    val errorFlow = flowOf<AsyncResult<Int>>(ErrorWithMetadata("error message"))
    val errorEither = errorFlow.toEither()
    assertThat(errorEither).isEqualTo(Left(ErrorWithMetadata("error message")))
  }

  @Test
  fun `Flow toEither transforms Success to Right and Error to Left`() = runTest {
    val flow = flowOf<AsyncResult<Int>>(Success(42))
    val either = flow.toEither<String, Int>()
    assertThat(either).isEqualTo(Right(42))

    val errorFlow = flowOf<AsyncResult<Int>>(ErrorWithMetadata("error message"))
    val errorEither = errorFlow.toEither<String, Int>()
    assertThat(errorEither).isEqualTo(Left("error message"))
  }

  @Test
  fun `Flow toEither uses the errorTransform function to transform the Error`() = runTest {
    val error = RuntimeException("boom")
    val flow = flowOf<AsyncResult<Int>>(Error(throwable = error))
    val either = flow.toEither<String, Int> { "Custom: ${it.throwable?.message}" }
    assertThat(either).isEqualTo(Left("Custom: boom"))
  }

  // ==============================
  // Flow<Either>.asAsyncResult
  // ==============================

  @Test
  fun `Flow Either asAsyncResult converts Right to Success with Loading start`() = runTest {
    val results = flowOf(Right(1), Right(2), Right(3)).asAsyncResult().toList()

    assertThat(results).containsExactly(Loading, Success(1), Success(2), Success(3))
  }

  @Test
  fun `Flow Either asAsyncResult converts Left to ErrorWithMetadata`() = runTest {
    val results = flowOf<Either<String, Int>>(Left("error")).asAsyncResult().toList()

    assertThat(results.size).isEqualTo(2)
    assertThat(results[0]).isEqualTo(Loading)
    assertThat(results[1]).isInstanceOf<Error>()
    assertThat((results[1] as Error).metadataOrNull<String>()).isEqualTo("error")
  }

  @Test
  fun `Flow Either asAsyncResult handles mixed Left and Right`() = runTest {
    val results =
        flowOf<Either<String, Int>>(Right(1), Left("error"), Right(2)).asAsyncResult().toList()

    assertThat(results.size).isEqualTo(4)
    assertThat(results[0]).isEqualTo(Loading)
    assertThat(results[1]).isEqualTo(Success(1))
    assertThat(results[2]).isInstanceOf<Error>()
    assertThat((results[2] as Error).metadataOrNull<String>()).isEqualTo("error")
    assertThat(results[3]).isEqualTo(Success(2))
  }

  @Test
  fun `Flow Either asAsyncResult without Loading start`() = runTest {
    val results =
        flowOf<Either<String, Int>>(Right(42)).asAsyncResult(startWithLoading = false).toList()

    assertThat(results).containsExactly(Success(42))
  }

  @Test
  fun `Flow Either asAsyncResult handles empty flow with Loading`() = runTest {
    val results = flowOf<Either<String, Int>>().asAsyncResult().toList()

    assertThat(results).containsExactly(Loading)
  }

  @Test
  fun `Flow Either asAsyncResult handles empty flow without Loading`() = runTest {
    val results = flowOf<Either<String, Int>>().asAsyncResult(startWithLoading = false).toList()

    assertThat(results).containsExactly()
  }

  // ==============================
  // Flow<Either<Throwable, R>>.asAsyncResult
  // ==============================

  @Test
  fun `Flow Either Throwable asAsyncResult converts Left throwable to Error throwable`() = runTest {
    val exception = RuntimeException("boom")
    val results = flowOf<Either<Throwable, Int>>(Left(exception)).asAsyncResult().toList()

    assertThat(results.size).isEqualTo(2)
    assertThat(results[0]).isEqualTo(Loading)
    assertThat(results[1]).isInstanceOf<Error>()
    assertThat((results[1] as Error).throwable).isEqualTo(exception)
  }

  @Test
  fun `Flow Either Throwable asAsyncResult converts Right to Success`() = runTest {
    val results = flowOf<Either<Throwable, Int>>(Right(42)).asAsyncResult().toList()

    assertThat(results).containsExactly(Loading, Success(42))
  }

  @Test
  fun `Flow Either Throwable asAsyncResult without Loading start`() = runTest {
    val exception = IllegalStateException("error")
    val results =
        flowOf<Either<Throwable, Int>>(Left(exception))
            .asAsyncResult(startWithLoading = false)
            .toList()

    assertThat(results.size).isEqualTo(1)
    assertThat(results[0]).isInstanceOf<Error>()
    assertThat((results[0] as Error).throwable).isEqualTo(exception)
  }

  @Test
  fun `Raise bind with Success returns Right value`() {
    val either = either<Error, Int> { bind(Success(42)) }
    assertThat(either).isEqualTo(Right(42))
  }

  @Test
  fun `Raise bind with Error raises Left error`() {
    val throwable = IllegalStateException("boom")
    val either = either<Error, Int> { bind(Error(throwable)) }
    assertThat(either).isEqualTo(Left(Error(throwable)))
  }
}
