#!/bin/python3
# This file has been modified from the original WalkerKnapp/devolay version by the vicvalentim/devolay community-maintained fork (2026).


import subprocess
import sys

target_compiler = sys.argv[0].split("-faker")[0]

# Collect the true version of this compiler
major_version = None
minor_version = None
patch_version = None

proc = subprocess.run([target_compiler, "-dM", "-E", "-"], stdin=subprocess.DEVNULL, stdout=subprocess.PIPE)
for line in proc.stdout.splitlines():
    items = line.decode("utf-8").split()
    name = items[1]
    value = " ".join(items[2:])

    if name == "__GNUC__":
        major_version = value
    elif name == "__GNUC_MINOR__":
        minor_version = value
    elif name == "__GNUC_PATCHLEVEL__":
        patch_version = value

# Run intended command, but intercept stderr to replace vendor string with one able to parsed by gradle
proc = subprocess.Popen([target_compiler] + sys.argv[1:], stderr=subprocess.PIPE)
for line in iter(proc.stderr.readline, b''):
    if line.decode("utf-8").startswith("gcc version"):
        print(f"gcc version {major_version}.{minor_version}.{patch_version} (GCC)", file=sys.stderr)
    else:
        print(line.decode("utf-8"), file=sys.stderr, end="")

# Propagate the real compiler exit status to Gradle.
sys.exit(proc.wait())
