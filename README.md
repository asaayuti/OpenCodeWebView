# Open Code WebView

Open Code WebView is an Android application designed to seamlessly connect and interact with OpenCode v2 servers.

## Pairing with an OpenCode v2 Server

You can connect to your OpenCode v2 server using two methods:
1. **QR Code Scanner**: Tap the scan button to use Google Play Services Code Scanner to instantly scan the pairing QR code provided by your OpenCode v2 server.
2. **Manual Input**: Paste the pair link or configuration JSON directly into the connection input field.

## Building the Project

To build the debug variant of the app from the command line, run:

```bash
./gradlew assembleDebug
```

## Configuring Authentication Path

The authentication connection path template is defined as a constant in `OpenCodeUtils.kt`. To change or customize the auth path template:

1. Open `app/src/main/java/com/sayuti/opencodewebview/util/OpenCodeUtils.kt`.
2. Locate the `AUTH_CONNECT_PATH_TEMPLATE` constant.
3. Modify the template string as needed for your server setup.
