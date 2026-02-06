# AsyncResult

AsyncResult is a small Kotlin Multiplatform library to model asynchronous operations using a sealed hierarchy.
It captures the common states you deal with in UI and data layers:

- `NotStarted` - The operation hasn't begun yet
- `Loading` - The operation is in progress
- `Success` - The operation completed successfully with a value
- `Error` - The operation failed, optionally with a throwable and metadata

## Quick Start

Convert any `Flow` to an `AsyncResult` flow and handle all states:

```kotlin
userRepository.observeUser()
    .asAsyncResult()
    .collect { result ->
        when (result) {
            is NotStarted -> { /* Initial state */ }
            is Loading -> showLoading()
            is Success -> showUser(result.value)
            is Error -> showError(result.throwable)
        }
    }
```

The library provides a rich set of operators for transforming, combining, and extracting values from these states, making it easy to handle async operations in a type-safe way.

## Modules

### asyncresult (Core)

The core module contains the type hierarchy and all essential utilities:

- **Transformations** - `mapSuccess`, `mapError`, `flatMap`, `fold`, `orError`, `filterOrError`, `castOrError`
- **Value extraction** - `getOrNull`, `getOrDefault`, `getOrElse`, `getOrThrow`, `getOrEmpty`
- **Side effects** - `onSuccess`, `onLoading`, `onError`, `onNotStarted`
- **Unwrapping** - `unwrap`, `unwrapError`, `expect`, `expectError` (Rust-style extraction)
- **Combining** - `zip`, `zipWith`, `and`, `andThen`, `spread`
- **Flow helpers** - `asAsyncResult`, `onLoading`, `onSuccess`, `onError`, `skipWhileLoading`, `cacheLatestSuccess`, `timeoutToError`, `retryOnError`
- **Collection utilities** - `getAllErrors`, `anyLoading`, `anyIncomplete`

[View full documentation](core.md)

### asyncresult-either

Extensions for interoperability with Arrow's `Either` type:

- **Conversion** - `toAsyncResult()` to convert `Either` to `AsyncResult`
- **Binding** - `bind()` to flatten `AsyncResult<Either<L, R>>` to `AsyncResult<R>`
- **Flow conversion** - `asAsyncResult()` to convert `Flow<Either<L, R>>` to `Flow<AsyncResult<R>>`, `toEither()` to convert `Flow<AsyncResult<T>>` to `Either`

[View full documentation](either.md)

### asyncresult-test

Testing utilities built on [assertk](https://github.com/willowtreeapps/assertk):

- **State assertions** - `isNotStarted()`, `isLoading()`, `isIncomplete()`, `isSuccess()`, `isError()`
- **Value assertions** - `isSuccessEqualTo()`, `isErrorWithMetadata()`, `isErrorWithMetadataEqualTo()`
- **Flow assertions** - `assertSuccess()`, `assertError()` for testing flow emissions

[View full documentation](testing.md)

## Installation

```kotlin
dependencies {
    implementation("io.nlopez.asyncresult:asyncresult:<version>")
    
    // Optional: Arrow Either interop
    implementation("io.nlopez.asyncresult:asyncresult-either:<version>")
    
    // Optional: Testing helpers
    testImplementation("io.nlopez.asyncresult:asyncresult-test:<version>")
}
```
