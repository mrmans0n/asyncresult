# Arrow Either extensions

The `asyncresult-either` artifact provides helpers to bridge Arrow's `Either` with `AsyncResult`.

## Bind

```kotlin
val result: AsyncResult<Either<Failure, User>> = Success(userEither)
val unwrapped: AsyncResult<User> = result.bind()
```

## Conversion

```kotlin
val either: Either<Failure, User> = eitherValue
val result: AsyncResult<User> = either.toAsyncResult()
```

## Flow to Either

```kotlin
val either: Either<Error, User> = flow.toEither()
```
