#!/usr/bin/env python3
"""Build using Google's SDK Build Tools and a JDK. No Gradle or network needed."""
import argparse
import hashlib
import shutil
import subprocess
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

root = Path(__file__).resolve().parent
p = argparse.ArgumentParser()
p.add_argument('--jdk', type=Path, required=True)
p.add_argument('--tools', type=Path, required=True)
p.add_argument('--platform', type=Path, required=True, help='Path to android.jar')
a = p.parse_args()
build = root / 'build'
classes = build / 'classes'
dex = build / 'dex'
for directory in (classes, dex):
    if directory.exists():
        shutil.rmtree(directory)
    directory.mkdir(parents=True)
out = root.parent / 'dist'
out.mkdir(exist_ok=True)
java = a.jdk / 'bin/java'

def run(*args):
    subprocess.run([str(x) for x in args], check=True)

run(a.jdk / 'bin/javac', '-encoding', 'UTF-8', '--release', '8',
    '-classpath', a.platform, '-d', classes, *sorted((root / 'src/main/java').rglob('*.java')), *sorted((root.parent / 'protocol/src/main/java').rglob('*.java')))
run(java, '-cp', a.tools / 'lib/d8.jar', 'com.android.tools.r8.D8',
    '--release', '--min-api', '29', '--lib', a.platform, '--output', dex,
    *sorted(classes.rglob('*.class')))
unsigned = build / 'unsigned.apk'
aligned = build / 'aligned.apk'
resources = build / 'resources.zip'
run(a.tools / 'aapt2', 'compile', '--dir', root / 'src/main/res', '-o', resources)
manifest = ET.parse(root / 'src/main/AndroidManifest.xml')
manifest.getroot().set('package', 'com.krplay.client')
generated_manifest = build / 'AndroidManifest.xml'
ET.register_namespace('android', 'http://schemas.android.com/apk/res/android')
manifest.write(generated_manifest, encoding='utf-8', xml_declaration=True)
run(a.tools / 'aapt2', 'link', '-I', a.platform, '--manifest', generated_manifest,
    '--min-sdk-version', '29', '--target-sdk-version', '32', '-o', unsigned, resources)
with zipfile.ZipFile(unsigned, 'a', compression=zipfile.ZIP_DEFLATED) as z:
    for path in dex.glob('*.dex'):
        z.write(path, path.name)
run(a.tools / 'zipalign', '-f', '4', unsigned, aligned)
keys = root / '.signing'
keys.mkdir(exist_ok=True)
keystore = keys / 'krplay.p12'
if not keystore.exists():
    run(a.jdk / 'bin/keytool', '-genkeypair', '-keystore', keystore,
        '-storepass', 'android', '-keypass', 'android', '-alias', 'krplay',
        '-keyalg', 'RSA', '-keysize', '2048', '-validity', '3650',
        '-dname', 'CN=KrPlay Local Development', '-storetype', 'PKCS12')
    keystore.chmod(0o600)
apk = out / 'KrPlay-Client-debug.apk'
run(java, '-jar', a.tools / 'lib/apksigner.jar', 'sign', '--ks', keystore,
    '--ks-key-alias', 'krplay', '--ks-pass', 'pass:android', '--key-pass', 'pass:android',
    '--out', apk, aligned)
run(java, '-jar', a.tools / 'lib/apksigner.jar', 'verify', '--verbose', apk)
run(a.tools / 'zipalign', '-c', '4', apk)
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
apk.with_suffix('.apk.sha256').write_text(f'{digest}  {apk.name}\n')
print(f'APK: {apk}\nSIZE: {apk.stat().st_size}\nSHA256: {digest}')
