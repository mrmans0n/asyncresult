# AsyncResult - Kotlin Multiplatform results for async operations

[![Build & test](https://github.com/mrmans0n/asyncresult/actions/workflows/build.yaml/badge.svg?branch=main)](https://github.com/mrmans0n/asyncresult/actions/workflows/build.yaml?query=branch%3Amain)
[![AsyncResult](https://img.shields.io/maven-central/v/io.nlopez.asyncresult/asyncresult)](https://central.sonatype.com/search?q=g%3Aio.nlopez.asyncresult)
[![AsyncResult Either](https://img.shields.io/maven-central/v/io.nlopez.asyncresult/asyncresult-either)](https://central.sonatype.com/search?q=g%3Aio.nlopez.asyncresult)
[![AsyncResult Test](https://img.shields.io/maven-central/v/io.nlopez.asyncresult/asyncresult-test)](https://central.sonatype.com/search?q=g%3Aio.nlopez.asyncresult)

AsyncResult models the state of asynchronous operations in a simple, explicit way: NotStarted, Loading, Success, Error.

## Artifacts

- `io.nlopez.asyncresult:asyncresult` - core AsyncResult types and utilities.
- `io.nlopez.asyncresult:asyncresult-either` - Arrow Either extensions.
- `io.nlopez.asyncresult:asyncresult-test` - assertk helpers for testing.

## Usage

```kotlin
val result: AsyncResult<User> = Success(user)
val message = result.fold(
    ifNotStarted = { "Waiting" },
    ifLoading = { "Loading..." },
    ifSuccess = { "Hello ${it.name}" },
    ifError = { "Error: ${it.throwable?.message}" },
)
```

For more details, check the documentation site.

## Contributing

Contributions are welcome. Please open an issue or PR.

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
