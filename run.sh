#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build
javac -d build src/main/java/learning/fintech/*.java src/test/java/learning/fintech/*.java
if [ "${1:-}" = test ]; then
  java -cp build learning.fintech.WorkspaceJoinTest
else
  java -cp build learning.fintech.FintechJoinServer
fi
