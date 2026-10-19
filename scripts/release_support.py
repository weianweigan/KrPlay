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
    if manifest.get('tag') != tag or manifest.get('distribution', {}).get('revision') != manifest.get('commit'):
        raise ValueError('Release tag/source provenance mismatch')
    expected = {}
    for apk in manifest['apks']:
        if apk['version'] != tag:
            raise ValueError('APK version must equal the release tag')
        expected[apk['file']] = apk['sha256']
    source = manifest['distribution']
    expected[source['sourceFile']] = source['sourceSha256']
    expected[source['licensesFile']] = source['licensesSha256']
    for name, digest in expected.items():
        if Path(name).name != name or hashlib.sha256((directory / name).read_bytes()).hexdigest() != digest:
            raise ValueError(f'Release file hash mismatch: {name}')
    files = manifest['releaseFiles']
    if set(files) != set(expected) | {name + '.sha256' for name in expected} | {'artifacts.json', 'DISTRIBUTION.md', 'RELEASE-NOTES.md'}:
        raise ValueError('Unexpected or missing release assets')
    for name in files:
        if Path(name).name != name or not (directory / name).is_file():
            raise ValueError('Unsafe or missing release asset')
    for name, digest in expected.items():
        if (directory / (name + '.sha256')).read_text() != f'{digest}  {name}\n':
            raise ValueError(f'Checksum file mismatch: {name}')
    return files
