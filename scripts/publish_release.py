#!/usr/bin/env python3
"""Publish a verified tag bundle; stage every asset in a draft before exposing it."""
import argparse
import json
import os
import subprocess
from pathlib import Path
from distribution import REPOSITORY
from release_support import verify_release_files
from release_tag import check_tag

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tag', required=True)
    parser.add_argument('--directory', type=Path, default=ROOT / 'dist/release')
    args = parser.parse_args()
    release = check_tag(ROOT, args.tag)
    repository = os.environ.get('GH_REPO', '')
    if repository.lower() != REPOSITORY.removeprefix('https://github.com/').lower():
        parser.error('GH_REPO must match the project source repository')
    directory = args.directory
    if not (directory / 'artifacts.json').is_file():
        candidates = list(directory.glob('*/artifacts.json'))
        if len(candidates) == 1:
            directory = candidates[0].parent
    manifest = json.loads((directory / 'artifacts.json').read_text())
    if manifest.get('commit') != release['commit']:
        parser.error('Release bundle must match the tag commit')
    files = verify_release_files(directory, manifest, args.tag)
    command = ['gh', 'release', 'create', args.tag, '--repo', repository, '--verify-tag',
               '--target', release['commit'], '--title', 'KrPlay ' + args.tag, '--draft',
               '--notes-file', str(directory / 'RELEASE-NOTES.md')]
    if release['prerelease']:
        command.append('--prerelease')
    # gh refuses an existing release, preserving previously published assets.
    subprocess.run(command + [str(directory / name) for name in files], check=True)
    response = subprocess.check_output(['gh', 'release', 'view', args.tag, '--repo', repository, '--json', 'assets'], text=True)
    uploaded = {asset['name'] for asset in json.loads(response)['assets']}
    if uploaded != set(files):
        raise ValueError('Draft asset set does not match the verified bundle; leaving it unpublished')
    subprocess.run(['gh', 'release', 'edit', args.tag, '--repo', repository, '--draft=false'], check=True)
    print(f'Published {REPOSITORY}/releases/tag/{args.tag}')


if __name__ == '__main__':
    main()
