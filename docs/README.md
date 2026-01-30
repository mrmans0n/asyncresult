# AsyncResult Documentation

This directory contains the source documentation for AsyncResult, which is built
using [MkDocs](https://www.mkdocs.org/) with the [Material theme](https://squidfunk.github.io/mkdocs-material/).

## Documentation Structure

- `index.md` - Overview and introduction
- `core.md` - Core AsyncResult API
- `either.md` - Arrow Either extensions
- `testing.md` - Test helpers and assertions

## Versioning

The documentation is **versioned** using [mike](https://github.com/jimporter/mike):

- **Stable versions** (e.g., `0.1.0`) - Created automatically when a release tag is pushed
- **Development version** (`next`) - Updated automatically on every push to `main` branch
- **Default version** (`latest`) - Always points to the most recent stable release

## Local Development

### Quick Preview (Recommended)

For fast iteration when writing documentation:

```bash
# Install dependencies (first time only)
uv sync

# Serve the docs locally
uv run mkdocs serve
```

Visit `http://localhost:8000` to see your changes live. This serves the current docs without versioning.

### Preview with Versioning

To see how the documentation will look with the version selector:

```bash
# Deploy a test version locally (won't push to GitHub)
uv run mike deploy dev --no-push

# Serve with version selector
uv run mike serve
```

Visit `http://localhost:8000` to see the versioned site.
