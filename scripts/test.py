#!/usr/bin/env python3
"""Run dependency-free protocol + actual TCP bridge tests using JDK and Kotlin compiler jars."""
import argparse,os,subprocess,tempfile
from pathlib import Path
p=argparse.ArgumentParser()
p.add_argument('--jdk',type=Path,required=True)
p.add_argument('--kotlin-lib-dir',type=Path,required=True,help='Kotlin compiler distribution lib/ or Gradle lib/')
a=p.parse_args();root=Path(__file__).resolve().parents[1]
def run(*cmd):subprocess.run([str(c) for c in cmd],check=True,cwd=root)
with tempfile.TemporaryDirectory(prefix='krplay-tests-') as temp:
 classes=Path(temp);java=a.jdk/'bin/java'
 run(a.jdk/'bin/javac','-d',classes,*sorted((root/'protocol/src/main/java').rglob('*.java')),root/'tests/ProtocolTest.java',root/'tests/HttpBodyTest.java')
 run(java,'-cp',classes,'ProtocolTest')
 run(java,'-cp',classes,'HttpBodyTest')
 std=next(a.kotlin_lib_dir.glob('kotlin-stdlib-[0-9]*.jar'));cp=os.pathsep.join([str(classes),str(std)])
 bridge=root/'krplay/common/src/main/java/com/zeekr/bridge'
 run(java,'-cp',str(a.kotlin_lib_dir/'*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','11','-classpath',cp,'-d',classes,bridge/'RawTcpNaluServer.kt',bridge/'ReverseSocks5Hub.kt',bridge/'LinkProbeServer.kt',root/'tests/BridgeIntegrationTest.kt')
 run(java,'-cp',cp,'BridgeIntegrationTestKt')
