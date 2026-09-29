import sys

# Fix BackupRestoreHelper
with open('app/src/main/java/com/example/data/BackupRestoreHelper.kt', 'r') as f:
    backup_content = f.read()

backup_content = backup_content.replace("return root.toString(2)", "root.toString(2)")
backup_content = backup_content.replace("return RestoreSummary(", "RestoreSummary(")

with open('app/src/main/java/com/example/data/BackupRestoreHelper.kt', 'w') as f:
    f.write(backup_content)

# Fix ChatScreen
with open('app/src/main/java/com/example/ui/screens/ChatScreen.kt', 'r') as f:
    chat_content = f.read()

if chat_content.startswith("import androidx.compose.runtime.saveable.rememberSaveable"):
    chat_content = chat_content.replace("import androidx.compose.runtime.saveable.rememberSaveable\npackage com.example.ui.screens", "package com.example.ui.screens\nimport androidx.compose.runtime.saveable.rememberSaveable")

with open('app/src/main/java/com/example/ui/screens/ChatScreen.kt', 'w') as f:
    f.write(chat_content)
