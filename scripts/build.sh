#!/bin/bash
set -e
cd /home/claude/w
rm -rf build/* && ./javac.sh -source 8 -target 8 -Xlint:-options -cp jar:stubs -d build src/PoweredRail.java src/Ext.java src/CreativeScreen.java 2>&1 | grep -v "^warning\|^Note\|^1 warning" || true
rm -rf build/org; cp build/*.class pkg/
rm -f dist/recreatecraft_patched.jar
(cd pkg && zip -q -r -X ../dist/recreatecraft_patched.jar META-INF/MANIFEST.MF .)
