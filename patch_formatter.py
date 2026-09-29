import sys

with open('app/src/main/java/com/example/ui/util/MarkdownFormatter.kt', 'r') as f:
    content = f.read()

# 1. Add unescape function at the end of the object
unescape_code = """
    private fun unescapeMarkdown(text: String): String {
        return text.replace("\\*", "*")
            .replace("\\_", "_")
            .replace("\\~", "~")
            .replace("\\`", "`")
            .replace("\\[", "[")
            .replace("\\]", "]")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\\\")
    }
}
"""
content = content.replace("}\n", unescape_code, 1) # wait, replace the LAST brace of object MarkdownFormatter?
