import sys
with open('app/src/main/java/com/example/data/BackupRestoreHelper.kt', 'r') as f:
    content = f.read()

content = content.replace("data class return RestoreSummary(", "data class RestoreSummary(")

with open('app/src/main/java/com/example/data/BackupRestoreHelper.kt', 'w') as f:
    f.write(content)
