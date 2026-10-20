#!/usr/bin/env python3
"""Provision a persistent APK signing keystore in runner temp; never print key material."""
import base64
import binascii
import hashlib
import os
from pathlib import Path
import re
import subprocess

SECRET_NAMES = (
    'KRPLAY_UPDATE_KEYSTORE_BASE64', 'KRPLAY_UPDATE_KEYSTORE_PASSWORD',
    'KRPLAY_UPDATE_KEY_ALIAS', 'KRPLAY_UPDATE_KEY_PASSWORD',
)
KEY_FILENAME = 'krplay-update.keystore'


def verify_apk_certificate(report, expected):
    """Check apksigner's verified certificate against the provisioned keystore."""
    if not re.fullmatch(r'[0-9a-fA-F]{64}', expected):
        raise ValueError('Expected APK signing certificate SHA-256 is missing or invalid')
    found = re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$', report, re.MULTILINE)
    if [value.lower() for value in found] != [expected.lower()]:
        raise ValueError('APK signing certificate does not match the persistent update key')


def provision(env):
    missing = [name for name in SECRET_NAMES if not env.get(name)]
    if missing:
        raise ValueError('Configure GitHub Actions signing secrets: ' + ', '.join(missing))
    encoded = ''.join(env['KRPLAY_UPDATE_KEYSTORE_BASE64'].split())
    if len(encoded) > 48 * 1024:
        raise ValueError('Keystore exceeds GitHub secret size limit')
    try:
        data = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError):
        raise ValueError('Keystore secret must contain valid Base64') from None
    if not data:
        raise ValueError('Keystore secret is empty')
    key = Path(env['RUNNER_TEMP']) / KEY_FILENAME
    # Exclusive creation refuses to overwrite a key or follow a preexisting symlink.
    descriptor = os.open(key, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    try:
        with os.fdopen(descriptor, 'wb') as output:
            output.write(data)
        keytool = Path(env['JAVA_HOME']) / 'bin/keytool'
        result = subprocess.run([
            str(keytool), '-exportcert', '-keystore', str(key),
            '-storepass:env', 'KRPLAY_UPDATE_KEYSTORE_PASSWORD',
            '-alias', env['KRPLAY_UPDATE_KEY_ALIAS'],
        ], env=env, capture_output=True, check=True)
        if not result.stdout:
            raise ValueError('Keystore has no signing certificate')
        fingerprint = hashlib.sha256(result.stdout).hexdigest()
        with open(env['GITHUB_OUTPUT'], 'a') as output:
            output.write(f'certificate_sha256={fingerprint}\n')
        return fingerprint
    except BaseException:
        key.unlink(missing_ok=True)
        raise


def main():
    try:
        fingerprint = provision(dict(os.environ))
    except (ValueError, OSError, subprocess.SubprocessError):
        # Avoid including subprocess stdout/stderr or secret input in Actions logs.
        raise SystemExit('APK signing setup failed. Check all four KRPLAY_UPDATE_* secrets, keystore password and alias.') from None
    print('Persistent APK signing certificate SHA-256: ' + fingerprint)


if __name__ == '__main__':
    main()
