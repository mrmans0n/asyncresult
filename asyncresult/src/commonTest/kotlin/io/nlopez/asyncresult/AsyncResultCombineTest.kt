// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
package io.nlopez.asyncresult

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

class AsyncResultCombineTest {
  @Test
  fun `combine returns Success with list when all are Success`() {
    val results = listOf(Success(1), Success(2), Success(3))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Success<List<Int>>>()
    assertThat((combined as Success).value).containsExactly(1, 2, 3)
  }

  @Test
  fun `combine returns Error when any is Error`() {
    val error = Error(Exception("Failed"))
    val results = listOf(Success(1), error, Success(3))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Error>()
    assertThat(combined as Error).isEqualTo(error)
  }

  @Test
  fun `combine returns first Error when multiple errors`() {
    val error1 = Error(Exception("First"))
    val error2 = Error(Exception("Second"))
    val results = listOf(Success(1), error1, error2)
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Error>()
    assertThat((combined as Error).throwable?.message).isEqualTo("First")
  }

  @Test
  fun `combine returns Loading when any is Loading`() {
    val results = listOf(Success(1), Loading, Success(3))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Loading>()
  }

  @Test
  fun `combine returns NotStarted when any is NotStarted`() {
    val results = listOf(Success(1), NotStarted, Success(3))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<NotStarted>()
  }

  @Test
  fun `combine prioritizes Error over Loading`() {
    val error = Error(Exception("Failed"))
    val results = listOf(Success(1), error, Loading)
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Error>()
  }

  @Test
  fun `combine prioritizes Error over NotStarted`() {
    val error = Error(Exception("Failed"))
    val results = listOf(Success(1), error, NotStarted)
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Error>()
  }

  @Test
  fun `combine prioritizes Loading over NotStarted`() {
    val results = listOf(Success(1), Loading, NotStarted)
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Loading>()
  }

  @Test
  fun `combine works with empty list`() {
    val results = emptyList<AsyncResult<Int>>()
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Success<List<Int>>>()
    assertThat((combined as Success).value).containsExactly()
  }

  @Test
  fun `combine works with single element`() {
    val results = listOf(Success(42))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Success<List<Int>>>()
    assertThat((combined as Success).value).containsExactly(42)
  }

  @Test
  fun `combine preserves order`() {
    val results = listOf(Success(3), Success(1), Success(2))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Success<List<Int>>>()
    assertThat((combined as Success).value).containsExactly(3, 1, 2)
  }

  @Test
  fun `combine works with different types`() {
    val results = listOf(Success("a"), Success("b"), Success("c"))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Success<List<String>>>()
    assertThat((combined as Success).value).containsExactly("a", "b", "c")
  }

  // Iterable combine tests

  @Test
  fun `combine on Iterable returns Success with list when all are Success`() {
    val results: Iterable<AsyncResult<Int>> = setOf(Success(1), Success(2), Success(3))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Success<List<Int>>>()
    assertThat((combined as Success).value.toSet()).isEqualTo(setOf(1, 2, 3))
  }

  @Test
  fun `combine on Iterable returns Error when any is Error`() {
    val error = Error(Exception("Failed"))
    val results: Iterable<AsyncResult<Int>> = listOf(Success(1), error, Success(3))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Error>()
    assertThat(combined as Error).isEqualTo(error)
  }

  @Test
  fun `combine on Iterable returns Loading when any is Loading`() {
    val results: Iterable<AsyncResult<Int>> = listOf(Success(1), Loading, Success(3))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Loading>()
  }

  @Test
  fun `combine on Iterable returns NotStarted when any is NotStarted`() {
    val results: Iterable<AsyncResult<Int>> = listOf(Success(1), NotStarted, Success(3))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<NotStarted>()
  }

  @Test
  fun `combine on Iterable works with sequence`() {
    val results: Sequence<AsyncResult<Int>> = sequenceOf(Success(1), Success(2), Success(3))
    val combined = results.combine()

    assertThat(combined).isInstanceOf<Success<List<Int>>>()
    assertThat((combined as Success).value).containsExactly(1, 2, 3)
  }

  // sequence alias tests

  @Test
  fun `sequence is an alias for combine`() {
    val results = listOf(Success(1), Success(2), Success(3))
    val combined = results.combine()
    val sequenced = results.sequence()

    assertThat(sequenced).isEqualTo(combined)
  }

  @Test
  fun `sequence returns Success with list when all are Success`() {
    val results = listOf(Success(1), Success(2), Success(3))
    val sequenced = results.sequence()

    assertThat(sequenced).isInstanceOf<Success<List<Int>>>()
    assertThat((sequenced as Success).value).containsExactly(1, 2, 3)
  }

  @Test
  fun `sequence returns Error when any is Error`() {
    val error = Error(Exception("Failed"))
    val results = listOf(Success(1), error, Success(3))
    val sequenced = results.sequence()

    assertThat(sequenced).isInstanceOf<Error>()
  }

  // Real-world use case tests

  @Test
  fun `combine multiple API results`() {
    data class User(val id: Int, val name: String)
    data class Post(val userId: Int, val title: String)
    data class Comment(val postId: Int, val text: String)

    val userResult: AsyncResult<User> = Success(User(1, "Alice"))
    val postResult: AsyncResult<Post> = Success(Post(1, "Hello"))
    val commentResult: AsyncResult<Comment> = Success(Comment(1, "Nice!"))

    val combined = listOf(userResult, postResult, commentResult).combine()

    assertThat(combined).isInstanceOf<Success<List<Any>>>()
    val list = (combined as Success).value
    assertThat(list[0]).isEqualTo(User(1, "Alice"))
    assertThat(list[1]).isEqualTo(Post(1, "Hello"))
    assertThat(list[2]).isEqualTo(Comment(1, "Nice!"))
  }

  @Test
  fun `combine with transformation`() {
    val results = listOf(Success(1), Success(2), Success(3))
    val combined = results.combine().mapSuccess { list -> list.sum() }

    assertThat(combined).isInstanceOf<Success<Int>>()
    assertThat((combined as Success).value).isEqualTo(6)
  }
}
