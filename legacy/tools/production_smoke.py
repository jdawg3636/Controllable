#!/usr/bin/env python3
"""Run the remapped test JAR against Unimined's obfuscated Forge jar, without the dev agent.
First run: ./gradlew remapJar exportClientLaunch -Psmoke
Uses only locally resolved Minecraft files. Never packages/distributes Minecraft.
"""
import json, os, pathlib, shutil, subprocess, sys
root = pathlib.Path(__file__).resolve().parents[1]
launch = json.loads((root / 'build/client-launch.json').read_text())
classpath = []
for item in launch['classpath']:
    path = pathlib.Path(item)
    if path.is_dir() or path.name.startswith('jna-'):
        continue
    if path.name.startswith('minecraft-1.5.2-'):
        path = path.parent.parent / 'minecraft-1.5.2-MinecraftForge-FG1-merged+MinecraftForge-FG1+forge-1.5.2-7.8.1.738-official.jar'
        assert path.is_file(), path
    classpath.append(str(path))
java = launch['java'] or os.environ.get('JAVA8_HOME', '/usr/lib/jvm/temurin-8-jdk-amd64') + '/bin/java'
cwd = pathlib.Path(launch['cwd'])
mods = cwd / 'mods'
mods.mkdir(exist_ok=True)
jar = root / 'build/libs/controllable-0.1.0+1.5.2.jar'
installed = mods / 'controllable-smoke.jar'
shutil.copyfile(jar, installed)
result = cwd / 'smoke-result.txt'
result.unlink(missing_ok=True)
command = [java, '-Xmx2G', '-Djava.library.path=' + str(cwd / 'natives'),
           '-Dminecraft.applet.TargetDirectory=' + str(cwd), '-cp', os.pathsep.join(classpath),
           'net.minecraft.launchwrapper.Launch', 'Dev', '0', '--gameDir', str(cwd), '--assetsDir', str(cwd / 'resources')]
try:
    completed = subprocess.run(command, cwd=cwd, timeout=180)
    message = result.read_text() if result.exists() else 'FAIL: game exited without smoke result'
    print(message)
    sys.exit(0 if completed.returncode == 0 and message.startswith('PASS:') else 1)
finally:
    installed.unlink(missing_ok=True)
