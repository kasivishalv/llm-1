import sys
with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

content = content.replace('android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"', 'android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"')
content = content.replace('android:foregroundServiceType="dataSync"', 'android:foregroundServiceType="connectedDevice"')

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/server/LocalHostService.kt', 'r') as f:
    content2 = f.read()

content2 = content2.replace('ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC', 'ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE')

with open('app/src/main/java/com/example/server/LocalHostService.kt', 'w') as f:
    f.write(content2)
