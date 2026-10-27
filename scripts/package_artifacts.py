#!/usr/bin/env python3
"""Verify and describe the two APKs built from this checkout; no publishing or secrets."""
import argparse
import hashlib
import json
import os
import re
import subprocess
import shutil
from release_tag import check_tag
from update_signing import verify_apk_certificate
from release_support import verify_authentication
import zipfile
from distribution import build_info, create_bundle
from datetime import datetime, timezone
from pathlib import Path

root = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser()
p.add_argument('--jdk', type=Path, default=os.environ.get('JAVA_HOME'))
p.add_argument('--build-tools', type=Path)
p.add_argument('--release-tag', default=os.environ.get('KRPLAY_RELEASE_TAG', ''))
p.add_argument('--release-auth', action='store_true', help='Include the pinned, verified DiPlay pair in a tag release')
p.add_argument('--allow-local-auth', action='store_true', help='Validate a private local build; never use in public CI')
p.add_argument('--device-test-report', type=Path, help='Record separately completed device testing; CI omits this')
a = p.parse_args()
if a.allow_local_auth and os.environ.get("GITHUB_ACTIONS"):
    p.error("Private local packaging is disabled in GitHub Actions")
if a.release_auth and (a.allow_local_auth or not a.release_tag):
    p.error('--release-auth requires a release tag and cannot be combined with --allow-local-auth')
release = check_tag(root, a.release_tag) if a.release_tag else None
signing_certificate = os.environ.get('KRPLAY_UPDATE_CERTIFICATE_SHA256', '')
if release and not re.fullmatch(r'[0-9a-fA-F]{64}', signing_certificate):
    p.error('Tag releases require KRPLAY_UPDATE_CERTIFICATE_SHA256 from persistent signing setup')
if a.release_tag and a.allow_local_auth:
    p.error('Tag releases must include their corresponding public source bundle')
destination = root / 'dist/release' if release else root / 'dist'
destination.mkdir(parents=True, exist_ok=True)
auth_source = json.loads((root / 'docs/auth-source.json').read_text()) if a.release_auth else None
if a.release_auth and not os.environ.get('KRPLAY_AUTH_ASSETS_DIR'):
    p.error('Set KRPLAY_AUTH_ASSETS_DIR to the verified extraction directory')
auth_dir = Path(os.environ['KRPLAY_AUTH_ASSETS_DIR']) if a.release_auth else root / '.local/auth-assets' if a.allow_local_auth else None
report = None
if a.device_test_report:
    candidate = (root / a.device_test_report).resolve()
    assert candidate.is_relative_to(root) and candidate.is_file(), 'Device report must be a file in the project'
    report = candidate.relative_to(root).as_posix()
if not a.jdk:
    p.error('Set JAVA_HOME or --jdk')
tools = a.build_tools
if tools is None:
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        p.error('Set ANDROID_HOME / ANDROID_SDK_ROOT or --build-tools')
    tools = Path(sdk) / 'build-tools/36.0.0'

source_info = build_info(root)
artifacts = []
for name, package, minimum in [
    ('KrPlay-Client-debug.apk', 'com.krplay.client', 29),
    ('KrPlay-debug.apk', 'com.krplay.server.debug', 28),
]:
    apk = root / 'dist' / name
    signature = subprocess.check_output([str(a.jdk / 'bin/java'), '-jar', str(tools / 'lib/apksigner.jar'),
                                         'verify', '--print-certs', str(apk)], text=True)
    if signing_certificate:
        verify_apk_certificate(signature, signing_certificate)
    subprocess.run([str(tools / 'zipalign'), '-c', '4', str(apk)], check=True)
    metadata = subprocess.check_output([str(tools / 'aapt2'), 'dump', 'badging', str(apk)], text=True)
    assert f"package: name='{package}'" in metadata, f'Unexpected application ID in {name}'
    assert f"minSdkVersion:'{minimum}'" in metadata
    assert "targetSdkVersion:'32'" in metadata
    expected_label = "KrPlay-Client" if minimum == 29 else "KrPlay"
    assert f"application-label:'{expected_label}'" in metadata
    version = re.search(r"versionName='([^']*)'", metadata).group(1)
    if release:
        assert version == release['tag'], f'APK version does not match tag: {name}'
        assert f"versionCode='{release['version_code']}'" in metadata, f'APK versionCode does not match tag: {name}'
    with zipfile.ZipFile(apk) as archive:
        embedded = json.loads(archive.read('assets/distribution/build.json'))
        assert embedded == source_info, f'Stale or mismatched source provenance in {name}; rebuild both APKs'
        notices = archive.read('assets/distribution/NOTICE.txt').decode()
        assert 'GNU AFFERO GENERAL PUBLIC LICENSE' in notices and 'GNU GENERAL PUBLIC LICENSE' in notices
        assert 'THIRD_PARTY_NOTICES.md' in notices
        verify_authentication(archive, minimum == 28, auth_dir, auth_source)
        if minimum == 29:
            assert apk.stat().st_size < 10 * 1024 * 1024, 'Car client exceeds 10 MiB'
            assert not any(n.endswith('.so') for n in archive.namelist()), 'Car client must not depend on native libraries'
        else:
            for abi in ('arm64-v8a','armeabi-v7a','x86_64'):
                assert f'lib/{abi}/libhev-socks5-tunnel.so' in archive.namelist(), f'Missing VPN library: {abi}'
    if release:
        label = 'KrPlay-Client' if minimum == 29 else 'KrPlay'
        output = destination / f'{label}-{release["tag"]}.apk'
        shutil.copyfile(apk, output)
        apk = output
        name = output.name
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    apk.with_suffix('.apk.sha256').write_text(f'{digest}  {name}\n')
    artifacts.append(dict(file=name, package=package, version=version, minSdk=minimum, targetSdk=32, bytes=apk.stat().st_size, sha256=digest))
distribution = None if a.allow_local_auth else create_bundle(root, destination)
manifest = dict(
    builtAtUtc=datetime.now(timezone.utc).isoformat(), commit=source_info['revision'], repository=source_info['repository'], distribution=distribution,
    tag=release['tag'] if release else None, signing='persistent' if signing_certificate else 'development',
    signingCertificateSha256=signing_certificate or None,
    authentication='upstream-release' if a.release_auth else 'local-private' if a.allow_local_auth else 'not-bundled',
    authenticationSource=auth_source, deviceTested=report is not None, deviceTestReport=report, apks=artifacts,
)
if release:
    manifest['releaseFiles'] = [apk['file'] for apk in artifacts]
    apk_rows = []
    for apk in artifacts:
        target = "Receiver (Phone / TV Box, Android 9+)" if apk['minSdk'] == 28 else "Head Unit (Car screen / Tablet, Android 10+)"
        size_mb = f"{apk['bytes'] / (1024 * 1024):.2f} MB" if apk['bytes'] >= 1024 * 1024 else f"{apk['bytes'] / 1024:.1f} KB"
        apk_rows.append(f"| `{apk['file']}` | {target} | {size_mb} | `{apk['sha256']}` |")
    apk_table = "\n".join(apk_rows)

    notes = f"""# KrPlay {release['tag']}

## Downloads & Checksums

| Package | Target | Size | SHA-256 |
| :--- | :--- | :--- | :--- |
{apk_table}

## Notes
- Modern dark mode styling and theme support for head-unit client and receiver.
- Support customized CarPlay device name and display settings.
- Realtime audio queue synchronization and jitter reduction.
- Video stream recovery, catch-up and latency optimizations.
- Internet borrowing is temporarily disabled while the core projection path stabilizes.
- **Receiver (`KrPlay-{release['tag']}.apk`)**: Includes and verifies matching runtime CarPlay authentication assets; supports system hotspot and USB wired connections.
- **Head Unit (`KrPlay-Client-{release['tag']}.apk`)**: Ultra-lightweight with zero native library dependencies; automatically discovers and streams CarPlay display to the car screen.
- Source code is available via GitHub's automatic Source Code (zip / tar.gz) archives.
- Both apps check GitHub Releases at startup and offer direct APK updates or the official download website: https://krplay.pages.dev/. Manual update checks are available in settings.
- Both APKs use the persistent project signing key. Android will still ask you to confirm installation.
- Signing certificate SHA-256: `{signing_certificate}`. Versions signed with a different key require a one-time manual reinstall.
"""
    (destination / 'RELEASE-NOTES.md').write_text(notes)
(destination / 'artifacts.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
print(f'Verified both KrPlay APKs; metadata and SHA-256 written to {destination.relative_to(root)}/')
