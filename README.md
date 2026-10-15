# TPL Android

**Tawjihi Palestine Library** is an Arabic-first Android app for Palestinian Tawjihi students.

## Stack
- Kotlin + Jetpack Compose
- Firebase Firestore live listeners for the student catalog
- Min SDK 24, Java 17
- Forest `#173F35`, ivory `#F5F0E6`, muted gold `#C5A15B`

## Current features
- Live active branches from Firestore `branches`
- Live active subjects from Firestore `subjects`, filtered by branch document IDs
- Live published external resources from Firestore `resources` inside each subject
- Local branch selection preference, updated automatically if the selected branch is deactivated
- Contextual branch picker card on the home screen and subject screen, instead of an isolated selector in the top bar
- Animated branch selection and subtle transitions between app sections, respecting reduced-motion preferences where the system controls animation
- Clear three-step student onboarding instead of static categories that all opened the same screen
- Search within the cloud-loaded subject list
- Local Pomodoro timer and navigation scaffold
- GitHub Actions debug APK build

## Admin-controlled cloud data
The app does not hardcode the branch or subject list. The admin panel manages branches, subjects, and external resource links. Only active branches/subjects and resources with `status = "published"` are shown to students. Changes arrive through Firestore snapshot listeners.

Expected fields:
- `branches/{autoId}`: `name` (string), `active` (boolean), `order` (number)
- `subjects/{autoId}`: `name` (string), `branchIds` (array of branch document IDs), `active` (boolean), `order` (number)
- `resources/{autoId}`: `title`, `description`, `type`, `subjectId`, `branchIds`, `status`, and `externalUrl` or `sourceUrl`

## Not connected yet
Student authentication/profile sync, exam/question screens, and private Dbrij upload/download are not wired yet. Resource links are external URLs; they are not uploaded to private storage by the app.

## Firebase setup
- Android package: `com.karamdev.tpl`
- The Firebase Android config belongs at `app/google-services.json`.
- Deploy the reviewed `firestore.rules` from the web repository through Firebase Console. Committing rules to GitHub does not publish them to Firebase.
- If branches or subjects do not load, check internet access, Firebase project/package match, and deployed Firestore rules.

## Build
Run `gradle assembleDebug` with JDK 17 and Android SDK installed, or use the Android build workflow in GitHub Actions.
