# TPL Android

**Tawjihi Palestine Library** is an Arabic-first Android app for Palestinian Tawjihi students.

## Stack
- Kotlin + Jetpack Compose
- Min SDK 24, Java 17
- Forest `#173F35`, ivory `#F5F0E6`, muted gold `#C5A15B`

## Current foundation
- Home and subjects screens with scientific/literary/industrial branch selection
- Navigation scaffold and starter study tools/test sections
- GitHub Actions debug APK build

## Not connected yet
Firebase Authentication, Firestore-backed resources, tests, and the private Dbrij file flow are not wired yet. The visible sample subjects are placeholders, not live curriculum data. No credentials or service secrets belong in this repository.

## Build
Run `gradle assembleDebug` with JDK 17 and Android SDK installed, or use the Android build workflow in GitHub Actions.

Package: `com.karamdev.tpl`
