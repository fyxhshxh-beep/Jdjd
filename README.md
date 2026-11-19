# FriendCall V2

Native Android app (Kotlin, Jetpack Compose, Material 3) with Firebase Authentication, Cloud Firestore and the Jitsi Meet Android SDK.

## Features
- Email/password sign-up, sign-in, password reset, email verification mail
- Friend requests by email: send, accept, decline, cancel; remove friends
- Real-time one-to-one text chat, with call invites that friends can tap to join
- Voice/video calls and screen sharing through Jitsi (More, then Share your screen)
- Private room codes: 16 random characters from a secure random source

## Project layout
```
app/src/main/java/com/friendcall/app/
  MainActivity.kt           entry point, edge-to-edge setup
  call/                     CallLauncher (Jitsi), RoomCodes
  data/                     Auth/Friend/Chat repositories, models, Firestore helpers
  ui/                       MainViewModel (state + actions), FriendCallApp (root)
  ui/screens/               AuthScreen, HomeScreen (friends + call tabs), ChatScreen
  ui/theme/                 Theme
firestore.rules             Hardened security rules (deploy these)
firebase.json               Firebase CLI config for the rules
```

## Setup
1. Open the folder in Android Studio (JDK 17). Let it sync; it creates the Gradle wrapper from `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.9).
2. Create a Firebase project and register the Android package `com.friendcall.app`.
3. Enable **Email/Password** in Authentication and create a **Cloud Firestore** database.
4. Download `google-services.json` into `app/` (it is git-ignored).
5. Deploy the rules: `firebase deploy --only firestore:rules` (or paste `firestore.rules` into the console).
6. Run on a physical device (calls need a camera and microphone).

### Configuration (`gradle.properties`)
| Property | Purpose |
| --- | --- |
| `jitsiSdkVersion` | Jitsi Android SDK version. Defaults to `11.+`; pin an exact version once it builds. |
| `jitsiServerUrl` | Jitsi server. Defaults to public `https://meet.jit.si`. |

The Jitsi SDK moves fast. If Gradle can't resolve it, or the SDK demands a higher `minSdk`, check the official Jitsi Android SDK docs and adjust `jitsiSdkVersion` and `minSdk` in `app/build.gradle.kts`.

## Security model (Firestore rules)
- **Users**: you can only write your own `{uid, email}` record; accounts can be looked up by email one at a time but not listed.
- **Friend requests**: created only as yourself, with your real email, to an existing user, with a fixed id (`from_to`), so duplicates are impossible. Immutable; deletable by sender or recipient.
- **Friendships**: can only be created by the *recipient* of a pending request, in one atomic batch that also deletes the request. Emails are verified against the request. Either member can delete.
- **Messages**: only the two friends can read or write; messages are immutable, at most 2000 characters, server-timestamped, and the sender fields can't be forged. Call invites must carry a valid room code.

Not covered by rules, and worth adding before a public release: rate limiting/abuse controls (for example Firebase App Check), blocking and reporting users, push notifications (FCM), and cleanup of messages after a friendship is removed (a Cloud Function).

## Calls and privacy
Room codes are long and random, but a public Jitsi room is still only protected by its name. For truly private calls use your own Jitsi deployment or JaaS with authenticated rooms/JWT tokens, and set `jitsiServerUrl` accordingly. The public meet.jit.si service may also limit or change terms for embedded apps.

## Tests
`./gradlew test` runs the JVM unit tests (room codes, friendship ids, message types).

## Known limitations
- Friend search matches an exact email of someone who already has an account.
- Chat is text-only: no push notifications, read receipts, typing indicators or attachments.
- Release minification is off until the Jitsi ProGuard rules are added and tested.
- Video/screen-share quality adapts to device and network; 1080p isn't guaranteed.
