# Security Notes

## Scope

Simple Wallet is an offline code-challenge application backed by deterministic fixtures. It does not connect to a banking system, transmit credentials, persist credentials or server tokens, or handle real customer data. It persists only an encrypted marker indicating successful demo authentication.

The fixed demo credentials and wallet records are public test data, not secrets.

## Threat model

Within the challenge scope, the relevant risks are:

- accidental credential or wallet-data disclosure through logs, backups, screenshots, or persistent storage;
- unintended network communication or cleartext traffic;
- authentication-state leakage across logout or application relaunch;
- identifier enumeration through overly specific login errors;
- malformed or future-dated fixture data bypassing statement rules;
- secrets, signing materials, local SDK paths, or build artifacts entering the repository.

Compromised devices, runtime instrumentation, malicious operating systems, server-side authorization, fraudulent transactions, and regulatory controls are outside this offline sample's security boundary.

## Implemented controls

- The manifest requests no network permissions.
- `android:usesCleartextTraffic="false"` blocks cleartext network traffic if networking is introduced accidentally.
- `android:allowBackup="false"` prevents Android backup of application data.
- Credentials are compared only inside the fake authentication data source and are not persisted.
- Login failure uses a generic message rather than disclosing whether an email or username exists.
- Successful demo authentication writes only a fixed marker to Preferences DataStore. The marker is encrypted with AES-256-GCM using a fresh randomized IV and authenticated context.
- The AES key is generated and retained by Android Keystore under an app-owned alias; key material is not exported into application storage.
- Missing keys, malformed envelopes, authentication-tag failures, unknown versions, and storage read failures all fail closed to a logged-out state. Invalid values are cleared on a best-effort basis.
- Session reads and writes run on the application worker executor. An opaque startup screen blocks interaction until routing completes without blocking the UI thread.
- Logout durably removes the encrypted marker before returning to login.
- Password text is masked by the Android input control.
- Account and transaction values are immutable domain objects; money uses `BigDecimal` rather than floating-point arithmetic.
- The statement use case enforces its own inclusive 30-day window, rejects future records from the visible statement, and applies deterministic newest-first ordering.
- Release builds enable R8 code shrinking/obfuscation and resource shrinking.
- Lint is configured to fail the build on errors.

## Source-control exclusions

Do not commit:

- `local.properties` or absolute SDK paths;
- `.gradle`, `build`, APK, AAB, or test-output directories;
- keystores, signing configuration, passwords, access tokens, API keys, or private endpoints;
- IDE user state, emulator snapshots, or device logs.

The fake credentials in the README are intentionally public fixtures and must never be reused as real credentials.

## Verification

Static source review on 2026-08-05 produced these findings:

| Check | Finding |
| --- | --- |
| Manifest permissions | No `uses-permission` declarations are present; in particular, there is no Internet, storage, location, contacts, camera, or microphone access. |
| Transport and backup policy | Cleartext traffic and application backup are disabled explicitly. |
| Credential persistence | Passwords and identifiers are never persisted. Preferences DataStore contains one AES-GCM encrypted authentication marker; its key is managed by Android Keystore. |
| Session failure behavior | Missing, tampered, malformed, wrong-version, or undecryptable session data routes to login and is cleared best-effort. |
| Sensitive logging | Application source contains no Android `Log`, `System.out`, stack-trace printing, or analytics call. |
| Embedded sensitive material | No API key, access token, client secret, private key, keystore, or private endpoint was found in application source or build configuration. The documented demo password is an intentional public fixture. |
| Error disclosure | Presentation catches data-layer runtime failures and renders generic user-facing messages; repository exception text is not displayed. |

This source review does not replace the executable build, test, and lint gates. Their observed results should be recorded with the submission handoff.

Run the repository checks before release:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat connectedDebugAndroidTest
git status --short
git grep -n -I -E "(api[_-]?key|access[_-]?token|client[_-]?secret|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY)"
```

Review the manifest to confirm that no `android.permission.INTERNET`, external-storage, contact, location, camera, or microphone permission has been introduced.

### Current review result

- Android lint completed with 0 errors. Its 12 remaining messages are version-availability advisories for the deliberately pinned SDK 35 toolchain and dependencies, not code, manifest, resource, or security defects.
- Source scanning found no API keys, tokens, private-key markers, or Android logging calls.
- Unit crypto coverage verifies fresh IVs, authenticated round-trip, tamper/wrong-key/wrong-context rejection, and malformed-version rejection. Emulator coverage verifies encrypted relaunch and durable logout behavior.
- APK permission inspection found no network or sensitive platform permission. AndroidX contributes only its app-scoped dynamic-receiver protection permission.
- Debug and R8-shrunk release assemblies both completed successfully.

## Production requirements

Do not treat the fake data source as a production authentication mechanism. A production backend integration would require, at minimum:

- HTTPS-only endpoints and a reviewed network security configuration;
- server-side authentication and authorization for every account resource;
- short-lived access tokens, rotation/refresh handling, and revocation;
- Android Keystore-backed encrypted token storage;
- safe retry and timeout policies without duplicate financial actions;
- certificate/TLS validation, request/response size limits, and strict DTO validation;
- server-driven pagination and authoritative balances;
- log redaction, privacy review, audit trails, monitoring, abuse controls, and dependency scanning;
- a secure signing and release pipeline with secrets outside the repository.

## Reporting a vulnerability

Do not include sensitive details in a public issue. Share a minimal reproduction, affected commit, device/API level, and observed impact through the private contact channel supplied with the submission.
