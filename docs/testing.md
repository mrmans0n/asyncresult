# Testing

The `asyncresult-test` artifact exposes assertk helpers for testing `AsyncResult`.

```kotlin
import assertk.assertThat
import io.nlopez.asyncresult.test.isSuccessEqualTo

val result: AsyncResult<Int> = Success(42)
assertThat(result).isSuccessEqualTo(42)
```

Flow helpers are also available:

```kotlin
flow.assertSuccess(42)
```
