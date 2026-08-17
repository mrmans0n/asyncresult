# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) and other AI assistants working on this repository.

## Project Overview

AsyncResult is a Kotlin Multiplatform library for modeling asynchronous operations using a sealed hierarchy. It captures common states: `NotStarted`, `Loading`, `Success`, and `Error`.

## Build Commands

```bash
./gradlew build          # Build all modules
./gradlew test           # Run all tests
./gradlew spotlessApply  # Format code (ktfmt)
./gradlew spotlessCheck  # Check formatting
./gradlew dokkaGenerate  # Generate Dokka documentation
./gradlew dokkaGenerateHtml # Generate Dokka HTML output
```

## Module Structure

```
asyncresult/              # Core module — type hierarchy and operators
asyncresult-either/       # Arrow Either interop extensions
asyncresult-test/         # Testing utilities (assertk-based)
```

## Code Quality

- All code must pass `./gradlew spotlessCheck` before commit
- Formatting uses ktfmt (Google style)
- License headers required (see `spotless/copyright.txt`)
- `explicitApi()` enforced for KMP modules

## Dependencies

- Gradle version catalog: `gradle/libs.versions.toml`
- Kotlin Multiplatform (JVM, Android, iOS, JS targets)
- Arrow (either module)
- assertk (test module)
- Spotless + ktfmt for formatting
- Dokka for API docs

## Publishing

Published to Maven Central via GitHub Actions. See `RELEASING.md` for the release process.

## Commit Style

Use Conventional Commits. Keep changes focused and include tests for new functionality.
