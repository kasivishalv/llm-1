import sys, re
with open('app/src/main/java/com/example/ui/screens/ChatScreen.kt', 'r') as f:
    content = f.read()

target = '''                                        Button(modifier = Modifier.weight(1f),
                                            onClick = { showModelSwitcherSheet = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Select AI Model", fontWeight = FontWeight.SemiBold)
                                        }'''

replacement = '''                                        Button(modifier = Modifier.weight(1f),
                                            onClick = { showModelSwitcherSheet = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Select AI Model",
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }'''

if target in content:
    content = content.replace(target, replacement)
    print("Fixed Select AI Model button")

target_settings = '''                                        OutlinedButton(modifier = Modifier.weight(1f),
                                            onClick = {
                                                focusManager.clearFocus()
                                                onNavigateToSettings()
                                            },
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Settings", fontWeight = FontWeight.Medium)
                                        }'''

replacement_settings = '''                                        OutlinedButton(modifier = Modifier.weight(1f),
                                            onClick = {
                                                focusManager.clearFocus()
                                                onNavigateToSettings()
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                        ) {
                                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Settings", fontWeight = FontWeight.Medium, maxLines = 1)
                                        }'''

if target_settings in content:
    content = content.replace(target_settings, replacement_settings)
    print("Fixed Settings button")

with open('app/src/main/java/com/example/ui/screens/ChatScreen.kt', 'w') as f:
    f.write(content)
