# Testing

The `asyncresult-test` artifact provides [assertk](https://github.com/willowtreeapps/assertk) extensions for testing `AsyncResult` values in a fluent and readable way.

## State assertions

Assert that a result is in a specific state:

```kotlin
import assertk.assertThat
import io.nlopez.asyncresult.test.*

assertThat(result).isNotStarted()
assertThat(result).isLoading()
assertThat(result).isIncomplete()  // NotStarted or Loading
assertThat(result).isSuccess()
assertThat(result).isError()
```

## Success assertions

Assert success with a specific value:

```kotlin
val result: AsyncResult<Int> = Success(42)
assertThat(result).isSuccessEqualTo(42)
```

Chain assertions on the success value:

```kotlin
assertThat(result).isSuccess().isEqualTo(expectedUser)
assertThat(result).isSuccess().prop(User::name).isEqualTo("Alice")
```

## Error assertions

Assert error state and inspect the error:

```kotlin
assertThat(result).isError()

// Check the throwable
assertThat(result).isError().isThrowableEqualTo(expectedException)

// Check metadata
assertThat(result).isErrorWithMetadata<T, NetworkError>()
assertThat(result).isErrorWithMetadataEqualTo(NetworkError.Timeout)

// Chain assertions on metadata
assertThat(result).isError().isMetadataEqualTo(expectedMetadata)
```

## Flow assertions

Test terminal emissions from a `Flow<AsyncResult<T>>`:

```kotlin
// Assert the first terminal emission is Success with value
flow.assertSuccess(42)

// Assert the first terminal emission is Error and get it for further checks
val error: Error = flow.assertError()
assertThat(error.throwable).isInstanceOf(IOException::class)
```

These suspend functions wait for the first `Success` or `Error` emission, ignoring `Loading` and `NotStarted` states.
