@file:OptIn(ExperimentalContracts::class)

package io.nlopez.asyncresult

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Combines two [AsyncResult]s ([result1] and [result2]) into a single one, by applying the [transform]
 * to their values.
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA")
inline fun <R1, R2, reified T> zip(
    crossinline result1: () -> AsyncResult<R1>,
    crossinline result2: () -> AsyncResult<R2>,
    crossinline transform: (R1, R2) -> T,
): AsyncResult<T> {
    contract {
        callsInPlace(result1, InvocationKind.EXACTLY_ONCE)
        callsInPlace(result2, InvocationKind.AT_MOST_ONCE)
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return result1().andThen { value1 ->
        result2().mapSuccess { value2 ->
            transform(value1, value2)
        }
    }
}

/**
 * Combines three [AsyncResult]s ([result1], [result2], and [result3]) into a single one,
 * by applying the [transform] to their values.
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA")
inline fun <R1, R2, R3, reified T> zip(
    crossinline result1: () -> AsyncResult<R1>,
    crossinline result2: () -> AsyncResult<R2>,
    crossinline result3: () -> AsyncResult<R3>,
    crossinline transform: (R1, R2, R3) -> T,
): AsyncResult<T> {
    contract {
        callsInPlace(result1, InvocationKind.EXACTLY_ONCE)
        callsInPlace(result2, InvocationKind.AT_MOST_ONCE)
        callsInPlace(result3, InvocationKind.AT_MOST_ONCE)
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return result1().andThen { value1 ->
        result2().andThen { value2 ->
            result3().mapSuccess { value3 ->
                transform(value1, value2, value3)
            }
        }
    }
}

/**
 * Combines four [AsyncResult]s ([result1], [result2], [result3], and [result4]) into a single one,
 * by applying the [transform] to their values.
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA")
inline fun <R1, R2, R3, R4, reified T> zip(
    crossinline result1: () -> AsyncResult<R1>,
    crossinline result2: () -> AsyncResult<R2>,
    crossinline result3: () -> AsyncResult<R3>,
    crossinline result4: () -> AsyncResult<R4>,
    crossinline transform: (R1, R2, R3, R4) -> T,
): AsyncResult<T> {
    contract {
        callsInPlace(result1, InvocationKind.EXACTLY_ONCE)
        callsInPlace(result2, InvocationKind.AT_MOST_ONCE)
        callsInPlace(result3, InvocationKind.AT_MOST_ONCE)
        callsInPlace(result4, InvocationKind.AT_MOST_ONCE)
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return result1().andThen { value1 ->
        result2().andThen { value2 ->
            result3().andThen { value3 ->
                result4().mapSuccess { value4 ->
                    transform(value1, value2, value3, value4)
                }
            }
        }
    }
}

/**
 * Combines two [AsyncResult]s (the receiver lambda result and the [producer] lambda) into a single one,
 * by applying the [transform] to their values.
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA")
inline fun <R1, R2, reified T> (() -> AsyncResult<R1>).zipWith(
    crossinline producer: () -> AsyncResult<R2>,
    crossinline transform: (R1, R2) -> T,
): AsyncResult<T> {
    contract {
        callsInPlace(producer, InvocationKind.AT_MOST_ONCE)
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return invoke().andThen { value1 ->
        producer().mapSuccess { value2 ->
            transform(value1, value2)
        }
    }
}

/**
 * Combines three [AsyncResult]s (the receiver lambda result, [producer1], and [producer2]) into a single one,
 * by applying the [transform] to their values.
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA")
inline fun <R1, R2, R3, reified T> (() -> AsyncResult<R1>).zipWith(
    crossinline producer1: () -> AsyncResult<R2>,
    crossinline producer2: () -> AsyncResult<R3>,
    crossinline transform: (R1, R2, R3) -> T,
): AsyncResult<T> {
    contract {
        callsInPlace(producer1, InvocationKind.AT_MOST_ONCE)
        callsInPlace(producer2, InvocationKind.AT_MOST_ONCE)
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return invoke().andThen { value1 ->
        producer1().andThen { value2 ->
            producer2().mapSuccess { value3 ->
                transform(value1, value2, value3)
            }
        }
    }
}

/**
 * Combines four [AsyncResult]s (the receiver lambda result, [producer1], [producer2], and [producer3]) into a single one,
 * by applying the [transform] to their values.
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA")
inline fun <R1, R2, R3, R4, reified T> (() -> AsyncResult<R1>).zipWith(
    crossinline producer1: () -> AsyncResult<R2>,
    crossinline producer2: () -> AsyncResult<R3>,
    crossinline producer3: () -> AsyncResult<R4>,
    crossinline transform: (R1, R2, R3, R4) -> T,
): AsyncResult<T> {
    contract {
        callsInPlace(producer1, InvocationKind.AT_MOST_ONCE)
        callsInPlace(producer2, InvocationKind.AT_MOST_ONCE)
        callsInPlace(producer3, InvocationKind.AT_MOST_ONCE)
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return invoke().andThen { value1 ->
        producer1().andThen { value2 ->
            producer2().andThen { value3 ->
                producer3().mapSuccess { value4 ->
                    transform(value1, value2, value3, value4)
                }
            }
        }
    }
}

/**
 * Combines two [AsyncResult]s (the receiver and [result]) into a single one,
 * by applying the [transform] to their values.
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA")
inline fun <R1, R2, reified T> AsyncResult<R1>.zipWith(
    result: AsyncResult<R2>,
    crossinline transform: (R1, R2) -> T,
): AsyncResult<T> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return andThen { value1 ->
        result.mapSuccess { value2 ->
            transform(value1, value2)
        }
    }
}

/**
 * Combines three [AsyncResult]s (the receiver, [result1], and [result2]) into a single one,
 * by applying the [transform] to their values.
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA")
inline fun <R1, R2, R3, reified T> AsyncResult<R1>.zipWith(
    result1: AsyncResult<R2>,
    result2: AsyncResult<R3>,
    crossinline transform: (R1, R2, R3) -> T,
): AsyncResult<T> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return andThen { value1 ->
        result1.andThen { value2 ->
            result2.mapSuccess { value3 ->
                transform(value1, value2, value3)
            }
        }
    }
}

/**
 * Combines four [AsyncResult]s (the receiver, [result1], [result2], and [result3]) into a single one,
 * by applying the [transform] to their values.
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA")
inline fun <R1, R2, R3, R4, reified T> AsyncResult<R1>.zipWith(
    result1: AsyncResult<R2>,
    result2: AsyncResult<R3>,
    result3: AsyncResult<R4>,
    crossinline transform: (R1, R2, R3, R4) -> T,
): AsyncResult<T> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return andThen { value1 ->
        result1.andThen { value2 ->
            result2.andThen { value3 ->
                result3.mapSuccess { value4 ->
                    transform(value1, value2, value3, value4)
                }
            }
        }
    }
}
