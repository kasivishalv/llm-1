import re

text = r"\*Net return = market gain minus\* fees*"
# We want to match * only if not preceded by \
pattern = re.compile(r"(?<!\\)(\*\*\*(.+?)\*\*\*|___(.+?)___|(?<!\\)\*\*(.+?)(?<!\\)\*\*|(?<!\\)__(.+?)(?<!\\)__|(?<!\\)\*(.+?)(?<!\\)\*|(?<!\\)_(.+?)(?<!\\)_|(?<!\\)~~(.+?)(?<!\\)~~|(?<!\\)`([^`]+)(?<!\\)`)")

matches = pattern.finditer(text)
for m in matches:
    print(m.group(0))
