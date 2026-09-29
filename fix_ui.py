import sys, re
with open('app/src/main/java/com/example/ui/screens/ChatScreen.kt', 'r') as f:
    content = f.read()

# 1. Input Bar Row
target_input_row = '''                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Add attachment (+) button supporting all documents, spreadsheets, images'''

replacement_input_row = '''                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Add attachment (+) button supporting all documents, spreadsheets, images'''

if target_input_row in content:
    content = content.replace(target_input_row, replacement_input_row)
    print("Replaced Input Row start")
else:
    print("Input Row start not found")

# Remove Spacers in Input Row
target_spacers_1 = '''                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField('''
replacement_spacers_1 = '''                            }
                            OutlinedTextField('''
if target_spacers_1 in content:
    content = content.replace(target_spacers_1, replacement_spacers_1)

target_spacers_2 = '''                                maxLines = 4
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Microphone button for voice input'''
replacement_spacers_2 = '''                                maxLines = 4
                            )
                            // Microphone button for voice input'''
if target_spacers_2 in content:
    content = content.replace(target_spacers_2, replacement_spacers_2)

target_spacers_3 = '''                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            val canSend = '''
replacement_spacers_3 = '''                            }
                            val canSend = '''
if target_spacers_3 in content:
    content = content.replace(target_spacers_3, replacement_spacers_3)

# 2. Image inside Bubble
target_image_max = '''                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip('''
replacement_image_max = '''                                .fillMaxWidth()
                                .aspectRatio(4f/3f, matchHeightConstraintsFirst = false)
                                .clip('''
if target_image_max in content:
    content = content.replace(target_image_max, replacement_image_max)
    print("Replaced Image Max Height")

# 3. Model Switcher Bottom Sheet List
target_sheet_list = '''                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {'''
replacement_sheet_list = '''                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {'''
if target_sheet_list in content:
    content = content.replace(target_sheet_list, replacement_sheet_list)
    print("Replaced Bottom Sheet Max Height")

# 4. Dialog scrollable max height
target_dialog_box = '''                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {'''
replacement_dialog_box = '''                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .verticalScroll(rememberScrollState())
                ) {'''
if target_dialog_box in content:
    content = content.replace(target_dialog_box, replacement_dialog_box)
    print("Replaced Dialog Box Max Height")
    
# 5. Modal Navigation Drawer menu row spacing
target_drawer_buttons = '''                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button('''
replacement_drawer_buttons = '''                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(modifier = Modifier.weight(1f),'''
if target_drawer_buttons in content:
    content = content.replace(target_drawer_buttons, replacement_drawer_buttons)

    # And we also need to add weight to the other button in this row
    target_drawer_button2 = '''                                        }
                                        OutlinedButton('''
    replacement_drawer_button2 = '''                                        }
                                        OutlinedButton(modifier = Modifier.weight(1f),'''
    if target_drawer_button2 in content:
        content = content.replace(target_drawer_button2, replacement_drawer_button2)

with open('app/src/main/java/com/example/ui/screens/ChatScreen.kt', 'w') as f:
    f.write(content)
