# Open Code WebView

Open Code WebView is a modern Android application designed to seamlessly connect and interact with OpenCode v2 servers.

## Tech Stack & Architecture
- **UI**: Jetpack Compose with Material 3 design.
- **Architecture**: MVVM with Repository pattern.
- **Key Libraries**: AndroidX Core, Lifecycle, ViewModel, Jetpack Navigation / Compose UI, Google Play Services Code Scanner, and WebView integration.

## Prerequisites
- Android Studio (Koala, Ladybug, or newer)
- JDK 17 or higher
- Android SDK (Min SDK 24+, Target SDK 34+)

---

## Pairing with an OpenCode v2 Server

### Pairing Methods on Server (Choose one):
- **Method 1**: Run in terminal:
  ```bash
  opencode pair --remote
  ```
- **Method 2**: Open OpenCode Desktop and click:
  **Home -> Settings -> Pairing -> Pair device**

### Client Connection Methods:
1. **QR Code Scanner**: Tap the scan button to use Google Play Services Code Scanner to instantly scan the pairing QR code provided by your OpenCode v2 server.
2. **Manual Input**: Paste the pair link or configuration JSON directly into the connection input field.

---

## Building and Testing

### Build Debug APK
To build the debug variant of the app from the command line, run:
```bash
./gradlew assembleDebug
```

### Run Tests
To run unit tests across the codebase:
```bash
./gradlew test
```

To run instrumentation tests:
```bash
./gradlew connectedAndroidTest
```

---

## Configuring Authentication Path

The authentication connection path template is defined as a constant in `OpenCodeUtils.kt`. To change or customize the auth path template:

1. Open [OpenCodeUtils.kt](file:///home/sayuti/AndroidStudioProjects/OpenCodeWebView/app/src/main/java/com/sayuti/opencodewebview/util/OpenCodeUtils.kt).
2. Locate the `AUTH_CONNECT_PATH_TEMPLATE` constant.
3. Modify the template string as needed for your server setup.
