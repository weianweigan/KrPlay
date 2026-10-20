# KrPlay binary and source distribution

Project/source: https://github.com/weianweigan/KrPlay

## What accompanies every public APK

Distribute the complete CI artifact together, not just its APKs:

- `KrPlay-debug.apk`, `KrPlay-Client-debug.apk` and their SHA-256 files.
- `KrPlay-source-<full-commit>.zip` and SHA-256: the complete committed project,
  including both applications, shared protocol/Android sources, vendored native
  code, Gradle wrappers, build scripts, CI workflow, original copyright headers,
  upstream provenance, license/notice files and build/install instructions.
- `KrPlay-licenses.zip` and SHA-256: full license and copyright notice files
  retained by this checkout, with original paths preserved.
- `artifacts.json`: APK hashes, versions and packages plus the source commit,
  source ZIP hash, license ZIP hash and immutable source URLs.
- This file.

Each APK also embeds `assets/distribution/build.json` and
`assets/distribution/NOTICE.txt`. Both apps offer a project link, source for the
installed revision and selectable offline copyright/license texts in settings.
The source archive includes `SOURCE-MANIFEST.json`; a build from an unchanged
extracted archive can retain its revision without needing a `.git` directory.
Modified archive contents are identified as a local working tree instead.

## Building and publishing

1. Resolve relevant asset permissions. Commit the full reviewed source and push
   it to the public project repository. The repository and exact revision must be
   readable without payment or a login before an APK is publicly distributed.
2. Build both APKs from that clean commit, using Python 3.9+, JDK 21, the Android
   SDK/NDK versions and Gradle wrappers. Build-time Python uses only the
   standard library. UI/build changes must not be omitted from the commit.
3. Run `python3 scripts/package_artifacts.py` with the Android/JDK environment
   configured. Public packaging rejects a dirty/uncommitted tree, stale APK
   provenance, missing notice assets and unexpected credentials. CI verifies
   its revision equals `GITHUB_SHA`. Public tag builds require the persistent
   APK signing secrets described below; authentication is provisioned separately. Generated output goes to ignored `dist/`.
4. Keep the whole resulting bundle together. For a GitHub Release, upload the
   two APKs, source ZIP, license ZIP, checksums and manifest on the same public
   release page; link directly to that source ZIP in the release description.
   For another APK download location, provide equivalent free access to these
   matching source files and license texts on that same download page.
5. Keep the exact source commit available. A link to a changing default branch,
   a private repository or an expired Actions artifact is not a durable source
   offer. Actions artifacts expire after 14 days and may require login: they are
   build handoff bundles, not the public source-hosting solution. Publish the
   source/release assets publicly before distributing the APK. This workflow
   does not automatically publish Releases or make the repository public.

A local uncommitted/dirty build links to the project and says `local working
tree`; it is not a public-release build. `--allow-local-auth` checks private
local APKs without claiming they have a corresponding public source bundle.
It never runs in public CI and does not authorize credential redistribution.
Source archives contain no signing keys, SDK installation, personal device
state or CarPlay authentication material. They retain dependency declarations
and vendored source. Gradle obtains external dependencies from official
repositories; preserve access to required non-system dependency source and
applicable notices when distributing a release. If extra private build steps,
patches or libraries are needed, their corresponding source/build instructions
must also be provided—this script cannot infer omitted external components.

## GPL and AGPL obligations

The receiver combines GPLv3 code with files marked AGPL-3.0-only. The root GPL
text does not replace the AGPL terms. Original headers and both full license
texts are preserved, along with third-party exceptions and asset restrictions.
The project modifies upstream software; the repository history, source headers
and provenance identify those modifications. The corresponding source includes
both apps and their build/install scripts, not merely a patch or upstream link.

For downloadable binaries, GPLv3 §6(d) calls for equivalent access to their
Corresponding Source at no further charge. This project uses the public source
repository plus the matching source archive beside each public APK download;
it does not rely on a vague promise to provide source on request.

The modified receiver supports remote interaction over a computer network.
Treat AGPLv3 §13 as applicable: all such users must be prominently offered free
access to the Corresponding Source of the actual modified version. The receiver
settings provide that offer. The receiver also advertises its source revision
in control-channel status; KrPlay-Client exposes the receiver's corresponding
source in its connection settings, independently of the client's own version.
Client-side remote links are constrained to this project's revision URLs.
For local builds without a published revision, publish the actual modified
source before offering remote use beyond private development. A downstream
fork must update the repository constant and build metadata to its own source
location and keep that version publicly accessible to its remote users.

If distributing a covered User Product under GPLv3 §6, assess whether you must
also supply Installation Information sufficient to install/run modified
versions. Current development APKs support Android sideloading; installation
steps and SDK requirements are documented. Local private signing keys and
third-party authentication material are excluded; their exclusion alone is
not evidence that every possible locked-device or commercial distribution
meets §6. Do not claim support for such a distribution without checking it.

Authoritative texts: [GNU GPLv3 §6 and §13](https://www.gnu.org/licenses/gpl-3.0.html),
[GNU AGPLv3 §13](https://www.gnu.org/licenses/agpl-3.0.html).
Third-party and trademark restrictions remain listed in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), especially maintainer-supplied
Figma images, Apple assets and Zeekr marks. Adding a source archive and notice
viewer does not resolve those separate permissions.

## In-app updates

Both applications check the latest non-draft, non-prerelease GitHub Release
once on process startup. An unavailable network or API does not block startup.
Settings also provide a manual check with retry and visible error details.
The prompt offers direct update, the official download website
<https://krplay.pages.dev/>, or postponement. Every manual-download/fallback
entry opens that website.

Direct update selects `KrPlay-<tag>.apk` or `KrPlay-Client-<tag>.apk` from that
release, with support for the older `*-debug.apk` names. It verifies download
size, GitHub's SHA-256 digest when supplied, package ID, release version,
increasing Android versionCode and matching signing certificates. It then
requests permission to install unknown apps and opens Android's installation
confirmation. Leaving the update screen cancels an active download; the user
can retry later.

For successive APKs to support in-place updates, publishers must retain the
same application ID and signing key and increment Android versionCode. Public
CI requires `KRPLAY_UPDATE_KEYSTORE_BASE64`, `KRPLAY_UPDATE_KEYSTORE_PASSWORD`,
`KRPLAY_UPDATE_KEY_ALIAS` and `KRPLAY_UPDATE_KEY_PASSWORD` repository Secrets.
Missing or invalid signing configuration stops the build. Both APKs keep their
existing IDs and use this persistent key; local builds without these variables
still use development signing.

The runner restores the key only in temporary storage, signs without Gradle
configuration caching, verifies both APK certificate fingerprints against the
provisioned certificate, and removes the key even when a build fails. The public
fingerprint is recorded in release metadata and notes. Signing passwords and
keystores are excluded from release artifacts and source archives.

Existing APKs signed with another key cannot be updated in place with this key.
The update page explains the mismatch and offers the official website. A one-time
manual reinstall can remove settings; the app never uninstalls itself.
See [fixed signing setup](docs/update-signing.zh-CN.md) for interactive key
generation, backups, Secrets setup, and local signing environment variables.
