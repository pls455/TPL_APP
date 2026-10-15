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

1. The Google Services Gradle plugin, Firebase BoM, Auth SDK, and Firestore SDK are now installed in the Android build.
2. The app reads active branches from `branches` and active subjects from `subjects` in Firestore using live snapshot listeners.
3. The admin web panel manages branches and subjects, including their active state, display order, and branch associations. Writes are intended for the super admin and content managers only.
4. Deploy and test Firestore rules before relying on any cloud data; files in the repository are not automatically deployed to Firebase Console.
5. Implement the Cloudflare Worker path for private Dbrij uploads/downloads. The app must never receive S3 access keys.
6. Student authentication/profile sync, resource metadata, tests, and exam submissions still need implementation and security testing.

## Current status

The Android branch and subject catalogs now load from Firestore; there is no embedded sample curriculum fallback. A branch will appear only after it exists in Firestore with `active: true`. Firebase Auth SDK is installed but student sign-in is not implemented yet. PDF storage delivery, resource metadata, and exam submissions are still not connected.
