import re

text = r"\*Net return = market gain minus* fees"
pattern = re.compile(r"(?<!\\)(\*\*\*(.+?)(?<!\\)\*\*\*|(?<!\\)___(.+?)(?<!\\)___|(?<!\\)\*\*(.+?)(?<!\\)\*\*|(?<!\\)__(.+?)(?<!\\)__|(?<!\\)\*(.+?)(?<!\\)\*|(?<!\\)_(.+?)(?<!\\)_|(?<!\\)~~(.+?)(?<!\\)~~|(?<!\\)`([^`]+)(?<!\\)`)")

matches = pattern.finditer(text)
for m in matches:
    print("Match:", m.group(0))

text2 = r"This is *italic* and \**this is not** but this \*\*is\*\* not either."
for m in pattern.finditer(text2):
    print("Match2:", m.group(0))
