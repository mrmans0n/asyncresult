# Arrow Either extensions

The `asyncresult-either` artifact provides helpers to bridge Arrow's `Either` type with `AsyncResult`. This is useful when working with codebases that use Arrow for error handling.

## Converting Either to AsyncResult

Transform an `Either<L, R>` directly into an `AsyncResult<R>`:

```kotlin
val either: Either<Failure, User> = fetchUser()
val result: AsyncResult<User> = either.toAsyncResult()
```

- `Either.Right` becomes `Success`
- `Either.Left` becomes `Error` with the left value stored in metadata

## Binding nested Either values

When you have an `AsyncResult` containing an `Either`, use `bind()` to flatten it:

```kotlin
val result: AsyncResult<Either<Failure, User>> = Success(userEither)
val unwrapped: AsyncResult<User> = result.bind()
```

The `bind()` function handles the conversion:

- `Success(Right(value))` becomes `Success(value)`
- `Success(Left(error))` becomes `Error` with the left value in metadata
- `Loading`, `NotStarted`, and `Error` pass through unchanged

### Special handling for Throwable

When the left type is `Throwable`, it's stored in `Error.throwable` instead of metadata:

```kotlin
val result: AsyncResult<Either<Throwable, User>> = Success(Left(IOException()))
val bound: AsyncResult<User> = result.bind() // Error with throwable set
```

## Converting Flow to Either

Transform a `Flow<AsyncResult<R>>` into an `Either`:

```kotlin
val flow: Flow<AsyncResult<User>> = userFlow
val either: Either<Error, User> = flow.toEither()
```

This waits for the first terminal emission (`Success` or `Error`) and converts it:

- `Success` becomes `Either.Right`
- `Error` becomes `Either.Left`

### Custom error transformation

If you need a specific error type, provide a transform function:

```kotlin
val either: Either<NetworkError, User> = flow.toEither { error ->
    error.metadataOrNull<NetworkError>() ?: NetworkError.Unknown
}
```

By default, it attempts to extract the error type from the `Error.metadata`.
