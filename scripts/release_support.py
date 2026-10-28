"""Checks shared by local packaging tests and the tag-release workflow."""
import hashlib
import json
from pathlib import Path

AUTH_NAMES = {'assets/offline-mfi/identity.pk8', 'assets/offline-mfi/certificate.p7b'}


def verify_authentication(archive, receiver, assets_dir=None, provenance=None):
    names = [name for name in archive.namelist() if name.startswith('assets/offline-mfi/')]
    if not receiver:
        if names:
            raise ValueError('The car client must never contain authentication assets')
        return
    if assets_dir is None:
        if names:
            raise ValueError('Unexpected authentication assets in an unprovisioned APK')
        return
    if len(names) != 2 or set(names) != AUTH_NAMES:
        raise ValueError('The receiver must contain exactly the two provisioned authentication assets')
    for name in names:
        source = assets_dir / Path(name).relative_to('assets')
        data = archive.read(name)
        if not data or not source.is_file() or data != source.read_bytes():
            raise ValueError('Packaged authentication asset does not match the verified extraction')
    if provenance is not None:
        actual = json.loads((assets_dir / 'provenance.json').read_text())
        if actual != provenance:
            raise ValueError('Authentication source provenance mismatch')


def verify_release_files(directory, manifest, tag):
    if manifest.get('tag') != tag:
        raise ValueError('Release tag mismatch')
    expected = {}
    for apk in manifest['apks']:
        if apk['version'] != tag:
            raise ValueError('APK version must equal the release tag')
        expected[apk['file']] = apk['sha256']
    for source in manifest.get('dependencySources', []):
        if source['file'] not in ('ffmpeg-n7.1.1.tar.gz', 'ffmpeg-config.h'):
            raise ValueError('Unexpected dependency source asset')
        expected[source['file']] = source['sha256']
    files = manifest['releaseFiles']
    if set(files) != set(expected):
        raise ValueError('Unexpected or missing release assets')
    for name in files:
        if Path(name).name != name or not (directory / name).is_file():
            raise ValueError(f'Unsafe or missing release asset: {name}')
        actual = hashlib.sha256((directory / name).read_bytes()).hexdigest()
        if actual != expected[name]:
            raise ValueError(f'Release file hash mismatch for {name}: expected {expected[name]}, got {actual}')
    return files
