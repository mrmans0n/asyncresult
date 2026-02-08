// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult.either

import arrow.core.raise.Raise
import io.nlopez.asyncresult.AsyncResult
import io.nlopez.asyncresult.Error
import io.nlopez.asyncresult.Loading
import io.nlopez.asyncresult.NotStarted
import io.nlopez.asyncresult.Success

/**
 * Binds an [AsyncResult] inside an Arrow [Raise] scope.
 * - [Success] returns the value.
 * - [Error], [Loading], and [NotStarted] raise an [Error] into the [Raise] scope.
 */
// ktfmt does not yet support context parameters, so we use an extension on Raise instead.
public inline fun <T> Raise<Error>.bind(result: AsyncResult<T>): T =
    when (result) {
      is Success -> result.value
      is Error -> raise(result)
      is Loading -> raise(Error(metadata = Loading))
      is NotStarted -> raise(Error(metadata = NotStarted))
    }
