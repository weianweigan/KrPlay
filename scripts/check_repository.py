#!/usr/bin/env python3
"""Check the actual Git source set for completeness and accidental local artifacts."""
import json
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
files = set(filter(None, subprocess.check_output(
    ['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=root,
).decode().split('\0')))
assert files, 'No project source found'
forbidden_suffixes = {'.apk', '.aab', '.apks', '.idsig', '.jks', '.keystore', '.pk8', '.p7b', '.pem', '.key', '.p12', '.pfx', '.pyc'}
for name in sorted(files):
    path = Path(name)
    assert path.suffix.lower() not in forbidden_suffixes, f'Local artifact or credential container in source set: {name}'
    assert not set(path.parts) & {'.git', '.gradle', '.signing', '.local', '.private', 'node_modules', '__pycache__'}, f'Local directory in source set: {name}'
    assert path.name != 'local.properties', f'Machine-specific SDK path: {name}'
    assert not (path.name == '.env' or path.name.startswith('.env.') and path.name not in {'.env.example', '.env.sample', '.env.template'}), f'Environment credentials: {name}'
    assert not name.startswith('dist/'), f'Generated distribution in Git: {name}'
    full = root / name
    assert full.exists(), f'Source file missing from checkout: {name}'
    if full.is_file():
        assert full.stat().st_size < 50 * 1024 * 1024, f'Unexpected large source file: {name}'

assert not any(name.startswith('upstream/') for name in files), 'Local upstream references must not be committed'
for required in ('LICENSE', 'settings.gradle.kts', 'gradlew', 'gradle/wrapper/gradle-wrapper.jar'):
    assert f'krplay/{required}' in files, f'Missing complete KrPlay source: {required}'
assert len(json.loads((root / 'docs/upstream-sources.json').read_text())) == 3
for required in (
    'krplay-client/src/main/java/com/krplay/client/MainActivity.java',
    'protocol/src/main/java/com/krplay/protocol/Wire.java',
    'krplay/common/src/main/java/com/zeekr/bridge/ZeekrBridgeManager.kt',
    '.github/workflows/build.yml',
    'DISTRIBUTION.md',
    'THIRD_PARTY_NOTICES.md',
    'scripts/distribution.py',
    'android-common/src/main/java/com/krplay/distribution/SourceDistribution.java',
):
    assert required in files, f'Missing KrPlay source: {required}'
entries = subprocess.check_output(['git', 'ls-files', '--stage'], cwd=root, text=True)
assert not any(line.startswith('160000 ') for line in entries.splitlines()), 'Gitlink detected: source would be missing from a normal checkout'

# Prevent removed browser transport and UI from returning to production source.
for path in (root / 'krplay/common/src/main').rglob('*'):
    if path.is_file() and path.suffix in {'.kt','.java','.js','.html','.xml','.cpp','.h'}:
        source = path.read_text()
        assert 'webrtc' not in source.lower(), f'Removed transport reference: {path}'
        assert not any(name in source for name in ('LanWebServer', 'WebSession', 'ScanQRCode')), f'Removed browser service: {path}'
assert not (root/'krplay/common/src/main/assets/web').exists(), 'Browser assets must not be packaged'
for path in (root/'krplay/shared/src/main/java').rglob('*.kt'):
    assert not any(name in path.read_text() for name in ('WifiP2pGroupManager', 'LocalOnlyHotspotManager', 'P2pStartupRecovery')), f'Removed automatic hotspot implementation: {path}'
for required in ('src/hev-main.c' ,'third-part/lwip/src/core/tcp.c','third-part/hev-task-system/src/kern/task/hev-task.c','third-part/yaml/src/api.c','SOURCE.json'):
    assert f'krplay/native/hev-socks5-tunnel/{required}' in files, f'Missing vendored VPN source: {required}'

print(f'Repository check passed: {len(files)} source files; complete KrPlay source and no upstream reference trees; no local artifacts or gitlinks')
