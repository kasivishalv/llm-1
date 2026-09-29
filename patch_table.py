import sys

with open('app/src/main/java/com/example/ui/util/MarkdownFormatter.kt', 'r') as f:
    content = f.read()

# 1. Update the columnWidths logic to be slightly wider to encourage scrolling instead of aggressive wrapping
old_widths = """            when {
                maxLen < 15 -> 120.dp
                maxLen < 40 -> 160.dp
                maxLen < 80 -> 210.dp
                else -> 260.dp
            }"""
new_widths = """            when {
                maxLen < 15 -> 100.dp
                maxLen < 30 -> 140.dp
                maxLen < 50 -> 200.dp
                maxLen < 80 -> 260.dp
                else -> 320.dp
            }"""
content = content.replace(old_widths, new_widths)

# 2. Fix the white gap by making the Surface wrap content instead of fillMaxWidth
old_surface = """    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {"""
new_surface = """    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .padding(vertical = 8.dp)
            // Removed fillMaxWidth() to fix the white gap on narrow tables
    ) {"""
content = content.replace(old_surface, new_surface)

# 3. We also need to fix the Box inside the Surface to wrap content width, 
# so it doesn't force a full width if the table is small.
old_box = """        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {"""
new_box = """        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
        ) {"""
content = content.replace(old_box, new_box)

with open('app/src/main/java/com/example/ui/util/MarkdownFormatter.kt', 'w') as f:
    f.write(content)

print("Table patched.")
