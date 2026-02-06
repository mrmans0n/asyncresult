# Core API

The `asyncresult` artifact contains the core types and utilities for modeling asynchronous operations.

## Types

The library provides a sealed class hierarchy to represent the different states:

- **`NotStarted`** - Idle state before an operation begins. Useful for user-initiated actions that haven't started yet.
- **`Loading`** - The operation is in-flight.
- **`Success<T>`** - The operation completed successfully, containing the result value.
- **`Error`** - The operation failed. Can optionally contain a `Throwable` and typed metadata for context.
- **`Incomplete`** - A marker interface implemented by both `NotStarted` and `Loading`, useful for bundling them in exhaustive `when` statements.

```kotlin
val result: AsyncResult<User> = Loading
```

## Transforming values

### Mapping

Transform the success value while preserving the result state:

```kotlin
val userName: AsyncResult<String> = userResult.mapSuccess { it.name }
```

Transform errors:

```kotlin
val enrichedResult = result.mapError { error -> 
    error.withMetadata(NetworkFailure) 
}
```

### Folding

Generate a value from any state:

```kotlin
val message = result.fold(
    ifNotStarted = { "Waiting" },
    ifLoading = { "Loading..." },
    ifSuccess = { "Hello ${it.name}" },
    ifError = { "Error: ${it.throwable?.message}" },
)
```

### FlatMap and chaining

Chain operations that return `AsyncResult`:

```kotlin
val profile: AsyncResult<Profile> = userResult.flatMap { user ->
    fetchProfile(user.id)
}
```

Use `andThen` for explicit chaining:

```kotlin
val result = fetchUser().andThen { user -> 
    fetchPermissions(user.id) 
}
```

### Filtering and casting

Filter values or convert to errors:

```kotlin
val admin: AsyncResult<User> = userResult.filterOrError { it.isAdmin }
val typed: AsyncResult<Admin> = result.castOrError<Admin>()
```

Handle nullable values:

```kotlin
val nonNull: AsyncResult<User> = nullableResult.orError()
```

## Getting values

Extract the underlying value in different ways:

```kotlin
val value: User? = result.getOrNull()
val value: User = result.getOrDefault(defaultUser)
val value: User = result.getOrElse { fallbackUser }
val value: User = result.getOrThrow() // throws if not Success

// For collections
val items: List<Item> = listResult.getOrEmpty()
```

Extract error information:

```kotlin
val error: Error? = result.errorOrNull()
val throwable: Throwable? = result.throwableOrNull()
val metadata: MyError? = result.errorWithMetadataOrNull<MyError>()
```

## Side effects

Run code based on the current state:

```kotlin
result
    .onNotStarted { showPlaceholder() }
    .onLoading { showSpinner() }
    .onSuccess { user -> render(user) }
    .onError { showError(it) }
    .onErrorWithMetadata<R, NetworkError> { throwable, metadata -> 
        showNetworkError(metadata) 
    }
```

## Unwrapping (Rust-style)

For when you're certain about the state and want to extract values directly:

```kotlin
val user: User = result.unwrap() // throws UnwrapException if not Success
val error: Error = result.unwrapError()
val throwable: Throwable = result.unwrapThrowable()
val metadata: MyError = result.unwrapMetadata<MyError>()
```

With custom error messages:

```kotlin
val user = result.expect { "User should be loaded by now" }
val error = result.expectError { "Expected failure" }
```

## Combining results

### Zipping

Combine multiple results into one:

```kotlin
val combined = zip(
    { userResult }, 
    { permissionsResult }
) { user, permissions ->
    UserWithPermissions(user, permissions)
}
```

Or using the `zipWith` extension:

```kotlin
val combined = userResult.zipWith(permissionsResult) { user, permissions ->
    UserWithPermissions(user, permissions)
}
```

Supports up to 4 results.

### Spreading

Split a result containing a `Pair` or `Triple`:

```kotlin
val (userResult, settingsResult) = pairResult.spread()
```

## Working with collections

Utilities for handling multiple results:

```kotlin
val results: List<AsyncResult<*>> = listOf(result1, result2, result3)

val errors: List<Error> = results.getAllErrors()
val throwables: List<Throwable> = results.getAllThrowables()
val isAnyLoading: Boolean = results.anyLoading()
val isAnyIncomplete: Boolean = results.anyIncomplete()
```

Standalone functions:

```kotlin
val hasError = anyError(result1, result2, result3)
val hasLoading = anyLoading(result1, result2, result3)
val errors = errorsFrom(result1, result2, result3)
```

## Flow helpers

Extensions for `Flow<AsyncResult<T>>`:

```kotlin
flowOf(result)
    .onLoading { showSpinner() }
    .onSuccess { render(it) }
    .onError { showError(it) }
```

Extract values from flows:

```kotlin
val value: User = flow.getOrThrow()
val value: User? = flow.getOrNull()
val value: User = flow.getOrElse { fallbackUser }
```

### Filtering

Skip loading states entirely:

```kotlin
flow.skipWhileLoading()
    .collect { result ->
        // Only receives NotStarted, Success, or Error
    }

// Alias
flow.filterNotLoading()
```

### Caching

Cache the latest success value and emit it during reloads:

```kotlin
flow.cacheLatestSuccess()
    .collect { result ->
        // During reload: shows cached Success instead of Loading
    }
```

This is useful for "stale-while-revalidate" patterns where you want to show existing data while fetching updates.

### Timeout

Convert slow operations to errors:

```kotlin
flow.timeoutToError(5.seconds) { 
    TimeoutException("Request timed out") 
}
```

If no `Success` or `Error` is emitted within the timeout, an `Error` with the provided throwable is emitted.

### Retry

Automatically retry on errors:

```kotlin
flow.retryOnError(
    maxRetries = 3,
    delay = 1.seconds,
    predicate = { error -> 
        error.throwable is IOException  // Only retry network errors
    }
)
```

The flow will restart from the beginning on each retry attempt.
