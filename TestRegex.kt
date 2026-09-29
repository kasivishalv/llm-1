fun main() {
    val text = "\\*Net return = market gain minus* fees"
    val pattern = Regex("""(?<!\\)(\*\*\*(.+?)(?<!\\)\*\*\*|(?<!\\)___(.+?)(?<!\\)___|(?<!\\)\*\*(.+?)(?<!\\)\*\*|(?<!\\)__(.+?)(?<!\\)__|(?<!\\)\*(.+?)(?<!\\)\*|(?<!\\)_(.+?)(?<!\\)_|(?<!\\)~~(.+?)(?<!\\)~~|(?<!\\)`([^`]+)(?<!\\)`)""", RegexOption.DOT_MATCHES_ALL)
    
    val matches = pattern.findAll(text)
    for (m in matches) {
        println("Match: " + m.value)
    }
    
    val text2 = "This is *italic* and \\**this is not**"
    for (m in pattern.findAll(text2)) {
        println("Match2: " + m.value)
    }
}
