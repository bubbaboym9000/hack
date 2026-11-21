"""Turns javac errors in a Gradle build log into GitHub Actions error annotations.

Usage: python3 annotate_errors.py build.log
Annotations show up on the run page and in the public check-run API, so a failed
build can be diagnosed without downloading the raw logs.
"""
import re
import sys

lines = open(sys.argv[1], errors="replace").read().splitlines()
pattern = re.compile(r"^.*?(src/main/\S+?):(\d+): error: (.*)$")


def escape(text):
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


count = 0
i = 0
while i < len(lines) and count < 10:
    m = pattern.match(lines[i])
    if not m:
        i += 1
        continue
    block = [m.group(3)]
    j = i + 1
    while j < len(lines) and j < i + 6 and not pattern.match(lines[j]):
        text = lines[j].strip()
        if text:
            block.append(text)
        j += 1
    print(f"::error file={m.group(1)},line={m.group(2)}::{escape(chr(10).join(block))}")
    count += 1
    i = j

if count == 0:
    tail = [l for l in lines if l.strip()][-25:]
    print("::error title=Build failed (no javac errors found)::" + escape("\n".join(tail)))
