import sys, re
with open('app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt', 'r') as f:
    content = f.read()

# 1. Add activeChatJob variable
target1 = '''    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()'''
replacement1 = '''    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var activeChatJob: kotlinx.coroutines.Job? = null'''
if target1 in content:
    content = content.replace(target1, replacement1)
    print("Added activeChatJob")

# 2. Update toggleIncognitoMode
target2 = '''    fun toggleIncognitoMode() {
        _isIncognitoMode.value = !_isIncognitoMode.value'''
replacement2 = '''    fun toggleIncognitoMode() {
        activeChatJob?.cancel()
        _isLoading.value = false
        _isIncognitoMode.value = !_isIncognitoMode.value'''
if target2 in content:
    content = content.replace(target2, replacement2)
    print("Updated toggleIncognitoMode")

# 3. Update sendMessage launch
target3 = '''        }

        viewModelScope.launch {
            val isIncognito = _isIncognitoMode.value'''
replacement3 = '''        }

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            val isIncognito = _isIncognitoMode.value'''
if target3 in content:
    content = content.replace(target3, replacement3)
    print("Updated sendMessage launch")

with open('app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt', 'w') as f:
    f.write(content)
