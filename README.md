# Simple Wallet

Simple Wallet is a native Android app built for the Mobile Application Developer Code Challenge. It covers login, account selection, and a 30-day account statement, with clear loading, refresh, empty, and error states.

## What it covers

- Login using `demo@example.com` or `demo`
- Multiple accounts with currency-aware balance formatting
- Newest-first transactions from the last 30 days
- Material circular loaders and full-screen loading states
- Pull-to-refresh for account and transaction lists
- Encrypted logged-in state with a clear logout flow
- Accessible Android XML layouts with light and dark themes
- Unit and emulator coverage for the main user flows

## Demo login

```text
Email:    demo@example.com
Username: demo
Password: password123
```

The credentials are fixed demo data and are never stored.

## Build and test

The project requires JDK 17, Android SDK 35, and an API 26 or newer device or emulator.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Project approach

The app uses Java 17, Android Views/XML, ViewModels, use cases, repositories, and Hilt dependency injection. The data source is an in-process mock because the challenge does not require a live backend. A real API can replace the data implementation without changing the presentation or domain contracts.

Each mock operation includes a 1.5-second delay so loading behavior is visible. Login blocks the screen, account and statement operations use translucent overlays, and pull-to-refresh keeps existing content visible.

Only an AES-GCM-encrypted authenticated marker is saved in DataStore and protected by Android Keystore. Usernames and passwords are never persisted. See [SECURITY.md](SECURITY.md) for the full security review.

## Validation

Validation consists of 45 JVM unit tests and 12 emulator tests, along with Android lint and debug and release builds. All checks passed on the final implementation.
