// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

@Suppress("LargeClass")
class AsyncResultTest {
  @Test
  fun `getOrNull returns value when Success else returns null`() {
    assertThat(Success(10).getOrNull()).isEqualTo(10)
    assertThat((NotStarted as AsyncResult<Int>).getOrNull()).isNull()
    assertThat((Loading as AsyncResult<Int>).getOrNull()).isNull()
    assertThat(Error(Throwable()).getOrNull()).isNull()
  }

  @Test
  fun `getOrElse returns value when success else runs the block`() {
    assertThat(Success(10).getOrElse { error("boo") }).isEqualTo(10)
    assertFailsWith<IllegalStateException> { NotStarted.getOrElse { error("boo") } }
    assertFailsWith<IllegalStateException> { Loading.getOrElse { error("boo") } }
    assertFailsWith<IllegalStateException> { Error(Throwable()).getOrElse { error("boo") } }
  }

  @Test
  fun `getOrEmpty returns values when success else returns an empty list`() {
    assertThat(Success(listOf(1, 2, 3)).getOrEmpty()).containsExactly(1, 2, 3)
    assertThat((NotStarted as AsyncResult<List<Int>>).getOrEmpty()).isEmpty()
    assertThat((Loading as AsyncResult<List<Int>>).getOrEmpty()).isEmpty()
    assertThat((Error(Throwable()) as AsyncResult<List<Int>>).getOrEmpty()).isEmpty()
  }

  @Test
  fun `getOrEmpty returns values when success else returns an empty sequence`() {
    assertThat(Success(sequenceOf(1, 2, 3)).getOrEmpty()).containsExactly(1, 2, 3)
    assertThat((NotStarted as AsyncResult<Sequence<Int>>).getOrEmpty()).isEmpty()
    assertThat((Loading as AsyncResult<Sequence<Int>>).getOrEmpty()).isEmpty()
    assertThat((Error(Throwable()) as AsyncResult<Sequence<Int>>).getOrEmpty()).isEmpty()
  }

  @Test
  fun `getOrEmpty returns values when success else returns an empty map`() {
    val testMap = mapOf("a" to 1, "b" to 2, "c" to 3)
    assertThat(Success(testMap).getOrEmpty()).isEqualTo(testMap)
    assertThat((NotStarted as AsyncResult<Map<String, Int>>).getOrEmpty()).isEmpty()
    assertThat((Loading as AsyncResult<Map<String, Int>>).getOrEmpty()).isEmpty()
    assertThat((Error(Throwable()) as AsyncResult<Map<String, Int>>).getOrEmpty()).isEmpty()
  }

  @Test
  fun `getOrDefault returns value when success else returns the default`() {
    assertThat(Success(10).getOrDefault(0)).isEqualTo(10)
    assertThat(NotStarted.getOrDefault(0)).isEqualTo(0)
    assertThat(Loading.getOrDefault(0)).isEqualTo(0)
    assertThat(Error(Throwable()).getOrDefault(0)).isEqualTo(0)
  }

  @Test
  fun `throwableOrNull returns the throwable else returns null`() {
    assertThat(Success(10).throwableOrNull()).isNull()
    assertThat(NotStarted.throwableOrNull()).isNull()
    assertThat(Loading.throwableOrNull()).isNull()
    val throwable = Throwable()
    assertThat(Error(throwable).throwableOrNull()).isEqualTo(throwable)
  }

  @Test
  fun `errorOrNull returns the exception when error else returns null`() {
    assertThat(Success(10).errorOrNull()).isNull()
    assertThat(NotStarted.errorOrNull()).isNull()
    assertThat(Loading.errorOrNull()).isNull()
    val throwable = Throwable()
    assertThat(Error(throwable).errorOrNull()).isEqualTo(Error(throwable))
    assertThat(Error(throwable, "metadata").errorOrNull()).isEqualTo(Error(throwable, "metadata"))
  }

  @Test
  fun `errorWithMetadataOrNull returns metadata when error has metadata of matching type else returns null`() {
    assertThat(Success(10).errorWithMetadataOrNull<String>()).isNull()
    assertThat((NotStarted as AsyncResult<Int>).errorWithMetadataOrNull<String>()).isNull()
    assertThat((Loading as AsyncResult<Int>).errorWithMetadataOrNull<String>()).isNull()
    val throwable = Throwable()
    assertThat(Error(throwable).errorWithMetadataOrNull<String>()).isNull()
    assertThat(Error(throwable, "metadata").errorWithMetadataOrNull<String>()).isEqualTo("metadata")
    assertThat(Error(throwable, 1234).errorWithMetadataOrNull<String>()).isNull()
  }

  @Test
  fun `errorIdOrNull returns the errorId when error has errorId else returns null`() {
    assertThat(Success(10).errorIdOrNull()).isNull()
    assertThat((NotStarted as AsyncResult<Int>).errorIdOrNull()).isNull()
    assertThat((Loading as AsyncResult<Int>).errorIdOrNull()).isNull()
    val throwable = Throwable()
    val errorId = ErrorId("test-error-123")
    assertThat(Error(throwable).errorIdOrNull()).isNull()
    assertThat(Error(throwable, errorId = errorId).errorIdOrNull()).isEqualTo(errorId)
    assertThat(Error(throwable, "metadata", errorId).errorIdOrNull()).isEqualTo(errorId)
  }

  @Test
  fun `Error withErrorId creates a copy with the given errorId preserving metadata`() {
    val throwable = Throwable()
    val errorId = ErrorId("test-error-123")
    val error = Error(throwable, "metadata")
    val errorWithId = error.withErrorId(errorId)
    assertThat(errorWithId.errorId).isEqualTo(errorId)
    assertThat(errorWithId.throwable).isEqualTo(throwable)
    assertThat(errorWithId.metadataOrNull<String>()).isEqualTo("metadata")
  }

  @Test
  fun `Error withMetadata preserves errorId`() {
    val throwable = Throwable()
    val errorId = ErrorId("test-error-123")
    val error = Error(throwable, errorId = errorId)
    val errorWithMetadata = error.withMetadata("new-metadata")
    assertThat(errorWithMetadata.errorId).isEqualTo(errorId)
    assertThat(errorWithMetadata.metadataOrNull<String>()).isEqualTo("new-metadata")
  }

  @Test
  fun `ErrorWithId creates an Error with the given errorId`() {
    val errorId = ErrorId("test-error-123")
    val error = ErrorWithId(errorId)
    assertThat(error.errorId).isEqualTo(errorId)
    assertThat(error.throwable).isNull()
    assertThat(error.metadataOrNull<Any>()).isNull()
  }

  @Test
  fun `ErrorWithId creates an Error with errorId and metadata`() {
    val errorId = ErrorId("test-error-123")
    val error = ErrorWithId(errorId, "metadata")
    assertThat(error.errorId).isEqualTo(errorId)
    assertThat(error.metadataOrNull<String>()).isEqualTo("metadata")
  }

  @Test
  fun `ErrorWithMetadata accepts optional errorId`() {
    val errorId = ErrorId("test-error-123")
    val errorWithoutId = ErrorWithMetadata("metadata")
    val errorWithId = ErrorWithMetadata("metadata", errorId)
    assertThat(errorWithoutId.errorId).isNull()
    assertThat(errorWithId.errorId).isEqualTo(errorId)
    assertThat(errorWithId.metadataOrNull<String>()).isEqualTo("metadata")
  }

  @Test
  fun `Error plus operator preserves errorId`() {
    val errorId = ErrorId("test-error-123")
    val error = Error(errorId = errorId)
    val errorWithMetadata = error + "metadata"
    assertThat(errorWithMetadata.errorId).isEqualTo(errorId)
    assertThat(errorWithMetadata.metadataOrNull<String>()).isEqualTo("metadata")
  }

  @Test
  fun `Error plus ErrorId operator adds errorId preserving metadata`() {
    val errorId = ErrorId("test-error-123")
    val error = Error(metadata = "metadata")
    val errorWithId = error + errorId
    assertThat(errorWithId.errorId).isEqualTo(errorId)
    assertThat(errorWithId.metadataOrNull<String>()).isEqualTo("metadata")
  }

  @Test
  fun `Error data class equals includes errorId`() {
    val throwable = Throwable()
    val errorId1 = ErrorId("error-1")
    val errorId2 = ErrorId("error-2")
    val error1 = Error(throwable, "metadata", errorId1)
    val error2 = Error(throwable, "metadata", errorId1)
    val error3 = Error(throwable, "metadata", errorId2)
    assertThat(error1).isEqualTo(error2)
    assertThat(error1 == error3).isFalse()
  }

  @Test
  fun `unwrap returns value when Success else throws UnwrapException`() {
    assertThat(Success(10).unwrap()).isEqualTo(10)
    assertFailsWith<UnwrapException> { NotStarted.unwrap() }
    assertFailsWith<UnwrapException> { Loading.unwrap() }
    assertFailsWith<UnwrapException> { Error(Throwable()).unwrap() }
  }

  @Test
  fun `unwrapError returns error when Error else throws UnwrapException`() {
    val throwable = Throwable()
    assertThat(Error(throwable).unwrapError()).isEqualTo(Error(throwable))
    assertThat(Error(throwable, "metadata").unwrapError()).isEqualTo(Error(throwable, "metadata"))
    assertFailsWith<UnwrapException> { Success(10).unwrapError() }
    assertFailsWith<UnwrapException> { NotStarted.unwrapError() }
    assertFailsWith<UnwrapException> { Loading.unwrapError() }
  }

  @Test
  fun `unwrapThrowable returns throwable when Error has throwable else throws UnwrapException`() {
    val throwable = Throwable()
    assertThat(Error(throwable).unwrapThrowable()).isEqualTo(throwable)
    assertFailsWith<UnwrapException> { Error(metadata = "metadata").unwrapThrowable() }
    assertFailsWith<UnwrapException> { Success(10).unwrapThrowable() }
    assertFailsWith<UnwrapException> { NotStarted.unwrapThrowable() }
    assertFailsWith<UnwrapException> { Loading.unwrapThrowable() }
  }

  @Test
  fun `unwrapMetadata returns metadata when Error has matching metadata type else throws UnwrapException`() {
    assertThat(Error(metadata = "metadata").unwrapMetadata<String>()).isEqualTo("metadata")
    assertFailsWith<UnwrapException> { Error(metadata = 1234).unwrapMetadata<String>() }
    assertFailsWith<UnwrapException> { Error(throwable = Throwable()).unwrapMetadata<String>() }
    assertFailsWith<UnwrapException> { Success(10).unwrapMetadata<String>() }
    assertFailsWith<UnwrapException> { NotStarted.unwrapMetadata<String>() }
    assertFailsWith<UnwrapException> { Loading.unwrapMetadata<String>() }
  }

  @Test
  fun `unwrapErrorId returns errorId when Error has errorId else throws UnwrapException`() {
    val errorId = ErrorId("test-error-123")
    assertThat(Error(errorId = errorId).unwrapErrorId()).isEqualTo(errorId)
    assertFailsWith<UnwrapException> { Error(throwable = Throwable()).unwrapErrorId() }
    assertFailsWith<UnwrapException> { Error(metadata = "metadata").unwrapErrorId() }
    assertFailsWith<UnwrapException> { Success(10).unwrapErrorId() }
    assertFailsWith<UnwrapException> { NotStarted.unwrapErrorId() }
    assertFailsWith<UnwrapException> { Loading.unwrapErrorId() }
  }

  @Test
  fun `expect returns value when Success else throws UnwrapException with custom message`() {
    assertThat(Success(10).expect { "Custom error" }).isEqualTo(10)
    val exception = assertFailsWith<UnwrapException> { NotStarted.expect { "Custom error" } }
    assertThat(exception.message).isEqualTo("Custom error")
  }

  @Test
  fun `expectError returns error when Error else throws UnwrapException with custom message`() {
    val throwable = Throwable()
    assertThat(Error(throwable).expectError { "Custom error" }).isEqualTo(Error(throwable))
    val exception = assertFailsWith<UnwrapException> { Success(10).expectError { "Custom error" } }
    assertThat(exception.message).isEqualTo("Custom error")
  }

  @Test
  fun `expectThrowable returns throwable when Error has throwable else throws UnwrapException with custom message`() {
    val throwable = Throwable()
    assertThat(Error(throwable).expectThrowable { "Custom error" }).isEqualTo(throwable)
    val exception =
        assertFailsWith<UnwrapException> {
          Error(metadata = "metadata").expectThrowable { "Custom error" }
        }
    assertThat(exception.message).isEqualTo("Custom error")
  }

  @Test
  fun `expectMetadata returns metadata else throws UnwrapException with custom message`() {
    assertThat(Error(metadata = "metadata").expectMetadata<String> { "Custom error" })
        .isEqualTo("metadata")
    val exception =
        assertFailsWith<UnwrapException> {
          Error(metadata = 1234).expectMetadata<String> { "Custom error" }
        }
    assertThat(exception.message).isEqualTo("Custom error")
  }

  @Test
  fun `expectErrorId returns errorId else throws UnwrapException with custom message`() {
    val errorId = ErrorId("test-error-123")
    assertThat(Error(errorId = errorId).expectErrorId { "Custom error" }).isEqualTo(errorId)
    val exception =
        assertFailsWith<UnwrapException> {
          Error(throwable = Throwable()).expectErrorId { "Custom error" }
        }
    assertThat(exception.message).isEqualTo("Custom error")
  }

  @Test
  fun `mapSuccess transforms the value when success else propagates the rest of states`() {
    assertThat(Success(10).mapSuccess { 1 }).isEqualTo(Success(1))

    assertThat(NotStarted.mapSuccess { 1 }).isEqualTo(NotStarted)
    assertThat(Loading.mapSuccess { 1 }).isEqualTo(Loading)
    val throwable = Throwable()
    assertThat(Error(throwable).mapSuccess { 1 }).isEqualTo(Error(throwable))
  }

  @Test
  fun `mapError transforms the error into other errors else propagates the rest of states`() {
    val throwable = Throwable()
    assertThat(Error(throwable).mapError { it.withMetadata("boo") })
        .isEqualTo(Error(throwable, "boo"))

    assertThat((NotStarted as AsyncResult<Int>).mapError { it.withMetadata("boo") })
        .isEqualTo(NotStarted)
    assertThat(Success(10).mapError { it.withMetadata("boo") }).isEqualTo(Success(10))
    assertThat((Loading as AsyncResult<Int>).mapError { it.withMetadata("boo") }).isEqualTo(Loading)
  }

  @Test
  fun `map transforms the AsyncResult to any other AsyncResult you want`() {
    assertThat((NotStarted as AsyncResult<Int>).map { Success("potato") })
        .isEqualTo(Success("potato"))
    assertThat(Success(10).map { Loading }).isEqualTo(Loading)
    assertThat((Loading as AsyncResult<Int>).map { Error(metadata = "boo") })
        .isEqualTo(Error(metadata = "boo"))
    val throwable = Throwable()
    assertThat(Error(throwable).map { NotStarted }).isEqualTo(NotStarted)
  }

  @Test
  fun `fold transforms values depending on the state`() {
    assertThat(
            Success(10)
                .fold(
                    ifNotStarted = { error("") },
                    ifLoading = { error("") },
                    ifSuccess = {
                      assertThat(it).isEqualTo(10)
                      1
                    },
                    ifError = { error("") },
                ),
        )
        .isEqualTo(1)
    assertThat(
            NotStarted.fold(
                ifNotStarted = { 2 },
                ifLoading = { error("") },
                ifSuccess = { error("") },
                ifError = { error("") },
            ),
        )
        .isEqualTo(2)
    assertThat(
            Loading.fold(
                ifNotStarted = { error("") },
                ifLoading = { 3 },
                ifSuccess = { error("") },
                ifError = { error("") },
            ),
        )
        .isEqualTo(3)
    val throwable = Throwable()
    assertThat(
            Error(throwable)
                .fold(
                    ifNotStarted = { error("") },
                    ifLoading = { error("") },
                    ifSuccess = { error("") },
                    ifError = {
                      assertThat(it).isEqualTo(Error(throwable))
                      4
                    },
                ),
        )
        .isEqualTo(4)
  }

  @Test
  fun `flatMap transforms the AsyncResult`() {
    assertThat(Success(10).flatMap { Success("yay") }).isEqualTo(Success("yay"))
    assertThat(NotStarted.flatMap { Success("yay") }).isEqualTo(NotStarted)
    assertThat(Loading.flatMap { Success("yay") }).isEqualTo(Loading)
    val throwable = Throwable()
    assertThat(Error(throwable).flatMap { Success("yay") }).isEqualTo(Error(throwable))
  }

  @Test
  fun `flatten will transform a AsyncResult of AsyncResult to the AsyncResult of the inner value`() {
    assertThat(Success(Success(10)).flatten()).isEqualTo(Success(10))
    assertThat((NotStarted as AsyncResult<AsyncResult<Int>>).flatten()).isEqualTo(NotStarted)
    assertThat((Loading as AsyncResult<AsyncResult<Int>>).flatten()).isEqualTo(Loading)
    val throwable = Throwable()
    assertThat((Error(throwable) as AsyncResult<AsyncResult<Int>>).flatten())
        .isEqualTo(Error(throwable))
  }

  @Test
  fun `andThen transforms the AsyncResult`() {
    assertThat(Success(10).andThen { Success("yay") }).isEqualTo(Success("yay"))
    assertThat(NotStarted.andThen { Success("yay") }).isEqualTo(NotStarted)
    assertThat(Loading.andThen { Success("yay") }).isEqualTo(Loading)
    val throwable = Throwable()
    assertThat(Error(throwable).andThen { Success("yay") }).isEqualTo(Error(throwable))
  }

  @Test
  fun `andThen with lambda transforms the AsyncResult`() {
    assertThat({ Success(10) }.andThen { Success("yay") }).isEqualTo(Success("yay"))
    assertThat({ NotStarted }.andThen { Success("yay") }).isEqualTo(NotStarted)
    assertThat({ Loading }.andThen { Success("yay") }).isEqualTo(Loading)
    val throwable = Throwable()
    assertThat({ Error(throwable) }.andThen { Success("yay") }).isEqualTo(Error(throwable))
  }

  @Test
  fun `and returns the result param if success or this if failed`() {
    assertThat(Success(10).and(Success(20))).isEqualTo(Success(20))
    assertThat(NotStarted.and(Success("yay"))).isEqualTo(NotStarted)
    assertThat(Loading.and(Success("yay"))).isEqualTo(Loading)
    val throwable = Throwable()
    assertThat(Error(throwable).and(Success("yay"))).isEqualTo(Error(throwable))
  }

  @Test
  fun `getAllThrowables returns all errors with throwables from a list of AsyncResult`() {
    val error1 = Throwable()
    val error2 = Throwable()
    val results =
        listOf(
            Success(10),
            Error(error1),
            Loading,
            NotStarted,
            Error(error2),
            Success("abc"),
        )
    assertThat(results.getAllThrowables()).containsExactly(error1, error2)
  }

  @Test
  fun `getAllErrors returns all errors from a list of AsyncResult`() {
    val error1 = Throwable()
    val error2 = Throwable()
    val results =
        listOf(
            Success(10),
            Error(error1),
            Loading,
            NotStarted,
            Error(error2, "bleh"),
            Success("abc"),
        )
    assertThat(results.getAllErrors()).containsExactly(Error(error1), Error(error2, "bleh"))
  }

  @Test
  fun `errorsFrom returns all errors from a list of AsyncResult`() {
    val error1 = Throwable()
    val error2 = Throwable()
    val results =
        errorsFrom(
            Success<Int>(10),
            Error(error1),
            Loading,
            NotStarted,
            Error(error2, "bleh"),
            Success("abc"),
        )
    assertThat(results).containsExactly(Error(error1), Error(error2, "bleh"))
  }

  @Test
  fun `metadata returns all metadata matching with the given type`() {
    val results =
        listOf(
            Error(metadata = 1234),
            Error(metadata = "bleh"),
            Error(metadata = 1234L),
        )
    assertThat(results.metadata<String>()).containsExactly("bleh")
  }

  @Test
  fun `anyLoading returns true if any result is loading or false if not`() {
    assertThat(listOf(Success(10), Loading, Loading, Loading, Error(Throwable())).anyLoading())
        .isTrue()
    assertThat(listOf(Success(10), Loading).anyLoading()).isTrue()
    assertThat(listOf(Success(10), Error(Throwable()), NotStarted).anyLoading()).isFalse()

    assertThat(anyLoading(Success(10), Loading, Loading, Loading, Error(Throwable()))).isTrue()
    assertThat(anyLoading(Success(10), Loading)).isTrue()
    assertThat(anyLoading(Success(10), Error(Throwable()), NotStarted)).isFalse()
  }

  @Test
  fun `anyIncomplete returns true if any result is loading or not started`() {
    assertThat(listOf(Success(10), NotStarted, Loading, Error(Throwable())).anyIncomplete())
        .isTrue()
    assertThat(listOf(Success(10), Loading).anyIncomplete()).isTrue()
    assertThat(listOf(Success(10), Error(Throwable()), NotStarted).anyIncomplete()).isTrue()
    assertThat(listOf(Success(10), Error(Throwable())).anyIncomplete()).isFalse()

    assertThat(anyIncomplete(Success(10), NotStarted, Loading, Error(Throwable()))).isTrue()
    assertThat(anyIncomplete(Success(10), Loading)).isTrue()
    assertThat(anyIncomplete(Success(10), Error(Throwable()), NotStarted)).isTrue()
    assertThat(anyIncomplete(Success(10), Error(Throwable()))).isFalse()
  }

  @Test
  fun `zip transforms the AsyncResult using the given transformer`() {
    val lcr =
        zip({ Success(30) }, { Success('b') }) { value1, value2 ->
          "value1: $value1 and value2: $value2"
        }
    assertThat(lcr).isEqualTo(Success("value1: 30 and value2: b"))
  }

  @Test
  fun `zip with 3 elements transforms the AsyncResult using the given transformer`() {
    val lcr =
        zip({ Success(30) }, { Success('b') }, { Success("test") }) { value1, value2, value3 ->
          "value1: $value1, value2: $value2, value3: $value3"
        }
    assertThat(lcr).isEqualTo(Success("value1: 30, value2: b, value3: test"))
  }

  @Test
  fun `zip with 4 elements transforms the AsyncResult using the given transformer`() {
    val lcr =
        zip(
            { Success(30) },
            { Success('b') },
            { Success("test") },
            { Success(true) },
        ) { value1, value2, value3, value4 ->
          "value1: $value1, value2: $value2, value3: $value3, value4: $value4"
        }
    assertThat(lcr).isEqualTo(Success("value1: 30, value2: b, value3: test, value4: true"))
  }

  @Test
  fun `zipWith with producer transforms the AsyncResult using the given transformer`() {
    val lcr =
        { Success(30) }.zipWith({ Success('b') }) { value1, value2 ->
          "value1: $value1 and value2: $value2"
        }
    assertThat(lcr).isEqualTo(Success("value1: 30 and value2: b"))
  }

  @Test
  fun `zipWith with producer and 3 elements transforms the AsyncResult using the given transformer`() {
    val lcr =
        { Success(30) }.zipWith({ Success('b') }, { Success("test") }) { value1, value2, value3 ->
          "value1: $value1, value2: $value2, value3: $value3"
        }
    assertThat(lcr).isEqualTo(Success("value1: 30, value2: b, value3: test"))
  }

  @Test
  fun `zipWith with producer and 4 elements transforms the AsyncResult using the given transformer`() {
    val lcr =
        { Success(30) }.zipWith(
            { Success('b') },
            { Success("test") },
            { Success(true) },
        ) { value1, value2, value3, value4 ->
          "value1: $value1, value2: $value2, value3: $value3, value4: $value4"
        }
    assertThat(lcr).isEqualTo(Success("value1: 30, value2: b, value3: test, value4: true"))
  }

  @Test
  fun `zipWith transforms the AsyncResult using the given transformer`() {
    val lcr =
        Success(30).zipWith(Success('b')) { value1, value2 ->
          "value1: $value1 and value2: $value2"
        }
    assertThat(lcr).isEqualTo(Success("value1: 30 and value2: b"))
  }

  @Test
  fun `zipWith with 3 elements transforms the AsyncResult using the given transformer`() {
    val lcr =
        Success(30).zipWith(Success('b'), Success("test")) { value1, value2, value3 ->
          "value1: $value1, value2: $value2, value3: $value3"
        }
    assertThat(lcr).isEqualTo(Success("value1: 30, value2: b, value3: test"))
  }

  @Test
  fun `zipWith with 4 elements transforms the AsyncResult using the given transformer`() {
    val lcr =
        Success(30).zipWith(
            Success('b'),
            Success("test"),
            Success(true),
        ) { value1, value2, value3, value4 ->
          "value1: $value1, value2: $value2, value3: $value3, value4: $value4"
        }
    assertThat(lcr).isEqualTo(Success("value1: 30, value2: b, value3: test, value4: true"))
  }

  @Test
  fun `orError returns Error if value is null`() {
    // Null successful values turn into error (and make the AsyncResult type change from nullable to
    // non-nullable)
    assertThat(Success<Int?>(null).orError()).isInstanceOf<Error>()
    // Other cases stay the same
    assertThat(Success<Int?>(10).orError()).isEqualTo(Success(10))
    assertThat((NotStarted as AsyncResult<Int?>).orError()).isEqualTo(NotStarted)
    assertThat((Loading as AsyncResult<Int?>).orError()).isEqualTo(Loading)
    val throwable = Throwable()
    assertThat((Error(throwable) as AsyncResult<Int?>).orError()).isEqualTo(Error(throwable))
  }

  @Test
  fun `orError returns Error if value is null and adds metadata when provided`() {
    val success = Success<Int?>(null).orError { "Boo" }
    assertThat(success).isInstanceOf<Error>().transform { it.metadata }.isEqualTo("Boo")
  }

  @Test
  fun `metadataOrNull will extract metadata from an Error if it exists and has the provided type`() {
    assertThat(Error(Throwable()).metadataOrNull<String>()).isNull()
    assertThat(Error(Throwable(), 1234).metadataOrNull<String>()).isNull()
    assertThat(Error(Throwable(), "boo").metadataOrNull<String>()).isEqualTo("boo")
  }

  @Test
  fun `Error withMetadata will append metadata to an Error`() {
    val throwable = Throwable()
    assertThat(Error(throwable).withMetadata("boo")).isEqualTo(Error(throwable, "boo"))
    assertThat(Error(throwable) + "boo").isEqualTo(Error(throwable, "boo"))
  }

  @Test
  fun `ErrorWithMetadata creates an Error with only metadata in it`() {
    assertThat(ErrorWithMetadata("boo")).isEqualTo(Error(metadata = "boo"))
  }

  private interface A

  private class B : A

  @Test
  fun `castOrError will cast the AsyncResult type if possible`() {
    val b = B()
    // downcast
    assertThat(Success<A>(b).castOrError<B>()).isEqualTo(Success(b))
    // upcast
    assertThat(Success(b).castOrError<A>()).isEqualTo(Success<A>(b))
  }

  @Test
  fun `castOrError will generate an Error if the casting is not possible`() {
    assertThat(Success(10).castOrError<String> { "Nope!" })
        .isInstanceOf<Error>()
        .transform { it.metadata }
        .isEqualTo("Nope!")
  }

  @Test
  fun `verify will pass if predicate matches`() {
    assertThat(Success(10).filterOrError { it > 0 }).isEqualTo(Success(10))
  }

  @Test
  fun `verify will generate an error if predicate does not match`() {
    assertThat(Success(10).filterOrError(lazyMetadata = { "Wrong" }) { it < 0 })
        .isInstanceOf<Error>()
        .transform { it.metadata }
        .isEqualTo("Wrong")
  }

  @Test
  fun `Flow onLoading will run action if loading`() = runTest {
    var count = 0
    flowOf<AsyncResult<Int>>(NotStarted).onLoading { count++ }.collect()
    assertThat(count).isEqualTo(0)
    flowOf<AsyncResult<Int>>(Loading).onLoading { count++ }.collect()
    assertThat(count).isEqualTo(1)
    flowOf<AsyncResult<Int>>(Success(10)).onLoading { count++ }.collect()
    assertThat(count).isEqualTo(1)
    flowOf<AsyncResult<Int>>(Error()).onLoading { count++ }.collect()
    assertThat(count).isEqualTo(1)
  }

  @Test
  fun `Flow onSuccess will run action if success`() = runTest {
    var count = 0
    flowOf<AsyncResult<Int>>(NotStarted).onSuccess { count++ }.collect()
    assertThat(count).isEqualTo(0)
    flowOf<AsyncResult<Int>>(Loading).onSuccess { count++ }.collect()
    assertThat(count).isEqualTo(0)
    flowOf<AsyncResult<Int>>(Success(10))
        .onSuccess {
          assertThat(it).isEqualTo(10)
          count++
        }
        .collect()
    assertThat(count).isEqualTo(1)
    flowOf<AsyncResult<Int>>(Error()).onSuccess { count++ }.collect()
    assertThat(count).isEqualTo(1)
  }

  @Test
  fun `Flow onError will run action if error`() = runTest {
    val error = Error(throwable = Throwable("boo"), metadata = "metadata")
    var count = 0
    flowOf<AsyncResult<Int>>(NotStarted).onError { count++ }.collect()
    assertThat(count).isEqualTo(0)
    flowOf<AsyncResult<Int>>(Loading).onError { count++ }.collect()
    assertThat(count).isEqualTo(0)
    flowOf<AsyncResult<Int>>(Success(10)).onError { count++ }.collect()
    assertThat(count).isEqualTo(0)
    flowOf<AsyncResult<Int>>(error)
        .onError {
          assertThat(it).isEqualTo(error)
          count++
        }
        .collect()
    assertThat(count).isEqualTo(1)
  }

  @Test
  fun `onSuccess will run action if success`() {
    var count = 0
    (NotStarted as AsyncResult<Int>).onSuccess { count++ }
    assertThat(count).isEqualTo(0)

    (Loading as AsyncResult<Int>).onSuccess { count++ }
    assertThat(count).isEqualTo(0)

    var value = 0
    Success(10).onSuccess {
      assertThat(it).isEqualTo(10)
      value = it
      count++
    }
    assertThat(count).isEqualTo(1)
    assertThat(value).isEqualTo(10)

    Error().onSuccess { count++ }
    assertThat(count).isEqualTo(1)
  }

  @Test
  fun `onLoading will run action if loading`() {
    var count = 0
    (NotStarted as AsyncResult<Int>).onLoading { count++ }
    assertThat(count).isEqualTo(0)

    (Loading as AsyncResult<Int>).onLoading { count++ }
    assertThat(count).isEqualTo(1)

    Success(10).onLoading { count++ }
    assertThat(count).isEqualTo(1)

    Error().onLoading { count++ }
    assertThat(count).isEqualTo(1)
  }

  @Test
  fun `onError will run action if error`() {
    val throwable = Throwable("test error")
    var count = 0

    (NotStarted as AsyncResult<Int>).onError { count++ }
    assertThat(count).isEqualTo(0)

    (Loading as AsyncResult<Int>).onError { count++ }
    assertThat(count).isEqualTo(0)

    Success(10).onError { count++ }
    assertThat(count).isEqualTo(0)

    var capturedThrowable: Throwable? = null
    Error(throwable).onError {
      capturedThrowable = it
      count++
    }
    assertThat(count).isEqualTo(1)
    assertThat(capturedThrowable).isEqualTo(throwable)
  }

  @Test
  fun `onErrorWithMetadata will run action if error with matching metadata type`() {
    val throwable = Throwable("test error")
    val metadata = "error metadata"
    var count = 0

    (NotStarted as AsyncResult<Int>).onErrorWithMetadata<Int, String> { _, _ -> count++ }
    assertThat(count).isEqualTo(0)

    (Loading as AsyncResult<Int>).onErrorWithMetadata<Int, String> { _, _ -> count++ }
    assertThat(count).isEqualTo(0)

    Success(10).onErrorWithMetadata<Int, String> { _, _ -> count++ }
    assertThat(count).isEqualTo(0)

    var capturedThrowable: Throwable? = null
    var capturedMetadata: String? = null
    Error(throwable, metadata).onErrorWithMetadata<Int, String> { t, m ->
      capturedThrowable = t
      capturedMetadata = m
      count++
    }
    assertThat(count).isEqualTo(1)
    assertThat(capturedThrowable).isEqualTo(throwable)
    assertThat(capturedMetadata).isEqualTo(metadata)

    // Test with wrong metadata type
    Error(throwable, 123).onErrorWithMetadata<Int, String> { _, m ->
      capturedMetadata = m
      count++
    }
    assertThat(count).isEqualTo(2)
    assertThat(capturedMetadata).isNull()
  }

  @Test
  fun `onNotStarted will run action if not started`() {
    var count = 0
    (NotStarted as AsyncResult<Int>).onNotStarted { count++ }
    assertThat(count).isEqualTo(1)

    (Loading as AsyncResult<Int>).onNotStarted { count++ }
    assertThat(count).isEqualTo(1)

    Success(10).onNotStarted { count++ }
    assertThat(count).isEqualTo(1)

    Error().onNotStarted { count++ }
    assertThat(count).isEqualTo(1)
  }

  @Test
  fun `Flow getOrThrow returns value on Success and throws on Error`() = runTest {
    val successFlow = flowOf<AsyncResult<Int>>(Success(42))
    assertThat(successFlow.getOrThrow()).isEqualTo(42)

    val errorFlow = flowOf<AsyncResult<Int>>(Error(Throwable("test error")))
    assertFailsWith<IllegalStateException> { errorFlow.getOrThrow() }
  }

  @Test
  fun `Flow getOrNull returns value on Success and null on Error`() = runTest {
    val successFlow = flowOf<AsyncResult<Int>>(Success(42))
    assertThat(successFlow.getOrNull()).isEqualTo(42)

    val errorFlow = flowOf<AsyncResult<Int>>(Error(Throwable("test error")))
    assertThat(errorFlow.getOrNull()).isNull()
  }

  @Test
  fun `Flow getOrElse returns value on Success and transforms the Error otherwise`() = runTest {
    val successFlow = flowOf<AsyncResult<Int>>(Success(42))
    assertThat(successFlow.getOrElse { 0 }).isEqualTo(42)

    val error = Throwable("test error")
    val errorFlow = flowOf<AsyncResult<Int>>(Error(error))
    assertThat(
            errorFlow.getOrElse {
              assertThat(it.throwable).isEqualTo(error)
              999
            },
        )
        .isEqualTo(999)
  }

  @Test
  fun `destructuring for AsyncResult of Pairs`() {
    // Success case
    val success: AsyncResult<Pair<Int, String>> = Success(Pair(1, "a"))
    val (first, second) = success
    assertThat(first).isEqualTo(Success(1))
    assertThat(second).isEqualTo(Success("a"))

    // Loading case
    val loading: AsyncResult<Pair<Int, String>> = Loading
    val (firstLoading, secondLoading) = loading
    assertThat(firstLoading).isEqualTo(Loading)
    assertThat(secondLoading).isEqualTo(Loading)

    // NotStarted case
    val notStarted: AsyncResult<Pair<Int, String>> = NotStarted
    val (firstNotStarted, secondNotStarted) = notStarted
    assertThat(firstNotStarted).isEqualTo(NotStarted)
    assertThat(secondNotStarted).isEqualTo(NotStarted)

    // Error case
    val throwable = Throwable("test error")
    val error: AsyncResult<Pair<Int, String>> = Error(throwable)
    val (firstError, secondError) = error
    assertThat(firstError).isEqualTo(Error(throwable))
    assertThat(secondError).isEqualTo(Error(throwable))
  }

  @Test
  fun `destructuring for AsyncResult of Triples`() {
    // Success case
    val success: AsyncResult<Triple<Int, String, Boolean>> = Success(Triple(1, "a", true))
    val (first, second, third) = success
    assertThat(first).isEqualTo(Success(1))
    assertThat(second).isEqualTo(Success("a"))
    assertThat(third).isEqualTo(Success(true))

    // Loading case
    val loading: AsyncResult<Triple<Int, String, Boolean>> = Loading
    val (firstLoading, secondLoading, thirdLoading) = loading
    assertThat(firstLoading).isEqualTo(Loading)
    assertThat(secondLoading).isEqualTo(Loading)
    assertThat(thirdLoading).isEqualTo(Loading)

    // NotStarted case
    val notStarted: AsyncResult<Triple<Int, String, Boolean>> = NotStarted
    val (firstNotStarted, secondNotStarted, thirdNotStarted) = notStarted
    assertThat(firstNotStarted).isEqualTo(NotStarted)
    assertThat(secondNotStarted).isEqualTo(NotStarted)
    assertThat(thirdNotStarted).isEqualTo(NotStarted)

    // Error case
    val throwable = Throwable("test error")
    val error: AsyncResult<Triple<Int, String, Boolean>> = Error(throwable)
    val (firstError, secondError, thirdError) = error
    assertThat(firstError).isEqualTo(Error(throwable))
    assertThat(secondError).isEqualTo(Error(throwable))
    assertThat(thirdError).isEqualTo(Error(throwable))
  }

  @Test
  fun `spread transforms Pair AsyncResult into Pair of AsyncResults`() {
    // Success case
    val successPair = Success(Pair(10, "hello"))
    val (first, second) = successPair.spread()
    assertThat(first).isEqualTo(Success(10))
    assertThat(second).isEqualTo(Success("hello"))

    // NotStarted case
    val notStartedPair = NotStarted as AsyncResult<Pair<Int, String>>
    val (nsFirst, nsSecond) = notStartedPair.spread()
    assertThat(nsFirst).isEqualTo(NotStarted)
    assertThat(nsSecond).isEqualTo(NotStarted)

    // Loading case
    val loadingPair = Loading as AsyncResult<Pair<Int, String>>
    val (loadFirst, loadSecond) = loadingPair.spread()
    assertThat(loadFirst).isEqualTo(Loading)
    assertThat(loadSecond).isEqualTo(Loading)

    // Error case
    val throwable = Throwable("test error")
    val errorPair = Error(throwable) as AsyncResult<Pair<Int, String>>
    val (errFirst, errSecond) = errorPair.spread()
    assertThat(errFirst).isEqualTo(Error(throwable))
    assertThat(errSecond).isEqualTo(Error(throwable))
  }

  @Test
  fun `spread transforms Triple AsyncResult into Triple of AsyncResults`() {
    // Success case
    val successTriple = Success(Triple(10, "hello", true))
    val (first, second, third) = successTriple.spread()
    assertThat(first).isEqualTo(Success(10))
    assertThat(second).isEqualTo(Success("hello"))
    assertThat(third).isEqualTo(Success(true))

    // NotStarted case
    val notStartedTriple = NotStarted as AsyncResult<Triple<Int, String, Boolean>>
    val (nsFirst, nsSecond, nsThird) = notStartedTriple.spread()
    assertThat(nsFirst).isEqualTo(NotStarted)
    assertThat(nsSecond).isEqualTo(NotStarted)
    assertThat(nsThird).isEqualTo(NotStarted)

    // Loading case
    val loadingTriple = Loading as AsyncResult<Triple<Int, String, Boolean>>
    val (loadFirst, loadSecond, loadThird) = loadingTriple.spread()
    assertThat(loadFirst).isEqualTo(Loading)
    assertThat(loadSecond).isEqualTo(Loading)
    assertThat(loadThird).isEqualTo(Loading)

    // Error case
    val throwable = Throwable("test error")
    val errorTriple = Error(throwable) as AsyncResult<Triple<Int, String, Boolean>>
    val (errFirst, errSecond, errThird) = errorTriple.spread()
    assertThat(errFirst).isEqualTo(Error(throwable))
    assertThat(errSecond).isEqualTo(Error(throwable))
    assertThat(errThird).isEqualTo(Error(throwable))
  }
}
