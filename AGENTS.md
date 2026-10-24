# Gains Goblin

Gains Goblin is an Android app in Kotlin with Jetpack Compose and a single Activity; the code lives in the `app` module under the `com.readyplayerai.gainsgoblin` package.

## Build and run

```bash
./gradlew :app:assembleDebug
./gradlew test                  # JVM unit tests (app/src/test)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Assemble a debug APK, install it on a device/emulator, launch it with `adb shell monkey -p com.readyplayerai.gainsgoblin -c android.intent.category.LAUNCHER 1`, and screenshot with `adb exec-out screencap -p > /tmp/screen.png`. At the time of writing, visual checks ran on `emulator-5554` (448x997dp, density 480 = 3x); substitute whatever device serial `adb devices` reports for your setup.

## UI layout: who owns window insets

- The app shell (`GainsGoblinApp`) owns window insets: its `Scaffold` sets `contentWindowInsets = WindowInsets(0, 0, 0, 0)` and hosts the persistent `GainsGoblinBottomBar`.
- The `NavHost` applies `padding(innerPadding)` **and** `consumeWindowInsets(innerPadding)`, so nested screen `Scaffold`s see 0 remaining bottom inset — insets are paid for exactly once.
- A screen `Scaffold` must **not** add `contentWindowInsets` overrides, `windowInsetsPadding`, or `statusBarsPadding` for chrome the shell already handles (`FlexibleHeaderBar` does its own `statusBarsPadding()`).
- Screen content uses `innerPadding.calculateTopPadding()` for the top of a list and its own `contentPadding` for the bottom — never `calculateBottomPadding()`.

## UI layout: component positioning

- `Scaffold` positions `floatingActionButton`, `snackbar` and `bottomBar` from those insets, so they are what breaks when insets are double-counted (a FAB must sit 16dp, i.e. 48px at 3x, above the bottom bar).
- Fixed-size text in a small tile/box must set an explicit `lineHeight` (both `Text`s otherwise inherit `bodyLarge`'s 24sp) and may need a small top spacer for optical centering when the glyphs have no descender — see `DateTile` in `JournalDayHeader.kt`.

## Visual verification

After any layout or inset change, rebuild, install, screenshot the affected screen plus the other bottom-bar tabs, and measure the affected gap in pixels (divide by 3 for dp at the test device's density) rather than eyeballing it.

## More detail

See `notes/compose-insets.md` for the incident write-up; `notes/` is gitignored, so keep durable rules in this file.
