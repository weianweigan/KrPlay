#!/usr/bin/env python3
"""Require an existing version tag pointing at the checked-out commit before building."""
import argparse
import os
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TAG_PATTERN = re.compile(r'v?(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*))?')


def version_code(tag):
    match = TAG_PATTERN.fullmatch(tag)
    if not match:
        raise ValueError('Tag must be a version such as v0.4.2 or v0.4.2-rc.1')
    major, minor, patch = map(int, match.groups()[:3])
    if minor > 999 or patch > 999:
        raise ValueError('Tag minor and patch components must be <= 999')
    code = major * 1_000_000 + minor * 1_000 + patch + 1
    if not 1 <= code <= 2_100_000_000:
        raise ValueError('Tag version exceeds Android versionCode range')
    return code


def check_tag(root, tag='', ref=''):
    if not tag:
        if not ref.startswith('refs/tags/'):
            raise ValueError('A tag is required: push a version tag or provide an existing tag when dispatching')
        tag = ref.removeprefix('refs/tags/')
    code = version_code(tag)
    try:
        try:
            revision = subprocess.check_output(['git', 'rev-parse', '--verify', f'refs/tags/{tag}^{{commit}}'], cwd=root, stderr=subprocess.DEVNULL).decode().strip()
        except subprocess.CalledProcessError:
            revision = subprocess.check_output(['git', 'rev-parse', '--verify', f'{tag}^{{commit}}'], cwd=root, stderr=subprocess.DEVNULL).decode().strip()
        head = subprocess.check_output(['git', 'rev-parse', '--verify', 'HEAD'], cwd=root, stderr=subprocess.DEVNULL).decode().strip()
    except subprocess.CalledProcessError as error:
        raise ValueError('The requested tag must already exist and resolve to a commit') from error
    if revision != head:
        raise ValueError('Requested tag does not point at the checked-out commit')
    return dict(tag=tag, commit=revision, version_code=code, prerelease=bool(TAG_PATTERN.fullmatch(tag).group(4)))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tag', default=os.environ.get('INPUT_TAG', ''))
    parser.add_argument('--ref', default=os.environ.get('GITHUB_REF', ''))
    args = parser.parse_args()
    try:
        result = check_tag(ROOT, args.tag, args.ref)
    except ValueError as error:
        parser.exit(1, f'Tag validation failed: {error}\n')
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
            for key, value in result.items():
                output.write(f'{key}={str(value).lower() if isinstance(value, bool) else value}\n')
    if os.environ.get('GITHUB_ENV'):
        with open(os.environ['GITHUB_ENV'], 'a') as output:
            for key, value in [('KRPLAY_RELEASE_TAG', result['tag']), ('KRPLAY_SOURCE_REVISION', result['commit']), ('KRPLAY_VERSION_CODE', result['version_code'])]:
                output.write(f'{key}={value}\n')
    print(f'Validated tag {result["tag"]} at {result["commit"]}; Android versionCode {result["version_code"]}')


if __name__ == '__main__':
    main()
