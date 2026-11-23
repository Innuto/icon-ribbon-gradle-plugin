# Changelog

All notable changes to this plugin are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows
[Semantic Versioning](https://semver.org/).

Add entries under **Unreleased** as you make changes; the release turns that section into a version.

## [Unreleased]

### Added
- Configuration cache compatibility is declared on the Gradle Plugin Portal.

## [0.1.0] - 2026-11-23

### Added
- First release: draws ribbons over Android launcher icons, configured per product flavor and
  build type with `iconRibbon { }` (text, colors, position, `enabled`).
- Ribbons of a variant's flavors and build type stack; positions must differ.
- `iconNames` chooses which icons get a ribbon (default `ic_launcher`, `ic_launcher_round`):
  module-wide in a top-level `iconRibbon { }` block, or per flavor / build type.
- PNG, WebP and JPEG icons are decorated in the bitmap; adaptive icons get a layer-list foreground
  with a ribbon overlay, keeping background and monochrome layers.
- Respects icon overrides in flavor / build type res folders.
- Invalid colors and `iconNames` fail the build during configuration, naming the flavor / build type.
- Drawing runs in a separate headless Gradle worker process; the Gradle daemon is left untouched.
- Configuration cache compatible.

[Unreleased]: https://github.com/innuto/icon-ribbon-gradle-plugin/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/innuto/icon-ribbon-gradle-plugin/releases/tag/v0.1.0
