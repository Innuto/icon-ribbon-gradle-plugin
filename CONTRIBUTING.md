# Contributing

Thanks for helping out! Bug reports, ideas and pull requests are all welcome.

## Reporting bugs and ideas

- **Bugs:** open an issue with the bug report template. Your `iconRibbon { }` configuration and
  AGP / Gradle versions usually make the difference between a quick fix and a guessing game.
- **Bigger changes:** please open an issue first, so we can agree on the approach before you
  spend time on a pull request.
- **Security issues:** don't open a public issue, see [SECURITY.md](SECURITY.md).

Everyone taking part is expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Development

The repository holds two builds:

- `plugin/`: the Gradle plugin itself (an included build).
- `sample/`: an Android app using the plugin through `includeBuild`, for end-to-end checks.

Requirements: JDK 17 and the Android SDK (for the sample).

```bash
./gradlew -p plugin test validatePlugins     # unit tests + plugin validation
./gradlew -p plugin functionalTest           # real Android builds through Gradle TestKit
./gradlew :sample:assembleDebug              # end-to-end; output in sample/build/generated/res
```

CI runs both on every push and pull request.

## Pull requests

- Keep a pull request to one change, with tests where it makes sense: unit tests in
  `plugin/src/test`, plugin behavior (DSL, variants, build failures) in `plugin/src/functionalTest`.
- Add a line under `## [Unreleased]` in [CHANGELOG.md](CHANGELOG.md) for anything users will
  notice (new options, behavior changes, fixes).
- Keep the plugin [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
  compatible: no `Project` access at execution time. The sample build uses the configuration cache,
  so `./gradlew :sample:assembleDebug` catches most problems.
- Only use the public AGP API (`com.android.tools.build:gradle-api`), no AGP internals.
- The public Kotlin API is compiled with `apiVersion = 1.8` so that older Gradle versions can read
  it; avoid newer Kotlin language features in public signatures.

Releases are done by the maintainer, see "Releasing" in the [README](README.md#releasing).
