# Simple Wallet

Simple Wallet is a native Android solution for the Mobile Application Developer Code Challenge. It implements the complete requested flow—authentication, account selection, and a 30-day account statement—with a small set of supporting behaviors for session restoration, refresh, accessibility, and state demonstration.

The app is built with Java 17 and Android Views/XML. Since the challenge allows mock authentication and does not require a live backend, the data layer uses an in-process mock source. A successful demo login is remembered through an AES-GCM-encrypted marker in Jetpack DataStore, protected by a non-exportable Android Keystore key. Credentials are never stored. Login, account retrieval, statement retrieval, and logout each use a predictable 1.5-second simulated backend delay, making every loading state easy to observe during review.

## What is included

- Login with either an email address or username and password
- Clear empty-field validation, generic invalid-credential feedback, loading, and retryable failure states
- Multiple accounts supplied through a repository-backed data source
- Selectable account cards with ISO currency codes and locale-aware balance formatting
- A newest-first statement covering the inclusive period from 30 days ago through the current instant
- Transaction date and time, description, credit/debit type, signed amount, and balance after each transaction
- Pull-to-refresh on both account and transaction lists without hiding the current content
- An opaque login loader, translucent dashboard and statement overlays, and explicit success, empty, and error states
- Persistent session state encrypted through Android Keystore, with an explicit logout path
- Unit and instrumentation coverage for the challenge's core behavior

## Requirements

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 36.0.0
- Android Emulator or device running API 26 or later

The repository includes a Gradle wrapper configured for Gradle 9.5.0. Android Gradle Plugin 9.3.1 is declared in the version catalog.

## Build and run

For a Git submission, clone the repository URL provided with the handoff:

```powershell
$repositoryUrl = Read-Host "Repository URL supplied with the submission"
git clone $repositoryUrl simple-wallet-android
Set-Location simple-wallet-android
java -version
$env:JAVA_HOME
$env:ANDROID_HOME
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug
```

For the ZIP submission, extract it and run the checks from its root:

```powershell
Expand-Archive -LiteralPath .\Simple-Wallet-Android-Submission.zip -DestinationPath .\Simple-Wallet-Android-Submission
Set-Location .\Simple-Wallet-Android-Submission
java -version
$env:JAVA_HOME
$env:ANDROID_HOME
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug
```

These commands expect JDK 17 and the Android SDK to be installed and `JAVA_HOME` and `ANDROID_HOME` to point to those local installations. No machine-specific paths are required by the repository. The debug APK is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a running emulator or connected device:

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

You can also open the repository root directly in Android Studio. Select the `app` run configuration and an API 26 or later device.

## Demo credentials

Use either identifier with the same password:

```text
Email:    demo@example.com
Username: demo
Password: password123
```

These credentials are fixed fixtures for the challenge, not production secrets. The app never persists them.

## Tests and checks

Run the JVM tests, Android lint, and a debug build with:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

To run the UI tests, connect an unlocked emulator or device and use:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Useful report locations:

- JVM tests: `app/build/reports/tests/testDebugUnitTest/index.html`
- Instrumentation tests: `app/build/reports/androidTests/connected/debug/index.html`
- Lint: `app/build/reports/lint/lint.html`

### Reviewing empty and error states

The shipped fixtures keep the normal walkthrough focused on the successful flow, and `error@example.com` produces the retryable login-service failure. Dashboard and statement empty/error presentations are exercised without production-only test controls: `DashboardViewModelTest` verifies empty account data and account-loading failures, while `StatementViewModelTest` verifies empty statements, statement-loading failures, and refresh failures that preserve visible content. `WalletFlowTest` covers the observable login failure and loading/refresh behavior on an Android runtime.

### Validation performed

The application implementation was validated on 5 August 2026 using a Pixel 6a Android 13 (API 33) emulator:

- `testDebugUnitTest`: 45/45 tests passed across 9 suites.
- `connectedDebugAndroidTest`: 12/12 device tests passed, including encrypted session relaunch, corruption fail-closed behavior, durable logout, loader opacity/bounds, minimum visible-delay, and content-preserving refresh coverage.
- `lintDebug`: 0 errors. The 12 remaining advisories only report that newer SDK, Gradle, or library versions exist; the challenge stack is intentionally pinned to compile/target SDK 35 and the versions listed below.
- `assembleDebug` and `assembleRelease`: passed, including R8 and release resource shrinking. The release APK is unsigned by design.
- Clean cold launch after clearing app data: 1.1 seconds on the emulator; the missing DataStore/key state failed closed to login with no crash or `AndroidRuntime` exception.
- Authenticated and logged-out states were each force-stopped and cold-relaunched; the dashboard and login routes were restored respectively.
- Background snapshot: no active CPU entry and approximately 72 MB PSS for the debug build.

Following the documentation corrections on 6 August 2026, the JVM tests, lint, debug build, and R8 release build were run again successfully. No application source changed in that documentation-only revision.

## Architecture

The project keeps dependencies pointing inward. The domain layer is framework-independent, while Android-specific code is isolated to the presentation, data-infrastructure, and composition layers:

```text
Fragment / XML view
        |
        v
ViewModel / presentation state
        |
        v
Use case / domain model
        |
        v
Repository interface
        |
        v
Repository implementation
        |
        v
Local mock data source
```

`RequestDelay` is injected where each ViewModel starts an operation. The default challenge/demo implementation uses a fixed 1.5-second simulated delay, while tests replace it with an immediate recording implementation. The delay exists only to make UI states observable; a real remote data source would own network latency, and this demo collaborator would be removed. Each visible operation waits exactly once on the worker executor. Session restoration runs on the same executor behind an opaque startup screen, so DataStore and Keystore work never blocks the UI thread. Login uses an opaque blocking loader; dashboard, statement, and logout use a themed scrim with 60% opacity. A pull-to-refresh action keeps the current content visible, shows only the native swipe indicator, and reports failure through a generic Snackbar.

- **Presentation** contains the fragments, adapters, immutable UI states, formatting, and lifecycle-aware observation.
- **Domain** defines the models, repository contracts, credential validation, and 30-day filtering and sorting rules.
- **Data** fulfills those repository contracts through `FakeAuthDataSource`, `InMemoryWalletDataSource`, and a Keystore-encrypted Preferences DataStore session boundary.
- **Composition** connects the dependencies once in the application container.

The statement use case accepts transactions only within the inclusive interval `[now - 30 days, now]`, rejects future entries, and returns the newest items first. When two transactions share a timestamp, a stable transaction ID provides a deterministic tie-breaker. Monetary values use `BigDecimal`, timestamps use `Instant`, and currencies use ISO codes.

## Backend integration decision

The project deliberately includes no server, Retrofit/OkHttp client, database, or `INTERNET` permission. A network backend falls outside this challenge, while the fake implementation keeps the app deterministic and fully usable offline without hiding where a real integration would fit.

For a production service, the repository interfaces can stay in place while only the data implementations change:

```text
AuthRepositoryImpl   -> RemoteAuthDataSource   -> HTTPS authentication API
WalletRepositoryImpl -> RemoteWalletDataSource -> HTTPS accounts/statements API
```

A remote layer would be responsible for DTO-to-domain mapping, timeouts, cancellation, authenticated requests, certificate-safe TLS, server error mapping, token expiry, refresh and revocation, and encrypted token storage. The encrypted marker in this project only restores the offline demo state; it is not a server token and does not replace server-side authorization. Repository tests could continue as shared contract tests for both fake and remote implementations. If offline persistence became a requirement, a Room cache could sit behind a separate local data source.

## Dependencies and rationale

The dependency list is intentionally short and tied directly to the challenge requirements:

| Dependency | Purpose |
| --- | --- |
| AndroidX AppCompat, Activity, and Fragment | Native activity/fragment lifecycle and compatibility |
| AndroidX Lifecycle LiveData and ViewModel | Lifecycle-safe state ownership and rotation retention |
| AndroidX Preferences DataStore with RxJava 3 | Transactional Java-friendly persistence for the encrypted session envelope |
| AndroidX RecyclerView | Efficient account and transaction lists with diffing |
| AndroidX SwipeRefreshLayout | Native pull-to-refresh behavior for both lists |
| ConstraintLayout | Responsive XML layouts without deeply nested views |
| Material Components | Accessible inputs, buttons, cards, toolbars, and themes |
| JUnit 4 and AndroidX core-testing | Fast domain/presentation JVM tests |
| AndroidX Test, Espresso, and rules | End-to-end UI behavior on a real Android runtime |

### Why manual DI instead of Hilt

The app already uses dependency injection; it simply does not need a DI framework at this size. `AppContainer` serves as the composition root, providing the authentication and wallet data sources, repositories, use cases, worker executor, and request-delay implementation. Dependencies are passed explicitly through constructors, and tests can swap them for controlled implementations.

Hilt or Dagger was deliberately not added for three reasons:

- The challenge does not mandate an architecture framework and explicitly favors a "less is more" approach to dependencies.
- This is a single-module offline sample with two repositories and a small, stable object graph. Adding Hilt would introduce a Gradle plugin, annotations, generated code, and lifecycle scopes without solving meaningful complexity here.
- Manual constructor injection makes dependency ownership easy to trace, routes ViewModel creation through a small factory, and already provides the testability expected from DI.

Hilt or Dagger may become worthwhile in a larger production banking application with many feature modules, API clients, databases, authenticated scopes, build variants, and development teams. Adopting one later would change the composition layer without forcing a redesign of the domain or presentation contracts. A DI framework is an architectural scaling choice rather than a security control.

A JSON library, networking client, image loader, and database are also intentionally absent because they do not add measurable value within this scope.

## Security review

The submission includes the following security controls:

- No `INTERNET` permission; all fixture data stays on the device.
- Cleartext traffic is disabled and application backup is disabled in the manifest.
- Passwords are not stored, logged, cached, or passed to another screen.
- Authentication errors do not reveal whether an identifier exists.
- Only a fixed authenticated marker is persisted. It is encrypted with AES-256-GCM using a randomized IV, authenticated context, and an Android Keystore key; malformed or undecryptable data fails closed to login.
- Logout durably removes the encrypted marker; passwords and identifiers are never written to DataStore.
- Release builds enable shrinking/obfuscation and resource shrinking.
- No API keys, access tokens, private endpoints, signing files, or generated build artifacts belong in source control.
- Lint is configured to abort on errors. Run `lintDebug` before submission.

See [SECURITY.md](SECURITY.md) for threat scope, findings, and production requirements.

## Design

The mobile UI began with the requested Stitch workflow and was then implemented with native Android XML. Those committed XML resources are the design source of truth for the submission, preserving Material behavior, touch targets, content descriptions, and dark-theme support. Private Stitch working files and project metadata are intentionally left out of both source control and the submission archive.

## Supported configuration

| Setting | Value |
| --- | --- |
| Application ID | `dev.jeyk.simplewallet` |
| Minimum SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Compile SDK | 35 |
| Java | 17 |
| Orientation | Portrait and landscape supported |
| Network requirement | None |

## Tradeoffs

- Persistent state represents only offline demo authentication. It makes relaunches smoother but does not attempt to model token expiry, refresh, revocation, or server-side authorization.
- Loading and refresh behavior is explicit and testable around background work. The fixed 1.5-second wait demonstrates those states; it is not intended as a network-latency benchmark.
- Each account keeps its own currency, so the app does not present a misleading converted total.
- Local fixtures make the experience repeatable, but they do not prove API interoperability, token refresh, server pagination, or offline synchronization.
- For this challenge, clarity and testability take priority over framework-heavy abstractions.

## Submission hygiene

Before creating the submission archive, run:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug
git status --short
git archive --format=zip --output=Simple-Wallet-Android-Submission.zip HEAD
```

Create the archive from the validated commit so `.gradle`, `build`, IDE state, local SDK paths, emulator state, and other machine-specific artifacts stay out of the submission.
