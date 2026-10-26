<p align="center">
  <img src="docs/assets/icons/krplay.png" width="96" alt="KrPlay">
  <img src="docs/assets/icons/krplay-client.png" width="96" alt="KrPlay-Client">
</p>

# KrPlay

**Give a spare Android device a new role: bring CarPlay to your car's screen.**

English · [简体中文](README.zh-CN.md)

KrPlay receives CarPlay from your iPhone on an Android phone or box, then sends the picture, sound and touch controls to **KrPlay-Client** on your head unit. It is designed around Zeekr 8295 head units that can install Android apps, with a native client that keeps everyday controls close at hand.

## What you can do

- Use your iPhone's CarPlay apps on a larger screen, with touch controls and a collapsible sidebar.
- Play audio on the client and use its microphone for Siri and calls when requested by the iPhone.
- Connect your iPhone over a configured system hotspot or USB; let the client discover the receiver automatically, or enter its address yourself.
- Internet sharing is temporarily disabled while the core projection path stabilizes.
- Adjust picture quality, inspect connection performance and export diagnostics when troubleshooting.
- On a rooted receiver, also stream Android apps through a virtual display.

## Two apps, one setup

| App | Install on | Requirements |
| --- | --- | --- |
| **KrPlay** · green icon | Spare Android phone or box | Android 9 or newer |
| **KrPlay-Client** · blue icon | Head unit | Android 10 or newer; a network route to the receiver |

Normal CarPlay reception and forwarding work without root. Internet sharing is temporarily disabled in this release while the projection path stabilizes. Android app streaming and injected touch controls require root. Vehicle firmware and network isolation affect compatibility; a fixed address cannot bypass the head unit's network restrictions.

A separate local KrPlay Box development project targets Armbian / Phicomm N1 (S905D). It is still under device validation and is not included in this Android release.

## Get started

1. Build and install both APKs. GitHub Actions can produce development APKs after a successful run.
2. Open KrPlay on the receiver. For wireless use, pair your iPhone in Android's Bluetooth settings and enter your system hotspot's exact name and password in connection settings. USB is also supported.
3. Connect the head unit's host system to the receiver's hotspot, then open KrPlay-Client. If discovery fails, enter a reachable address shown by KrPlay.
4. Allow microphone access on the client if you want Siri or call input. Accept Android's VPN prompt if you use the fixed connection entry.

Public source builds **do not include CarPlay authentication material**. CarPlay use requires separately obtained, authorized authentication material and a private local build before an unprovisioned APK can connect.

Internet sharing is disabled in this release. The fixed connection entry still uses Android VPN; only one Android VPN can be active at a time.

## Project and credits

KrPlay builds on [wheelplay](https://github.com/fython/wheelplay), [DiPlay](https://github.com/shihabal3amri/DiPlay) and [xcertplay](https://github.com/shilapi/xcertplay). Thanks to their authors and the libraries that make this project possible. Fixed upstream revisions are recorded in [source provenance](docs/upstream-sources.json).

The repository retains GPL-3.0 code and AGPL-3.0 UI components; third-party assets keep their own terms. See [LICENSE](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md). CarPlay, Apple and Zeekr names and marks belong to their respective owners. This is an independent project, with no claim of official endorsement or certification.

APK distributions must include the matching source archive, copyright notices and license texts; see [binary and source distribution](DISTRIBUTION.md).
