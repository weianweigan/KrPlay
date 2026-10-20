"""Persistent signing setup must fail closed without exposing key material."""
import base64
import hashlib
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from update_signing import provision, verify_apk_certificate, KEY_FILENAME, SECRET_NAMES


class UpdateSigningTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)
        self.env = {
            'KRPLAY_UPDATE_KEYSTORE_BASE64': base64.b64encode(b'test keystore').decode(),
            'KRPLAY_UPDATE_KEYSTORE_PASSWORD': 'private-store-password',
            'KRPLAY_UPDATE_KEY_ALIAS': 'krplay',
            'KRPLAY_UPDATE_KEY_PASSWORD': 'private-key-password',
            'RUNNER_TEMP': self.temp.name,
            'JAVA_HOME': '/fake/jdk',
            'GITHUB_OUTPUT': str(self.directory / 'outputs'),
        }

    def test_every_secret_is_required_and_missing_input_leaves_no_key(self):
        for name in SECRET_NAMES:
            env = dict(self.env)
            del env[name]
            with self.assertRaises(ValueError):
                provision(env)
            self.assertFalse((self.directory / KEY_FILENAME).exists())

    def test_rejects_malformed_or_oversized_base64(self):
        for encoded in ['not-valid!', 'A' * (48 * 1024 + 1), '   ']:
            with self.assertRaises(ValueError):
                provision(dict(self.env, KRPLAY_UPDATE_KEYSTORE_BASE64=encoded))
            self.assertFalse((self.directory / KEY_FILENAME).exists())

    @patch('update_signing.subprocess.run')
    def test_private_file_permissions_and_public_certificate_output(self, run):
        run.return_value.stdout = b'public certificate DER'
        result = provision(self.env)
        key = self.directory / KEY_FILENAME
        self.assertEqual(b'test keystore', key.read_bytes())
        self.assertEqual(0o600, key.stat().st_mode & 0o777)
        self.assertEqual(hashlib.sha256(b'public certificate DER').hexdigest(), result)
        self.assertEqual(f'certificate_sha256={result}\n', (self.directory / 'outputs').read_text())
        arguments = run.call_args.args[0]
        self.assertNotIn(self.env['KRPLAY_UPDATE_KEYSTORE_PASSWORD'], arguments)
        self.assertIn('-storepass:env', arguments)
        self.assertTrue(run.call_args.kwargs['capture_output'])

    @patch('update_signing.subprocess.run')
    def test_keytool_failure_removes_key(self, run):
        run.side_effect = subprocess.CalledProcessError(1, ['keytool'], stderr=b'private diagnostic')
        with self.assertRaises(subprocess.CalledProcessError):
            provision(self.env)
        self.assertFalse((self.directory / KEY_FILENAME).exists())
        self.assertFalse((self.directory / 'outputs').exists())

    def test_refuses_to_overwrite_existing_key_or_follow_symlink(self):
        key = self.directory / KEY_FILENAME
        key.write_bytes(b'existing private key')
        with self.assertRaises(FileExistsError):
            provision(self.env)
        self.assertEqual(b'existing private key', key.read_bytes())
        key.unlink()
        target = self.directory / 'another-key'
        target.write_bytes(b'another private key')
        key.symlink_to(target)
        with self.assertRaises(FileExistsError):
            provision(self.env)
        self.assertEqual(b'another private key', target.read_bytes())

    def test_requires_exactly_one_matching_verified_apk_certificate(self):
        expected = 'a1' * 32
        report = f'Signer #1 certificate SHA-256 digest: {expected}\n'
        verify_apk_certificate(report, expected.upper())
        for invalid in ['', 'aa', 'z' * 64, 'b2' * 32]:
            with self.assertRaises(ValueError):
                verify_apk_certificate(report, invalid)
        for bad_report in ['', report + report.replace('#1', '#2')]:
            with self.assertRaises(ValueError):
                verify_apk_certificate(bad_report, expected)


if __name__ == '__main__':
    unittest.main()
