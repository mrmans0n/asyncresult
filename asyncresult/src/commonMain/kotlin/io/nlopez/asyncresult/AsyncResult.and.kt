@file:Suppress("NOTHING_TO_INLINE")

package io.nlopez.asyncresult

/**
 * Returns the [transform] result value if both the receiver and the transformed value are [Success].
 */
inline fun <R, reified T> AsyncResult<R>.andThen(
    noinline transform: (R) -> AsyncResult<T>,
): AsyncResult<T> = flatMap(transform)

/**
 * Returns the [result] if the receiver is [Success].
 */
inline fun <R> AsyncResult<R>.and(result: AsyncResult<R>): AsyncResult<R> = when (this) {
    is Success -> result
    else -> this
}

/**
 * Returns the [transform] result value if the receiver function returned a [Success] value. This value will be
 * passed to the [transform] function.
 */
inline fun <T1, reified T2> (() -> AsyncResult<T1>).andThen(
    noinline transform: (T1) -> AsyncResult<T2>,
): AsyncResult<T2> = invoke().andThen(transform)
