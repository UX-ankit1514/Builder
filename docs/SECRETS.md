# Secrets and keys

**This repository is public.** Nothing below may be committed. `.gitignore` already blocks
every file listed here. Run `git status` before each commit anyway.

## Where each value lives

| What | File / place | In git? | Notes |
|---|---|---|---|
| Android Firebase config (API key, app ID, OAuth web client ID) | `androidApp/google-services.json` | No | Download from Firebase. The build reads the web client ID from it for Google sign-in. |
| iOS Firebase config (API key, client IDs, reversed client ID) | `iosApp/iosApp/GoogleService-Info.plist` | No | Download from Firebase. The build copies `REVERSED_CLIENT_ID` into the app's URL scheme. |
| Android SDK path | `local.properties` | No | Per machine. |
| Release signing keystore + passwords | `keystore.properties` + a `.jks` file | No | Or CI env vars `STEPWISE_STOREFILE`, `STEPWISE_STOREPASSWORD`, `STEPWISE_KEYALIAS`, `STEPWISE_KEYPASSWORD`. |
| Apple team ID | `iosApp/Configuration/Secrets.xcconfig` | No | Copy from `Secrets.example.xcconfig`. |
| Firebase project ID `builder-a7987` | `.firebaserc` | Yes | Not secret. It is visible in every client request. |
| Future AI provider key | Firebase Secret Manager (Cloud Functions) | Never in the app | The app will call a function; only the function holds the key. |

`keystore.properties` format:

```properties
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## What actually protects the data

Firebase client config values (the API key in the two files above) are identifiers, not
passwords. They end up inside every installed app. Keeping them out of this public repo is
good hygiene, but real protection comes from:

1. **Firestore security rules** (`firebase/firestore.rules`). A signed-in user can only reach
   `users/{their uid}/…`. Documents are checked field by field. Everything else is denied.
   `firebase/tests` proves this on the emulator. Always deploy these rules; never leave the
   console's test-mode rules on.
2. **Firebase Authentication.** Only Google and the silent guest account are enabled.
3. **API key restrictions** (to do in Google Cloud console → APIs & Services → Credentials):
   restrict the Android key to package `com.uxankit.stepwise` + your SHA-1 fingerprints, and
   the iOS key to bundle ID `com.uxankit.stepwise`.
4. **App Check** (next milestone): Play Integrity on Android, App Attest on iOS, then enforce
   it for Firestore and Auth so only the real app can call the backend.

## On the device

- Firebase Auth stores its session in the Android Keystore / iOS Keychain.
- Android backups exclude the app's databases and preferences (`data_extraction_rules.xml`),
  so the Firestore cache and auth tokens are not copied to cloud backups or other devices.
- Export files are written to the app's private cache and shared only through the system
  share sheet (Android `FileProvider`, limited to `cache/exports/`).

## If a secret leaks

1. Rotate it first (new keystore upload key via Play Console support, regenerate or restrict
   the API key in Google Cloud, delete and re-add the Firebase app if needed).
2. Then remove it from git history. Rotating matters more than rewriting history, because
   public commits may already be cloned.
