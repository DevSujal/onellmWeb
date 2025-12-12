#!/usr/bin/env bash
set -euo pipefail
# Build shaded jar and run Main
java -cp target/classes:target/dependency/* Main.java
