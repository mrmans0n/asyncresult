# Core API

## Modeling async work

```kotlin
val result: AsyncResult<User> = Loading
```

## Mapping and folding

```kotlin
val message = result.fold(
    ifNotStarted = { "Waiting" },
    ifLoading = { "Loading..." },
    ifSuccess = { "Hello ${it.name}" },
    ifError = { "Error: ${it.throwable?.message}" },
)
```

## Zipping

```kotlin
val combined = zip({ userResult }, { permissionsResult }) { user, permissions ->
    UserWithPermissions(user, permissions)
}
```

## Flow helpers

```kotlin
flowOf(result)
    .onLoading { showSpinner() }
    .onSuccess { render(it) }
    .onError { showError(it) }
```
