# AsyncResult - Results for async operations in Kotlin Multiplatform

[![Build & test](https://github.com/mrmans0n/asyncresult/actions/workflows/build.yaml/badge.svg?branch=main)](https://github.com/mrmans0n/asyncresult/actions/workflows/build.yaml?query=branch%3Amain)
[![AsyncResult](https://img.shields.io/maven-central/v/io.nlopez.asyncresult/asyncresult)](https://central.sonatype.com/search?q=g%3Aio.nlopez.asyncresult)

AsyncResult is a small Kotlin Multiplatform library to model asynchronous operations using a sealed hierarchy.
It captures the common states you deal with in UI and data layers:

- `NotStarted` - The operation hasn't begun yet
- `Loading` - The operation is in progress
- `Success` - The operation completed successfully with a value
- `Error` - The operation failed, optionally with a throwable, metadata, and error ID

The library provides a rich set of operators for transforming, combining, and extracting values from these states, making it easy to handle async operations in a type-safe way.

## Modules

### asyncresult (Core)

The core module contains the type hierarchy and all essential utilities:

- **Transformations** - `mapSuccess`, `mapError`, `flatMap`, `flatten`, `bimap`, `fold`, `orError`, `filterOrError`, `castOrError`
- **State checks** - `isSuccess`, `isError`, `isIncomplete`, `isSuccessAnd`, `isErrorAnd`, `isErrorWithMetadataAnd`, `contains`
- **Value extraction** - `getOrNull`, `getOrDefault`, `getOrElse`, `getOrThrow`, `getOrEmpty`, `errorOrNull`, `errorWithMetadataOrNull`, `throwableOrNull`, `errorIdOrNull`
- **Side effects** - `onSuccess`, `onLoading`, `onError`, `onErrorWithMetadata`, `onNotStarted`, `onIncomplete`
- **Unwrapping** - `unwrap`, `unwrapError`, `unwrapThrowable`, `unwrapMetadata`, `unwrapErrorId`, `expect`, `expectError`, `expectThrowable`, `expectMetadata`, `expectErrorId` (Rust-style extraction)
- **Combining** - `zip`, `zipWith`, `and`, `andThen`, `spread`, `combine`, `sequence`
- **Recovery** - `recover`, `recoverIf`, `or`, `orElse`
- **Validation** - `toErrorIf`, `toErrorUnless`
- **Monad DSL** - `result { }` comprehension with `bind()`, `error()`, `loading()`, `ensure()`, `ensureNotNull()`
- **Flow helpers** - `asAsyncResult`, `onLoading`, `onSuccess`, `onError`, `onIncomplete`, `skipWhileLoading`, `filterNotLoading`, `cacheLatestSuccess`, `timeoutToError`, `retryOnError`, `retryOnErrorWithMetadata`
- **Collection utilities** - `errors`, `successes`, `throwables`, `incompletes`, `metadata`, `anyLoading`, `anyIncomplete`, `anyError`, `errorsFrom`, `partition`

### asyncresult-either

Extensions for interoperability with Arrow's `Either` type:

- **Conversion** - `toAsyncResult()` to convert `Either` to `AsyncResult`
- **Binding** - `bind()` to flatten `AsyncResult<Either<L, R>>` to `AsyncResult<R>`
- **Flow conversion** - `asAsyncResult()` to convert `Flow<Either<L, R>>` to `Flow<AsyncResult<R>>`, `toEither()` to convert `Flow<AsyncResult<T>>` to `Either`

### asyncresult-test

Testing utilities built on [assertk](https://github.com/willowtreeapps/assertk):

- **State assertions** - `isNotStarted()`, `isLoading()`, `isIncomplete()`, `isSuccess()`, `isError()`
- **Value assertions** - `isSuccessEqualTo()`, `isErrorWithMetadata()`, `isErrorWithMetadataEqualTo()`
- **Flow assertions** - `assertSuccess()`, `assertError()` for testing flow emissions

## Usage

```kotlin
dependencies {
    implementation("io.nlopez.asyncresult:asyncresult:<version>")

    // Optional: Arrow Either interop
    implementation("io.nlopez.asyncresult:asyncresult-either:<version>")

    // Optional: Testing helpers
    testImplementation("io.nlopez.asyncresult:asyncresult-test:<version>")
}
```

## Documentation

📚 [mrmans0n.github.io/asyncresult](https://mrmans0n.github.io/asyncresult)

## License

```
MIT License

Copyright (c) 2026 Nacho Lopez

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
