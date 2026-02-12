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

// Check throwable type
assertThat(result).isErrorWithThrowable()  // asserts Error has a non-null throwable
assertThat(result).isErrorWithThrowableOfType<IOException>()  // asserts throwable is specific type
assertThat(result).isErrorWithThrowableMessage("Network error")  // asserts throwable message

// Check metadata
assertThat(result).isErrorWithMetadata<T, NetworkError>()
assertThat(result).isErrorWithMetadataEqualTo(NetworkError.Timeout)

// Chain assertions on metadata
assertThat(result).isError().isMetadataEqualTo(expectedMetadata)

// Check error ID
assertThat(result).isErrorWithId()  // asserts Error has a non-null errorId
assertThat(result).isErrorWithIdEqualTo(ErrorId("req-123"))
assertThat(result).hasErrorId(ErrorId("req-123"))

// Chain assertions on error ID
assertThat(result).isError().isErrorIdEqualTo(ErrorId("req-123"))
```

## Flow assertions

Test terminal emissions from a `Flow<AsyncResult<T>>`:

```kotlin
// Assert the first terminal emission is Success with value
flow.assertSuccess(42)

// Assert the first terminal emission is Error and get it for further checks
val error: Error = flow.assertError()
assertThat(error.throwable).isInstanceOf(IOException::class)

// Assert error with typed metadata
val metadata: NetworkError = flow.assertErrorWithMetadata<NetworkError>()

// Assert error with throwable type
flow.assertErrorWithThrowableOfType<IOException>()

// Assert error with error ID
flow.assertErrorWithId(ErrorId("req-123"))
```

### Initial state assertions

Assert the very first emission (before terminal state):

```kotlin
flow.assertFirstIsLoading()
flow.assertFirstIsNotStarted()
flow.assertFirstIsIncomplete()  // Loading or NotStarted
```

## Collection assertions

Assert properties of a collection of results:

```kotlin
val results: List<AsyncResult<Int>> = listOf(Loading, Success(1), Error.Empty)

assertThat(results).hasAnyLoading()
assertThat(results).hasAnyIncomplete()

// Extract and assert on all errors
assertThat(results).allErrors().hasSize(1)

// Extract and assert on all error metadata of a specific type
assertThat(results).allErrorMetadata<NetworkError>().isEmpty()
```
