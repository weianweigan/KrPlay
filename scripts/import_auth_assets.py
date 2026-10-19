#!/usr/bin/env python3
"""Extract only the two local CarPlay assets from a user-provided APK and verify their key pair."""
import argparse
import os
import re
import subprocess
import tempfile
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser()
p.add_argument('apk', type=Path)
p.add_argument('--output', type=Path, default=root / '.local/auth-assets')
a = p.parse_args()

def openssl(*args, data=None):
    result = subprocess.run(['openssl', *map(str, args)], input=data, capture_output=True)
    if result.returncode:
        raise ValueError('OpenSSL rejected an authentication asset; nothing was imported')
    return result.stdout

if not a.apk.is_file() or a.apk.stat().st_size > 256 * 1024 * 1024:
    p.error('Expected a local APK no larger than 256 MiB')
with zipfile.ZipFile(a.apk) as archive:
    assets = {}
    for name in ('identity.pk8', 'certificate.p7b'):
        path = 'assets/offline-mfi/' + name
        entries = [e for e in archive.infolist() if e.filename == path]
        if len(entries) != 1 or not 0 < entries[0].file_size <= 16384:
            p.error('APK authentication resources are missing, duplicated or oversized')
        assets[name] = archive.read(entries[0])

# Validate in a private staging directory, and never print private key or certificate bytes.
a.output.mkdir(parents=True, exist_ok=True, mode=0o700)
a.output.chmod(0o700)
with tempfile.TemporaryDirectory(prefix='.verify-', dir=a.output) as temp:
    stage = Path(temp)
    for name, data in assets.items():
        file = stage / name
        file.write_bytes(data)
        file.chmod(0o600)
    key_public = openssl('pkey', '-inform', 'DER', '-in', stage / 'identity.pk8', '-pubout', '-outform', 'DER')
    certs = openssl('pkcs7', '-inform', 'DER', '-in', stage / 'certificate.p7b', '-print_certs')
    blocks = re.findall(rb'-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----', certs, re.S)
    if len(blocks) != 1:
        p.error('Expected one X.509 certificate in the PKCS#7 container')
    certificate_public = openssl('x509', '-pubkey', '-noout', data=blocks[0])
    certificate_der = openssl('pkey', '-pubin', '-outform', 'DER', data=certificate_public)
    if key_public != certificate_der:
        p.error('Private key and certificate do not match')
    description = openssl('pkey', '-pubin', '-inform', 'DER', '-text', '-noout', data=key_public)
    if b'prime256v1' not in description and b'P-256' not in description:
        p.error('Expected an EC P-256 identity')
    destination = a.output / 'offline-mfi'
    destination.mkdir(exist_ok=True, mode=0o700)
    destination.chmod(0o700)
    for name in assets:
        os.replace(stage / name, destination / name)
        (destination / name).chmod(0o600)
print('Imported two matching EC P-256 authentication assets into the local provisioning directory; no key material was printed')
