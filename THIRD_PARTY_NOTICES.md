# Third-party notices

Current KrPlay source review: 2026-10-19.
Project: https://github.com/weianweigan/KrPlay

KrPlay modifies the upstream applications for native receiver/client streaming,
connection management, settings and distribution. Original copyright headers
remain in source files. Current project changes are recorded by KrPlay
contributors in this repository; this notice does not claim ownership of
upstream code or third-party images.

See [the distribution procedure](DISTRIBUTION.md) for exact-version source
archives, build scripts, offline license texts and the remote-user source offer.
 Historical notices are retained in
[the upstream archive](krplay/docs/upstream/THIRD_PARTY_NOTICES.md); statements
there about preview APKs, browser services and credential bundling describe the
upstream project, not the current public KrPlay build.

## Code

- FFmpeg 7.1.1 (`n7.1.1`): copyright the FFmpeg developers; LGPL-2.1-or-later.
  The ARM64 client dynamically links minimal `avcodec`, `avutil`, and `swscale`
  libraries (H.264/HEVC software decoding, NEON and pthreads; no GPL/nonfree options).
  Full license: [FFmpeg-LGPL-2.1.txt](docs/licenses/FFmpeg-LGPL-2.1.txt).
  Exact upstream source: https://github.com/FFmpeg/FFmpeg/tree/n7.1.1 .
  `scripts/build_ffmpeg_android.sh` pins the archive SHA-256 and reproduces these
  unmodified libraries. Replacement libraries can be linked/repackaged using the
  public client CMake/Gradle project. Release bundles must accompany the FFmpeg
  source archive and build configuration as described in DISTRIBUTION.md.

- KrPlay derives from fython/wheelplay, with xcertplay and DiPlay protocol
  ancestry. Fixed source revisions and roles are in
  [docs/upstream-sources.json](docs/upstream-sources.json). Retain copyright
  headers and the [GPL version 3 text](LICENSE).
- Home/settings UI and other files carrying `SPDX-License-Identifier:
  AGPL-3.0-only` retain those terms. The full text is in
  [DiAuto-AGPL-3.0.txt](krplay/docs/upstream/licenses/DiAuto-AGPL-3.0.txt).
  The root GPL file does not relicense these files. GPLv3 section 13 permits a
  combination with AGPLv3 code; the AGPL network-source requirements must also
  be considered for the resulting combination. See the
  [GNU GPLv3 section 13](https://www.gnu.org/licenses/gpl-3.0.html#section13).
  When distributing APKs, provide their corresponding source, build scripts,
  notices and applicable license texts for the exact revision.
- Concentus/Opus: BSD-style notices are retained in
  [the vendored source license](krplay/shared/src/main/java/org/concentus/LICENSE)
  and the APK's `concentus-license.txt`.
- VPNHotspot-derived code: [GPLv3 notice](krplay/docs/VPNHOTSPOT-LICENSE.txt)
  and APK `vpnhotspot-license.txt`.
- hev-socks5-tunnel and its bundled components: provenance in
  [SOURCE.json](krplay/native/hev-socks5-tunnel/SOURCE.json); retain each
  component's license alongside its source (including lwIP, hev-task-system,
  YAML and core). These components do not all share the application's license.
- AndroidX, Compose, Material Components and Kotlin: Apache-2.0.
  Google Material icons/symbols: [icon license](krplay/docs/MATERIAL-ICONS-LICENSE.txt)
  and [symbol license](krplay/docs/licenses/Material-Symbols-Apache-2.0.txt).
- Bouncy Castle: [MIT-style license](krplay/docs/upstream/licenses/dependencies/Bouncy-Castle-LICENSE.txt).
  JmDNS: Apache-2.0. SLF4J: [MIT license](krplay/docs/upstream/licenses/dependencies/SLF4J-LICENSE.txt).
  Runtime versions are specified by the Gradle files and version catalog.
- Historical NanoHTTPD/Quickie notices remain in `krplay/docs/`; retaining a
  notice does not mean a removed library is a current runtime dependency.
- Separate local KrPlay Box development project (not included in this Android
  release or its source archive): reuses the same AirPlay/iAP2 and Concentus sources.
  Its portable JVM package includes Kotlin stdlib (Apache-2.0), Gson 2.11.0
  (Apache-2.0), and Bouncy Castle 1.79 (MIT-style); exact library JARs and
  license texts are included in the package. BlueZ, NetworkManager,
  dnsmasq, Avahi, libusb, libimobiledevice, usbmuxd and FFmpeg are installed
  separately through the operating system package manager and retain their
  respective licenses. Corresponding application source and build scripts
  are included under the Box package's `source/` directory.
  Bookworm usbmuxd 1.1.1 additionally uses a privately built compatibility
  executable. The original Debian 1.1.1-2+deb12u1 source package, SHA-256
  digests, security patches and KrPlay's USB configuration patch are included
  under its local `krplay-box/vendor/usbmuxd/` directory. Its GPL-2.0-or-3.0 notices remain intact;
  the installer also retains the exact patched source beside the executable.

## Images and marks

- **KrPlay and KrPlay-Client icons:** exported unchanged from the Figma file
  provided by the project maintainer; node/source records and generated
  variants are documented in [the icon inventory](docs/assets/icons/README.md).
  No separate copyright license or author declaration was supplied with that
  file. Export access alone does not establish redistribution rights. Confirm
  authorship and redistribution terms before a public release; do not assume
  the source-code license grants rights to all images.
- **Zeekr return-to-car icon:** exported from the same Figma file. Zeekr's mark
  remains subject to its owner's rights; the export is not evidence of a
  trademark license or endorsement.
- **CarPlay icon:** upstream obtained it from
  [Apple's developer assets](https://developer.apple.com/assets/elements/icons/carplay/carplay-96x96_2x.png).
  It remains an Apple asset, outside the source-code license. Confirm usage
  terms before distributing it. Apple and CarPlay marks belong to Apple Inc.
- README screenshots show KrPlay on Android test devices. Third-party UI,
  product names and any depicted marks remain their owners' property.

## Authentication and signing material

Public CI excludes CarPlay authentication material and local signing keys.
Private local packaging is a separate workflow; it does not grant rights to
redistribute third-party credentials or a provisioned APK. Do not publish
`.local/`, `.signing/`, `dist/` or APKs as source files.
