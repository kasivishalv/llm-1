import sys
with open('app/src/main/java/com/example/ui/screens/ChatScreen.kt', 'r') as f:
    content = f.read()

if "import androidx.compose.runtime.saveable.rememberSaveable" not in content:
    content = content.replace("import androidx.compose.runtime.remember", "import androidx.compose.runtime.remember\nimport androidx.compose.runtime.saveable.rememberSaveable")

target_input = "var inputText by remember { mutableStateOf(\"\") }"
replacement_input = "var inputText by rememberSaveable { mutableStateOf(\"\") }"
content = content.replace(target_input, replacement_input)

with open('app/src/main/java/com/example/ui/screens/ChatScreen.kt', 'w') as f:
    f.write(content)
