import sys

# 1. Revert AndroidManifest.xml
with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()
content = content.replace('android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE', 'android.permission.FOREGROUND_SERVICE_DATA_SYNC')
content = content.replace('android:foregroundServiceType="connectedDevice"', 'android:foregroundServiceType="dataSync"')
with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)

# 2. Revert LocalHostService.kt
with open('app/src/main/java/com/example/server/LocalHostService.kt', 'r') as f:
    content = f.read()
content = content.replace('ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE', 'ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC')
with open('app/src/main/java/com/example/server/LocalHostService.kt', 'w') as f:
    f.write(content)

# 3. Revert build.gradle.kts
with open('app/build.gradle.kts', 'r') as f:
    content = f.read()
target_release = """    release {
      isCrunchPngs = true
      isMinifyEnabled = true"""
replacement_release = """    release {
      isCrunchPngs = false
      isMinifyEnabled = false"""
content = content.replace(target_release, replacement_release)
with open('app/build.gradle.kts', 'w') as f:
    f.write(content)

# 4. Revert ChatScreen.kt
with open('app/src/main/java/com/example/ui/screens/ChatScreen.kt', 'r') as f:
    content = f.read()
content = content.replace('import androidx.compose.runtime.saveable.rememberSaveable\n', '')
content = content.replace('rememberSaveable { mutableStateOf("") }', 'remember { mutableStateOf("") }')
with open('app/src/main/java/com/example/ui/screens/ChatScreen.kt', 'w') as f:
    f.write(content)

# 5. Revert BackupRestoreHelper.kt
with open('app/src/main/java/com/example/data/BackupRestoreHelper.kt', 'r') as f:
    content = f.read()
content = content.replace("import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext\nimport org.json.JSONObject", "import org.json.JSONObject")
content = content.replace("suspend fun exportAllData(appDao: AppDao, prefs: SharedPreferences): String = withContext(Dispatchers.IO) {", "suspend fun exportAllData(appDao: AppDao, prefs: SharedPreferences): String {")
content = content.replace("root.toString(2)\n    }", "return root.toString(2)\n    }")
content = content.replace("suspend fun restoreAllData(jsonString: String, appDao: AppDao, prefs: SharedPreferences): RestoreSummary = withContext(Dispatchers.IO) {", "suspend fun restoreAllData(jsonString: String, appDao: AppDao, prefs: SharedPreferences): RestoreSummary {")
content = content.replace("    try {", "    return try {")
content = content.replace("RestoreSummary(", "return RestoreSummary(")
content = content.replace("return return", "return")
with open('app/src/main/java/com/example/data/BackupRestoreHelper.kt', 'w') as f:
    f.write(content)

print("Reverted all changes.")
