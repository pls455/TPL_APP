# Firebase setup for TPL Android

Package name is fixed as `com.karamdev.tpl`.

## One-time Firebase Console step

The current Firebase project is `tpl-38d2e`. The registered web app is for the admin panel; it is not a substitute for registering the Android app.

1. Open https://console.firebase.google.com/ and select project `tpl-38d2e`.
2. Open Project settings, then General.
3. In Your apps, choose Add app and select Android.
4. Enter package name exactly: `com.karamdev.tpl`. The nickname can be `TPL Android`.
5. Complete registration and download `google-services.json`.
6. Place the file at `app/google-services.json` in this repository.
7. Enable Email/Password and Google providers only if they are needed for the current release. Google sign-in also requires registering the app's SHA-1/SHA-256 fingerprints.
8. Confirm the Android app appears in Firebase Project settings before attempting authentication.

The Firebase Android config file contains client configuration, not an Admin SDK private key. Never place service-account JSON, private keys, Dbrij S3 secrets, or Cloudflare API tokens in the repository.

## Integration order

1. Add the Google Services Gradle plugin and Firebase BoM.
2. Wire Firebase Authentication and a student profile repository.
3. Add Firestore repositories for branches, subjects, and published resource metadata.
4. Deploy and test Firestore rules before enabling writes.
5. Implement the Cloudflare Worker path for private Dbrij uploads/downloads. The app must never receive S3 access keys.
6. Test authenticated access, unauthorized access, expired downloads, and role boundaries before release.

## Current status

The current Android app has a working local branch preference, searchable starter subject list, and local Pomodoro timer. Firebase sign-in, live Firestore content, PDF storage delivery, and exam submissions are not connected yet. Starter subjects are demonstration data, not an authoritative curriculum.
