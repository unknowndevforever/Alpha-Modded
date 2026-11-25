#!/bin/bash
set -e
cd /home/claude/w
./javac.sh --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED -d pbuild src/Patch.java 2>&1 | grep -v "^Note\|warning" || true
rm -rf out && mkdir out
java --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED -cp pbuild Patch jar out | tail -1
# keep our textures/manifest, refresh class files
cp -r out/. pkg/ && rm -rf pkg/META-INF/*.SF pkg/META-INF/*.DSA && cp pkg_manifest.mf pkg/META-INF/MANIFEST.MF
python3 apply_tex.py
./build.sh
