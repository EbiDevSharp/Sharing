# Share redesign and picker validation

The native Android home now provides a server status/power card, six sharing actions, floating navigation, light/dark palettes, an English/Persian switch and bundled offline fonts. App-to-app sharing, streaming and certificate inspection remain accessible.

The picker uses a common grid/list surface for apps, folders, images, videos and audio. Selection belongs to the picker ViewModel and survives search, sorting, navigation, previews and rotation. File rows show size/date; media opens a native preview with selection, playback, a seek bar and bounded ten-second seeks. System document providers and SD cards remain accessible from the toolbar menu.

## Performance changes

- No installed-app icons or full-size bitmap arrays are loaded before the app list appears.
- App metadata is queried without unnecessary package flags. Labels are cached by package source and language; selection is never globally cached.
- App icons use two background workers, a bounded 2 MiB cache and recycled-row checks.
- File metadata uses one stat per visible-folder entry. MediaStore queries and provider metadata run off the UI thread; cursors close reliably.
- Search keys are precomputed. A 150 ms debounce filters the current in-memory snapshot; typing does not rescan storage or PackageManager.
- Filtering and sorting run on a worker. Generation checks reject stale results; RecyclerView computes list differences asynchronously. Selection updates do not reload thumbnails.

## QR fix

The previous QR path forced version 2, used transparent/inverted modules, and expanded each image to 5000×5000. The replacement chooses a QR version for the payload, uses medium error correction and a four-module quiet zone, and renders opaque black/white modules at an integer scale up to 720 px. LAN interface selection prefers Wi-Fi/hotspot/Ethernet and excludes common VPN/mobile interfaces.

## Automated validation

- Android debug build: passed for arm64-v8a, armeabi-v7a and x86_64.
- JVM tests: 15 passed, 0 failed (5 QR round trips/validation, 4 search cases, 4 sorting cases and 2 existing tests).
- Focused Android lint (`./gradlew :app:lintDebug -I scripts/lint-focused.gradle`): passed with no findings for NewApi, StringFormatMatches, ResourceType, WrongConstant, CheckResult, InvalidId and UnknownId.
- Full-project lint initially reports unrelated missing translations and an obsolete manifest activity registration. The initial run also caught API-23 incompatibilities in new code and an existing Russian count-format mismatch; these were corrected. The focused check does not suppress those project-wide existing debts.
- All requested font families are bundled with their OFL notices. Static regular instances provide compatibility with older Android font loaders; bold text uses Android's normal typeface styling.

## Device verification

Runtime checks and the real two-phone camera/network check are tracked separately from the build/tests. Desktop software-emulator timings do not establish phone performance.

## Build

From the repository root, initialize submodules, install the Android SDK/NDK/CMake versions requested by Gradle, install Rust Android targets and Bun, run `bun install` in `web`, then `./gradlew :app:assembleDebug :app:testDebugUnitTest`.

The APK uses a development signing key. Updating an installed copy signed by a different key requires a matching signing key or a separate installation.
