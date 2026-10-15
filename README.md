# TPL Android

**Tawjihi Palestine Library** is an Arabic-first Android app for Palestinian Tawjihi students.

## Stack
- Kotlin + Jetpack Compose
- Firebase Firestore for live branch and subject catalog
- Min SDK 24, Java 17
- Forest `#173F35`, ivory `#F5F0E6`, muted gold `#C5A15B`

## Current features
- Live active branches from Firestore `branches` collection
- Live active subjects from Firestore `subjects` collection, filtered by branch IDs
- Branch preference saved locally; catalog updates appear through Firestore listeners
- Navigation scaffold and local Pomodoro timer
- GitHub Actions debug APK build

## Not connected yet
Student authentication/profile sync, resource files and resource metadata in the app, exams/questions, and private Dbrij upload/download are not wired yet. An empty branch or subject list means the admin panel has not created active catalog data or Firestore access/rules need checking.

## Firestore catalog schema
- `branches/{autoId}`: `name` (string), `active` (boolean), `order` (number), `createdBy` (UID), `createdAt`, `updatedAt`
- `subjects/{autoId}`: `name` (string), `branchIds` (array of branch document IDs), `active` (boolean), `order` (number), `createdBy` (UID), `createdAt`, `updatedAt`

Only active records are shown to students. The admin panel restricts branch/subject writes to the super admin and content managers; Firestore rules must also be deployed in Firebase Console.

## Build
Run `gradle assembleDebug` with JDK 17 and Android SDK installed, or use the Android build workflow in GitHub Actions.

Package: `com.karamdev.tpl`
