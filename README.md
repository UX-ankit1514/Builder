# Stepwise

A calm task app for people who put things off. Capture a task, pick what matters today,
break big things into small steps, and do one step at a time in Focus Mode.

One Kotlin codebase runs on **Android and iOS**:

| Layer | Tech |
|---|---|
| UI | Compose Multiplatform (shared screens for both platforms) |
| Logic | Kotlin Multiplatform (`shared/src/commonMain`) |
| Auth | Firebase Authentication — **Google sign-in only** (plus a silent guest account, see below) |
| Data | Cloud Firestore, with offline cache |
| Firebase SDK | [GitLive firebase-kotlin-sdk](https://github.com/GitLiveApp/firebase-kotlin-sdk) |

The product sources are the PRD, the IA and the MindMarket style guide (Inter, cream canvas,
coral CTA, green accent). The Figma hi-fi screens were used for layout, spacing and icons.

## Project layout

```
shared/                 All app code (Kotlin Multiplatform + Compose)
  src/commonMain/       Screens, view models, Firebase repositories, design system
  src/androidMain/      Google sign-in (Credential Manager), share sheet
  src/iosMain/          Compose entry point for iOS, share sheet
  src/commonTest/       Unit tests for the planning rules
androidApp/             Thin Android app shell (MainActivity, manifest, icon)
iosApp/                 Thin iOS app shell (SwiftUI host, GoogleSignIn bridge)
firebase/               Firestore security rules + rules tests (emulator)
docs/SECRETS.md         What is secret, where it lives, what never goes in git
```

## How sign-in works

The IA says sign-up comes *after* the user has seen the app work (Flow 1). So:

1. **Get started** creates a silent Firebase *anonymous* account. Tasks save to Firestore right away.
   The user never sees this as a sign-in option.
2. **Save your progress → Continue with Google** links Google to that same account. The uid
   stays the same, so no data moves.
3. If that Google account already has Stepwise data (for example, on a new phone), the app
   signs in to it and copies the guest's tasks across.
4. **Log in** on the Welcome screen goes straight to Google.

## Firebase setup (one time)

Project: **`builder-a7987`**. Android package: **`Builder.com`** (as registered in Firebase). iOS bundle ID: **`com.uxankit.stepwise`**.

In the [Firebase console](https://console.firebase.google.com/project/builder-a7987):

1. **Authentication → Sign-in method**
   - Enable **Google** (pick a support email).
   - Enable **Anonymous** (used only for the silent guest account).
2. **Firestore Database → Create database** (production mode, a region near your users).
3. **Deploy the security rules** in this repo (do not use the console's default rules):
   ```sh
   npx firebase-tools login          # with the Google account that owns builder-a7987
   npx firebase-tools deploy --only firestore:rules,firestore:indexes
   ```
4. **Add the Android app** (Project settings → Your apps → Android)
   - Package name: `Builder.com` (already registered)
   - Add your debug **SHA-1** (and later the release SHA-1). Get it with:
     ```sh
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
     ```
   - Download `google-services.json` → put it at **`androidApp/google-services.json`**.
5. **Add the iOS app** (Project settings → Your apps → iOS)
   - Bundle ID: `com.uxankit.stepwise`
   - Download `GoogleService-Info.plist` → put it at **`iosApp/iosApp/GoogleService-Info.plist`**.

> Enable Google sign-in **before** downloading both files. Otherwise they miss the OAuth client
> IDs and Google sign-in will say it isn't set up.

Both files are gitignored. See [docs/SECRETS.md](docs/SECRETS.md).

## Run on Android

Requirements: Android Studio (or JDK 17+ and the Android SDK).

```sh
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # once; gitignored
./gradlew :androidApp:installDebug
```

Or open the project in Android Studio and run **androidApp**.

## Run on iOS

Requirements: a Mac with **Xcode 16 or newer** (full Xcode, not just Command Line Tools).

1. Put `GoogleService-Info.plist` in `iosApp/iosApp/` (see above).
2. Open `iosApp/iosApp.xcodeproj` in Xcode. Swift packages (Firebase 11.x, GoogleSignIn 9.x)
   resolve on first open.
3. For a real iPhone: copy `iosApp/Configuration/Secrets.example.xcconfig` to
   `Secrets.xcconfig` and set your `TEAM_ID`. The simulator needs nothing extra.
4. Run the **iosApp** scheme. Xcode builds the Kotlin framework through Gradle automatically.

The build stops with a clear message if `GoogleService-Info.plist` is missing. The Google
URL scheme is copied from that file into the built app, so no project file holds it.

## Tests

```sh
./gradlew :shared:testAndroidHostTest     # planning rules (limits, focus queue, steps)
cd firebase/tests && npm install && npm test   # security rules on the local emulator (needs Java)
```

The rules tests use a `demo-` project on the local emulator and never touch real data.

## What's in this first build

Done: onboarding (Flow 1), Today, Inbox, Progress, Quick add (Flow 2), planning with the
Urgent 3 / Today 5 soft limits and the gentle "Which ones are truly urgent?" check
(Flows 3 and 7), Task Page with manual Break Down (Flow 4), Focus Mode with Done / Skip /
Pause / Make it smaller and the Step Done screen (Flow 5), Settings (Flow 8): accessibility,
account, export, log out and delete account. Undo after Done, Skip and Delete.

Next milestone:
- **AI step suggestions.** They need a server-side function so the AI key never ships in the app.
- **Welcome Back** after 3+ days away (Flow 6). `lastActiveAt` is already recorded.
- **Reminders** (notifications). The settings row shows "Coming soon".
- **App Check** and Firebase API key restrictions (see docs/SECRETS.md).
