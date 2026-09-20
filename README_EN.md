<div align="center">

<img src="docs/images/app-icon.png" width="160" alt="BioPay app icon" />

<h1>BioPay</h1>

<p>为支付应用开启原生般的生物识别认证体验</p>
<p>Native-like biometric payment for WeChat, via LSPosed</p>

[![Release](https://img.shields.io/github/v/release/kiriashi/BioPay?style=flat)](https://github.com/kiriashi/BioPay/releases)
[![CI](https://github.com/kiriashi/BioPay/actions/workflows/ci.yml/badge.svg)](https://github.com/kiriashi/BioPay/actions)
[![License](https://img.shields.io/github/license/kiriashi/BioPay?style=flat)](LICENSE)
[![Android](https://img.shields.io/badge/Android-9.0%2B-green.svg?style=flat)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-purple.svg?style=flat)](https://kotlinlang.org)
[![LSPosed](https://img.shields.io/badge/LSPosed-API%20102-purple.svg?style=flat)](https://github.com/LSPosed/LSPosed)

[简体中文](README.md) | [English](README_EN.md)

</div>

## Introduction

WeChat does not offer fingerprint/face payment on some devices, forcing a
manual 6-digit password entry every time. On devices with untrusted TEE
(e.g. some OnePlus phones), a "system error" style prompt can additionally
interrupt the payment flow.

**BioPay** is a WeChat biometric payment module based on LSPosed (LibXposed
API 102): once fingerprint/face authentication passes, the payment password
is typed automatically, with a weak-face compatibility mode for older
devices — a near-native experience.

## Screenshots

<p align="center">
  <img src="docs/images/settings.png" width="300" alt="Module settings: biometric toggles and payment password" />
</p>

## Features

```
┌───────────────────────────────────────────────┐
│                    BioPay                     │
│     Native-like biometric payment for WeChat  │
└───────────────────────────────────────────────┘
        │                  │                  │
        ▼                  ▼                  ▼
 【Auth & input】     【Keyboard aware】   【Settings & storage】
 • Fingerprint/face   • Verify on keyboard • Long-press Settings
 • Weak-face compat     pop-up               to open settings
 • Volume-key retrigger • Human-like Gaussian • AES-GCM in Keystore
                        touch timing         • Logs off by default
                        • Fallback keyboard
```

- Fingerprint and face payment, with a weak-face compatibility mode covering
  a wider range of Android 9.0+ devices.
- Covers both in-WeChat payments and WeChat payments launched from external apps.
- Volume key instantly re-triggers biometric authentication, no keyboard taps needed.
- `Me → Settings → long-press "Settings"` opens the module settings page,
  where you store the 6-digit payment password and pick biometric methods.

## Architecture

The module is split into 6 packages with one-way downward dependencies;
`core` has zero project dependencies:

| Package | Responsibility |
|---|---|
| `entry` | Xposed entry `BioPayModule` + composition root `AppWiring` + lifecycle callbacks |
| `payment` | Biometric payment feature: orchestrator, auth gate, auto input, keyboard cloaking, sessions |
| `hook` | WeChat hooks: three interceptors, top-activity lookup, field store |
| `settings` | Settings page: pure UI, business controller, dialog host, custom M3 widgets |
| `data` | Data layer: AES-GCM crypto, password version policy, preference store |
| `core` | Foundation: log capture, XOR codec, Activity/dp extensions |

```
WeChat process                BioPay module
┌─────────┐  setInputEditText  ┌──────────────────────┐
│ Payment ├─────────────────►│ KeyboardWindowHook   │
│ keyboard│                   └──────────┬───────────┘
└─────────┘                              ▼
                              ┌──────────────────────┐
                              │ BiometricPayment-    │
                              │ Controller → Gate    │──► System biometric dialog
                              └──────────┬───────────┘
                                         │ on success
                                         ▼
                              ┌──────────────────────┐
                              │ PasswordAutoInput    │──► Gaussian fake touches
                              │ (Keystore decrypt →  │    typed digit by digit
                              │  type chars)         │
                              └──────────────────────┘
```

## How It Works

1. Store the 6-digit payment password on the settings page; it is encrypted
   with AES-GCM into the AndroidKeystore.
2. When the WeChat payment keyboard pops up, the module shows the system
   biometric dialog.
3. On success the password is decrypted and typed via human-like Gaussian
   fake touches, digit by digit.
4. On failure or cancel, it falls back to the normal keyboard; normal
   payment is unaffected.

Honest note: for weak-face compatibility, biometrics act as an
application-level gate here rather than a biometric-bound `CryptoObject`
(weak face cannot authorize one on some devices). Please review the code
before deciding to trust this module.

## Usage

1. Download the latest APK from [Releases](https://github.com/kiriashi/BioPay/releases) and install it.
2. Enable BioPay in the LSPosed manager with scope `com.tencent.mm`
   (WeChat); LSPosed with LibXposed API 102 support is required.
3. Restart WeChat (restart the scope in LSPosed, or force-stop and reopen).
4. Open WeChat `Me → Settings` and long-press "Settings" to open the module page.
5. Store the payment password, enable fingerprint/face, and complete one
   biometric verification when saving.

Requirements: Android 9.0+, with fingerprint or face enrolled on the device.

## Green Credentials

- **Single permission**: only `USE_BIOMETRIC`; no `INTERNET`, storage,
  notification or any other permission.
- **Zero network**: not a single line of networking code; the module itself
  makes no network connections at runtime.
- **Zero third-party libraries**: only system APIs plus our own code inside
  the APK (`libxposed` is compile-time only and not packaged).
- **No backdoors**: no malicious code — zero components, single
  permission, zero networking, fully open source and auditable.
- **Logs off by default**: debug logging is opt-in and only ever written to
  the app-private directory on the device, never uploaded.
- **Fully open and verifiable**: AGPL-3.0, every Release ships debug APK
  and full source archives alongside the release APK; you can also rebuild
  from the tag to verify from source.
  Every line of code is open to review.

## Tech Stack

- **Language**: 100% Kotlin (JVM 17 target, 21 toolchain).
- **UI**: hand-drawn Material 3 widgets on classic Views (`settings/ui`),
  no Compose, no third-party UI libraries.
- **Xposed**: LSPosed LibXposed API 102, hooks live in the `hook` package.
- **Encrypted storage**: AndroidKeystore AES-GCM with a forward-compatible
  password version policy.
- **Tests**: JUnit 4 unit tests over pure-logic units (`test/` mirrors the
  `main/` package layout).
- **Build**: Gradle + R8, fully automated CI/Release via GitHub Actions.

## Compatibility & Feedback

If a WeChat update breaks the module, please file an issue with the WeChat
version, Android version and LSPosed version. Only use this module on
devices and accounts you are authorized to modify.

## Roadmap (TODO)

- The current version targets WeChat (`com.tencent.mm`) only; other payment
  apps (Alipay, UnionPay, …) are not adapted yet.

## Build

Debug build:

```bash
./gradlew assembleDebug
```

Signed release builds take key material from environment variables
(CI secrets) or an untracked `local.properties` file:

```text
BIOPAY_RELEASE_STORE_FILE
BIOPAY_RELEASE_STORE_PASSWORD
BIOPAY_RELEASE_KEY_ALIAS
BIOPAY_RELEASE_KEY_PASSWORD
```

```properties
# local.properties (never commit this file)
RELEASE_STORE_FILE=../biopay.keystore
RELEASE_STORE_PASSWORD=<keystore password>
RELEASE_KEY_ALIAS=<key alias>
RELEASE_KEY_PASSWORD=<key password>
```

Without key material the build gracefully falls back to an unsigned APK
(`app-release-unsigned.apk`) instead of failing. Personal keystores must
never be committed to the repository.

## License

Copyright (C) 2026 kiriashi.

BioPay is free software licensed under the GNU Affero General Public License v3.0 (or any later version). See [LICENSE](LICENSE).

You may redistribute and modify it under the terms of the AGPL-3.0. Any distributed or network-deployed modifications must also be released under the AGPL-3.0 with the corresponding source code. There is no warranty.

## Disclaimer

This program is for study, research, technical exchange and personal lawful
testing only. Do not use it for anything illegal or against any platform's
terms of service. You bear all consequences of using this module, including
account bans, data loss, legal disputes or any other direct/indirect damage;
the author accepts no liability.
