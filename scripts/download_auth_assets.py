#!/usr/bin/env python3
"""Download the pinned DiPlay APK, verify its digest, and extract only the authentication pair."""
import argparse
import hashlib
import json
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAX_BYTES = 256 * 1024 * 1024


def verified_download(source, destination):
    if not source['url'].startswith('https://github.com/shihabal3amri/DiPlay/releases/download/'):
        raise ValueError('Authentication source must be the pinned upstream GitHub release')
    request = urllib.request.Request(source['url'], headers={'User-Agent': 'KrPlay-build'})
    for attempt in range(3):
        try:
            digest = hashlib.sha256()
            total = 0
            with urllib.request.urlopen(request, timeout=60) as response, destination.open('wb') as output:
                while True:
                    block = response.read(1024 * 1024)
                    if not block:
                        break
                    total += len(block)
                    if total > MAX_BYTES:
                        raise ValueError('Upstream APK exceeds 256 MiB')
                    digest.update(block)
                    output.write(block)
            if digest.hexdigest() != source['sha256']:
                raise ValueError('Upstream APK SHA-256 mismatch; refusing to extract credentials')
            return
        except (OSError, urllib.error.URLError):
            destination.unlink(missing_ok=True)
            if attempt == 2:
                raise
            time.sleep(2 ** attempt)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    source = json.loads((ROOT / 'docs/auth-source.json').read_text())
    with tempfile.TemporaryDirectory(prefix='krplay-upstream-auth-') as directory:
        apk = Path(directory) / 'DiPlay.apk'
        verified_download(source, apk)
        subprocess.run([sys.executable, str(ROOT / 'scripts/import_auth_assets.py'), str(apk), '--output', str(args.output)], check=True)
    # This public provenance record contains no credential bytes.
    (args.output / 'provenance.json').write_text(json.dumps(source, indent=2) + '\n')
    print(f'Verified DiPlay {source["version"]} and extracted the matching authentication pair; temporary APK removed')


if __name__ == '__main__':
    main()
