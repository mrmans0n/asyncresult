# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) and other AI assistants working on this repository.

## Project Overview

**AsyncResult** is a Kotlin Multiplatform library for modeling asynchronous operations using a sealed hierarchy (NotStarted, Loading, Success, Error). It provides operators for transforming, combining, and extracting values from these states.

## Build & Test Commands

```bash
./gradlew build          # Build everything
./gradlew test           # Run all tests
./gradlew ktfmtFormat    # Format code (ktfmt, Kotlin lang style)
./gradlew ktfmtCheck     # Check formatting (used by CI)
```

## Project Structure

```
asyncresult/             # Core module (type hierarchy + operators)
asyncresult-either/      # Either integration
asyncresult-test/        # Test helpers
docs/                    # mkdocs documentation
```

## Code Quality

- All new functionality must have corresponding tests
- Run `./gradlew ktfmtFormat` before committing
- CI runs ktfmtCheck, build, and test

## Commit Style

Use Conventional Commits. Keep code, comments, and commit messages in English.
