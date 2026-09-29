import sys
with open('app/src/main/java/com/example/data/BackupRestoreHelper.kt', 'r') as f:
    content = f.read()

content = content.replace("import org.json.JSONObject", "import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext\nimport org.json.JSONObject")

target_export = "    suspend fun exportAllData(appDao: AppDao, prefs: SharedPreferences): String {"
replacement_export = "    suspend fun exportAllData(appDao: AppDao, prefs: SharedPreferences): String = withContext(Dispatchers.IO) {"
content = content.replace(target_export, replacement_export)

target_restore = "    suspend fun restoreAllData(jsonString: String, appDao: AppDao, prefs: SharedPreferences): RestoreSummary {"
replacement_restore = "    suspend fun restoreAllData(jsonString: String, appDao: AppDao, prefs: SharedPreferences): RestoreSummary = withContext(Dispatchers.IO) {"
content = content.replace(target_restore, replacement_restore)

with open('app/src/main/java/com/example/data/BackupRestoreHelper.kt', 'w') as f:
    f.write(content)
