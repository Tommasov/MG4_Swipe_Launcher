# Changelog

All notable changes to **MG4 Swipe Launcher** are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project roughly follows semantic versioning.

## [1.4.1] - 2026-06-22

### Changed
- The "Opening…" loader overlay is now **enabled by default** for new installs.

## [1.4] - 2026-06-22

### ⚠️ Breaking — existing users must reinstall once
- The app is now signed with a **new, stable release key**. Android does not allow
  installing an update over an app signed with a different key, so anyone on
  **v1.2 / v1.3 must uninstall the old app once** before installing v1.4. This is a
  one-time migration — every future update installs normally, without uninstalling.
  App settings are reset on reinstall.

### Added
- Stable release signing configuration: credentials read from a git-ignored
  `keystore.properties`, so every build is signed with the same key and users can
  always update in place going forward.

### Changed
- Default swipe target is now **MG4 Simple Launcher**
  (`com.tommasov.mg4simplelauncher`) instead of Nova Launcher. As the home launcher
  it stays warm in memory, so the swipe re-opens it almost instantly.
- App display name simplified to "MG4 Swipe Launcher"; repository renamed to
  `MG4_Swipe_Launcher` (the old URL redirects automatically).
- Added missing Italian translations (loader/option strings).

### Fixed
- The "Opening…" loader is now genuinely **event-driven**: it disappears the moment
  the target app's window is actually on screen, instead of after a fixed timer. The
  root cause was the accessibility-service config filtering out *all* window events
  (empty `packageNames`); the timeout now only acts as a safety cap.

## [1.3] - 2026-06-22

### Added
- **Swap swipe areas (left ↔ right)** option.
- "Opening…" loader overlay (initial fixed-timer version).

### Changed
- New app icon; README updates.

### Fixed
- The swap help labels now swap both their text **and** their background colour to
  match the active layout.

## [1.2] - 2024-11-03

### Added
- On-screen help labels for the two swipe areas.
- Option to hide the floating back button (useful during car servicing).

### Changed
- UI, strings and layout refinements; service-stop intent handling; back button
  visibility preference.

## [1.1] - 2024-11-02

### Added
- Custom target-app selection.
- Floating back button via the Accessibility Service (simulated physical Back).
- Italian translation; dedicated night back button.

### Fixed
- Permissions flow, floating-button drag handling, preferences manager.

## [1.0] - 2024-10-29

### Added
- Initial release: swipe up from the bottom edge of the screen to launch a chosen app.
