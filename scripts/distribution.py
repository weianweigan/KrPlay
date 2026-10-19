#!/usr/bin/env python3
"""Embed notices in APKs and pair public binaries with their exact committed source."""
import argparse
import hashlib
import json
import os
import re
import subprocess
import zipfile
from pathlib import Path

REPOSITORY = 'https://github.com/weianweigan/KrPlay'
ROOT = Path(__file__).resolve().parents[1]


def git(root, *args):
    top = subprocess.check_output(['git', 'rev-parse', '--show-toplevel'], cwd=root, stderr=subprocess.DEVNULL)
    if Path(top.decode().strip()).resolve() != root.resolve():
        raise subprocess.CalledProcessError(1, ['git', *args])
    return subprocess.check_output(['git', *args], cwd=root, stderr=subprocess.DEVNULL)


def build_info(root):
    revision = None
    try:
        head = git(root, 'rev-parse', '--verify', 'HEAD').decode().strip()
        clean = not git(root, 'status', '--porcelain', '--untracked-files=all').strip()
        if clean:
            revision = head
    except subprocess.CalledProcessError:
        # Source ZIPs deliberately omit .git; a checksum manifest preserves provenance.
        manifest_path = root / 'SOURCE-MANIFEST.json'
        if manifest_path.is_file():
            manifest = json.loads(manifest_path.read_text())
            files = manifest['files']
            ignored_dirs = {'.git', '.gradle', '.kotlin', '.cxx', '.externalNativeBuild',
                            '.local', '.signing', '.idea', 'build', 'dist', '__pycache__', 'node_modules'}
            actual = set()
            for directory, directories, names in os.walk(root):
                directories[:] = [d for d in directories if d not in ignored_dirs]
                for name in names:
                    if name not in {'SOURCE-MANIFEST.json', 'local.properties', '.DS_Store'} and not name.endswith('.iml'):
                        actual.add((Path(directory) / name).relative_to(root).as_posix())
            if actual == set(files) and all((root / name).is_file() and
                    hashlib.sha256((root / name).read_bytes()).hexdigest() == digest
                    for name, digest in files.items()):
                candidate = manifest.get('revision', '')
                if re.fullmatch(r'[0-9a-f]{40}', candidate):
                    revision = candidate
    expected = os.environ.get('KRPLAY_SOURCE_REVISION') or os.environ.get('GITHUB_SHA')
    if expected and (not re.fullmatch(r'[0-9a-f]{40}', expected) or revision != expected):
        raise ValueError('CI source must be clean and match GITHUB_SHA')
    return dict(repository=REPOSITORY, revision=revision,
                sourceUrl=f'{REPOSITORY}/tree/{revision}' if revision else REPOSITORY,
                sourceArchiveUrl=f'{REPOSITORY}/archive/{revision}.zip' if revision else None,
                buildKind='committed-source' if revision else 'local-working-tree')


def license_files(root):
    # Preserve the full text and path of every license/notice already in the source set.
    try:
        names = git(root, 'ls-files', '--cached', '--others', '--exclude-standard', '-z').decode().split('\0')
    except subprocess.CalledProcessError:
        names = json.loads((root / 'SOURCE-MANIFEST.json').read_text())['files']
    selected = {}
    for name in sorted(set(filter(None, names))):
        path = Path(name)
        if any(word in path.name.upper() for word in ('LICENSE', 'COPYING', 'NOTICE', 'COPYRIGHT')) or any(part.lower() == 'licenses' for part in path.parts):
            full = root / path
            if full.is_file() and not full.is_symlink():
                selected[name] = full.read_bytes()
    for required in ('LICENSE', 'THIRD_PARTY_NOTICES.md', 'krplay/docs/upstream/licenses/DiAuto-AGPL-3.0.txt'):
        if required not in selected:
            raise ValueError(f'Missing distribution notice: {required}')
    return selected


def prepare_assets(root, destination):
    info = build_info(root)
    licenses = license_files(root)
    destination = destination / 'distribution'
    destination.mkdir(parents=True, exist_ok=True)
    (destination / 'build.json').write_text(json.dumps(info, indent=2) + '\n')
    # One selectable offline document includes copyright notices and complete texts.
    notice = [f'KrPlay — source code and licenses\n{REPOSITORY}\n',
              f'Source: {info["sourceUrl"]}\nBuild: {info["buildKind"]}\n',
              'Modified KrPlay source is available without charge at the URL above.\n'
              'Local working-tree builds have no verified published revision.\n'
              'See DISTRIBUTION.md for the corresponding-source distribution procedure.\n']
    for name, content in licenses.items():
        notice.append(f'\n===== {name} =====\n' + content.decode('utf-8'))
    (destination / 'NOTICE.txt').write_text('\n'.join(notice))
    return info


def create_bundle(root, destination):
    info = build_info(root)
    if not info['revision']:
        raise ValueError('Public distribution requires a committed, clean source tree; commit reviewed changes first')
    licenses = license_files(root)
    destination.mkdir(parents=True, exist_ok=True)
    revision = info['revision']
    archive = git(root, 'archive', '--format=zip', '--prefix=KrPlay-source/', revision)
    source = destination / f'KrPlay-source-{revision}.zip'
    source.write_bytes(archive)
    with zipfile.ZipFile(source) as source_zip:
        forbidden_suffixes = {'.apk', '.aab', '.jks', '.keystore', '.pk8', '.p7b', '.pem', '.key', '.p12', '.pfx'}
        for name in source_zip.namelist():
            path = Path(name)
            if path.suffix.lower() in forbidden_suffixes or set(path.parts) & {'.local', '.private', '.signing', '.git'}:
                source.unlink()
                raise ValueError(f'Private artifact in source archive: {name}')
        for required in ('LICENSE', 'THIRD_PARTY_NOTICES.md', 'DISTRIBUTION.md', 'scripts/distribution.py',
                         '.github/workflows/build.yml', 'krplay/gradlew', 'krplay-client/gradlew'):
            if 'KrPlay-source/' + required not in source_zip.namelist():
                raise ValueError(f'Source archive is missing {required}')
    with zipfile.ZipFile(source, 'a', zipfile.ZIP_DEFLATED) as output:
        hashes = {name.removeprefix('KrPlay-source/'): hashlib.sha256(output.read(name)).hexdigest()
                  for name in output.namelist() if not name.endswith('/')}
        output.writestr('KrPlay-source/SOURCE-MANIFEST.json',
                        json.dumps(dict(**info, files=hashes), indent=2) + '\n')
    license_zip = destination / 'KrPlay-licenses.zip'
    with zipfile.ZipFile(license_zip, 'w', zipfile.ZIP_DEFLATED) as output:
        for name, content in licenses.items():
            output.writestr(name, content)
    for file in (source, license_zip):
        file.with_suffix(file.suffix + '.sha256').write_text(f'{hashlib.sha256(file.read_bytes()).hexdigest()}  {file.name}\n')
    (destination / 'DISTRIBUTION.md').write_bytes((root / 'DISTRIBUTION.md').read_bytes())
    return dict(**info, sourceFile=source.name, sourceSha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                licensesFile=license_zip.name, licensesSha256=hashlib.sha256(license_zip.read_bytes()).hexdigest())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--assets', type=Path)
    parser.add_argument('--bundle', type=Path)
    args = parser.parse_args()
    if bool(args.assets) == bool(args.bundle):
        parser.error('Choose exactly one of --assets or --bundle')
    result = prepare_assets(ROOT, args.assets) if args.assets else create_bundle(ROOT, args.bundle)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
