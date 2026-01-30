package io.nlopez.asyncresult.either

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.nlopez.asyncresult.AsyncResult
import io.nlopez.asyncresult.Error
import io.nlopez.asyncresult.ErrorWithMetadata
import io.nlopez.asyncresult.Loading
import io.nlopez.asyncresult.NotStarted
import io.nlopez.asyncresult.Success
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
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
}
