import sys
with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

target_release = """    release {
      isCrunchPngs = false
      isMinifyEnabled = false"""
replacement_release = """    release {
      isCrunchPngs = true
      isMinifyEnabled = true"""

content = content.replace(target_release, replacement_release)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
