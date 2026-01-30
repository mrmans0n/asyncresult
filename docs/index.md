# AsyncResult

AsyncResult is a small Kotlin Multiplatform library to model asynchronous operations using a sealed hierarchy.
It captures the common states you deal with in UI and data layers:

- `NotStarted`
- `Loading`
- `Success`
- `Error`

It also provides convenience helpers for mapping, folding, flow processing, zipping, and error metadata.

## Modules

- **asyncresult**: Core types and utilities.
- **asyncresult-either**: Arrow `Either` extensions.
- **asyncresult-test**: assertk helpers for testing.
