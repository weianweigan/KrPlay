"""Exercise exact-source packaging with isolated commits, stale trees and extracted ZIPs."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

SPEC = importlib.util.spec_from_file_location('distribution', Path(__file__).resolve().parents[1] / 'scripts/distribution.py')
distribution = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(distribution)


class DistributionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='krplay-distribution-test-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / 'repo'
        self.root.mkdir()
        self.env = patch.dict(os.environ, {}, clear=False)
        self.env.start()
        self.addCleanup(self.env.stop)
        os.environ.pop('GITHUB_SHA', None)
        os.environ.pop('KRPLAY_SOURCE_REVISION', None)
        for name in ['LICENSE', 'THIRD_PARTY_NOTICES.md', 'DISTRIBUTION.md', 'scripts/distribution.py',
                     '.github/workflows/build.yml', 'krplay/gradlew', 'krplay-client/gradlew',
                     'krplay/docs/upstream/licenses/DiAuto-AGPL-3.0.txt']:
            file = self.root / name
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text('GNU AFFERO GENERAL PUBLIC LICENSE\nGNU GENERAL PUBLIC LICENSE\nCopyright fixture\n')
        (self.root / '.gitignore').write_text('build/\n')
        self.git('init', '-q')
        self.git('add', '.')
        self.git('-c', 'user.name=Distribution Test', '-c', 'user.email=distribution@example.invalid',
                 '-c', 'commit.gpgsign=false', 'commit', '-qm', 'Fixture source')
        self.revision = self.git('rev-parse', 'HEAD').strip()

    def git(self, *args):
        return subprocess.check_output(['git', *args], cwd=self.root, text=True, stderr=subprocess.DEVNULL)

    def test_exact_source_bundle_and_offline_notices(self):
        assets = Path(self.temp.name) / 'assets'
        info = distribution.prepare_assets(self.root, assets)
        self.assertEqual(info['revision'], self.revision)
        self.assertEqual(info['sourceArchiveUrl'], distribution.REPOSITORY + '/archive/' + self.revision + '.zip')
        self.assertIn('DiAuto-AGPL-3.0.txt', (assets / 'distribution/NOTICE.txt').read_text())
        result = distribution.create_bundle(self.root, Path(self.temp.name) / 'dist')
        with zipfile.ZipFile(Path(self.temp.name) / 'dist' / result['sourceFile']) as archive:
            self.assertIn('KrPlay-source/.github/workflows/build.yml', archive.namelist())
            archive.extractall(Path(self.temp.name) / 'extracted')
        extracted = Path(self.temp.name) / 'extracted/KrPlay-source'
        self.assertEqual(distribution.build_info(extracted)['revision'], self.revision)
        distribution.prepare_assets(extracted, extracted / 'build/assets')
        self.assertEqual(distribution.build_info(extracted)['revision'], self.revision)
        (extracted / 'scripts/distribution.py').write_text('Modified after extraction')
        self.assertIsNone(distribution.build_info(extracted)['revision'])

    def test_dirty_or_untracked_source_cannot_be_distributed_as_commit(self):
        (self.root / 'new-source.java').write_text('uncommitted')
        self.assertIsNone(distribution.build_info(self.root)['revision'])
        with self.assertRaisesRegex(ValueError, 'clean source tree'):
            distribution.create_bundle(self.root, Path(self.temp.name) / 'dist')
        (self.root / 'new-source.java').unlink()
        (self.root / 'LICENSE').write_text('modified')
        self.assertIsNone(distribution.build_info(self.root)['revision'])

    def test_committed_private_artifact_is_not_exported(self):
        (self.root / 'credential.pem').write_text('private fixture')
        self.git('add', 'credential.pem')
        self.git('-c', 'user.name=Distribution Test', '-c', 'user.email=distribution@example.invalid',
                 '-c', 'commit.gpgsign=false', 'commit', '-qm', 'Bad fixture')
        with self.assertRaisesRegex(ValueError, 'Private artifact'):
            distribution.create_bundle(self.root, Path(self.temp.name) / 'dist')
        self.assertFalse(list((Path(self.temp.name) / 'dist').glob('KrPlay-source-*.zip')))

    def test_ci_revision_mismatch_is_rejected(self):
        with patch.dict(os.environ, {'GITHUB_SHA': '0' * 40}):
            with self.assertRaisesRegex(ValueError, 'match GITHUB_SHA'):
                distribution.build_info(self.root)

    def test_modified_extracted_source_cannot_hide_additional_code(self):
        result = distribution.create_bundle(self.root, Path(self.temp.name) / 'dist')
        with zipfile.ZipFile(Path(self.temp.name) / 'dist' / result['sourceFile']) as archive:
            archive.extractall(Path(self.temp.name) / 'extracted')
        extracted = Path(self.temp.name) / 'extracted/KrPlay-source'
        (extracted / 'additional-code.java').write_text('new code')
        self.assertIsNone(distribution.build_info(extracted)['revision'])


if __name__ == '__main__':
    unittest.main()
