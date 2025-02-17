## 🚀 Creating a macOS Application with `jpackage`
1. Do maven package
2. Then run the below command
```jpackage --name TBI --input target --main-jar TBI-1.0-SNAPSHOT.jar --main-class apidemo.stategies.CalendarSpreadStrategy --type dmg --dest mac-app --icon src/main/resources/TBI.icns```
