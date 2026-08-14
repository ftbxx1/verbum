# Contributing to Verbum

Thanks for wanting to help! Verbum is a small language project, so every
contribution counts.

## Before you start

- Read the [language spec](verbum-engine/docs/SPEC.md) so changes fit the
  existing grammar.
- Check open issues for the same idea before opening a new one.

## Getting the project to build

```
mvn package          # builds engine + paper plugin, runs all tests
powershell -File run-tests.ps1   # acceptance demo + full test suite
```

The build is fully offline-capable once dependencies are cached.

## What to work on

The project has two parts:

- `verbum-engine/` — the pure-Java language core. New keywords, actions,
  conditions, and parser work live here. No Minecraft needed; tests use a
  mock world.
- `verbum-paper/` — the Paper plugin that bridges the engine to a live
  server (events in, actions out).

## Rules of thumb

- Every new word must be an English sentence, never a symbol or abbreviation.
  No `+`, no quotes, no braces — that is the whole point of the language.
- Every new action/condition needs a test in `verbum-engine/src/test/` and an
  example in `verbum-engine/src/main/resources/scripts/examples/`.
- Keep changes small and readable. Favor a few clear words over clever ones.
- Run the full test suite before opening a PR (`mvn package`).

## Submitting a change

1. Fork the repository.
2. Create a branch: `git checkout -b my-change`.
3. Commit with a clear message describing the language change (not just the
   code change).
4. Open a pull request. Explain what the new sentence reads like and how it
   behaves.

## Code of conduct

Be kind. This is a learning-friendly project — assume good faith, explain
things clearly, and never gatekeep.

## Community

Questions and ideas are welcome on the official Discord server:

**https://discord.gg/qnpHBEmbUC**
