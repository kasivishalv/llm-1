def parseTableRow(line):
    trimmed = line.strip()
    if trimmed.startswith("|"): trimmed = trimmed[1:]
    if trimmed.endswith("|"): trimmed = trimmed[:-1]
    return [x.strip() for x in trimmed.split("|")]

print(parseTableRow("| Checklist before opening a live account |"))
print(parseTableRow("| --- |"))
print(parseTableRow("| ✅ Emergency fund in place |"))
