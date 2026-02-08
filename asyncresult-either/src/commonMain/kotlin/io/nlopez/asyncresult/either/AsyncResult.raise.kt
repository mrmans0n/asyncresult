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

context(raise: Raise<Error>)
public inline fun <T> AsyncResult<T>.bind(): T =
    when (this) {
      is Success -> value
      is Error -> raise.raise(this)
      is Loading -> raise.raise(Error(metadata = Loading))
      is NotStarted -> raise.raise(Error(metadata = NotStarted))
    }
