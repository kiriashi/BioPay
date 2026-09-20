# Security Policy

## Supported Versions

| Version                 | Supported          |
| ----------------------- | ------------------ |
| Latest release (1.0.0+) | :white_check_mark: |
| Older releases          | :x:                |

Security fixes are provided for the latest release on the default branch
only. If you are on an older release, update first, then check whether the
issue still reproduces.

## Reporting a Vulnerability

**Do not open a public issue for a security vulnerability.** Use GitHub's
private vulnerability reporting
([Security](../../security) → Report a vulnerability). If that is
unavailable, contact the repository owner privately.

Please include:

- A description of the issue and the affected version (APK checksum helps).
- Reproduction steps or a proof of concept.
- Device model, Android version, LSPosed version and WeChat version.
- Relevant logs with passwords, tokens and personal data removed.

What happens next:

- **First response within 48 hours** confirming receipt of your report.
- The maintainers assess severity and blast radius, then prepare a fix and
  test it on supported device configurations.
- Fixed versions ship with the next release; credit is given unless you
  prefer to stay anonymous.
- Please allow coordinated disclosure: do not publish details until a fix
  is released. Fix or mitigation proposals are welcome alongside the report.

In scope: payment-password storage, biometric gate bypass, hook integrity,
release tampering, malicious debug output. Out of scope: WeChat server-side
issues, phishing/social engineering, physical device access, findings that
require a rooted device to already be compromised by other malware.

Never include signing keys, keystore passwords, or unredacted payment
information in a report.

## Verifying Releases

- Install APKs only from this repository's GitHub Releases page.
- Prefer the signed release APK over the debug one for daily use.
- The release signature must stay stable across updates. A signature change
  is a red flag — do not install, report it privately instead.
- You can rebuild from the release tag to verify the published source; the
  full source of every release is public under AGPL-3.0.

---

Thank you for helping keep BioPay and its users safe.
