import sys, re
with open('app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt', 'r') as f:
    content = f.read()

# 1. add incognitoMessageId
target1 = '''    private val _isIncognitoMode = MutableStateFlow(false)'''
replacement1 = '''    private val _isIncognitoMode = MutableStateFlow(false)
    private var incognitoMessageId = -1'''
if target1 in content:
    content = content.replace(target1, replacement1)
    print("Added incognitoMessageId")
else:
    print("Target 1 not found")

# 2. reset in toggleIncognitoMode
target2 = '''    fun toggleIncognitoMode() {
        activeChatJob?.cancel()
        _isLoading.value = false
        _isIncognitoMode.value = !_isIncognitoMode.value
        if (_isIncognitoMode.value) {
            _incognitoMessages.value = emptyList() // clear on entry'''
replacement2 = '''    fun toggleIncognitoMode() {
        activeChatJob?.cancel()
        _isLoading.value = false
        _isIncognitoMode.value = !_isIncognitoMode.value
        if (_isIncognitoMode.value) {
            _incognitoMessages.value = emptyList() // clear on entry
            incognitoMessageId = -1'''
if target2 in content:
    content = content.replace(target2, replacement2)
    print("Reset incognitoMessageId")
else:
    print("Target 2 not found")

# 3. assign in userMsg
target3 = '''                if (isIncognito) {
                    val userMsg = ChatMessage(
                        sessionId = -1,
                        role = "user",'''
replacement3 = '''                if (isIncognito) {
                    val userMsg = ChatMessage(
                        id = incognitoMessageId--,
                        sessionId = -1,
                        role = "user",'''
if target3 in content:
    content = content.replace(target3, replacement3)
    print("Assigned to userMsg")
else:
    print("Target 3 not found")

# 4. assign in aiMsg
target4 = '''                    if (isIncognito) {
                        val aiMsg = ChatMessage(
                            sessionId = -1,
                            role = "assistant",'''
replacement4 = '''                    if (isIncognito) {
                        val aiMsg = ChatMessage(
                            id = incognitoMessageId--,
                            sessionId = -1,
                            role = "assistant",'''
if target4 in content:
    content = content.replace(target4, replacement4)
    print("Assigned to aiMsg")
else:
    print("Target 4 not found")

with open('app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt', 'w') as f:
    f.write(content)
