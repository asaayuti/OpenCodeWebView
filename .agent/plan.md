# Project Plan

Build a minimal Android app: a WebView client for OpenCode (v2) web UI.

Stack:
- Kotlin, Jetpack Compose, single Activity, minSdk 26, targetSdk 35
- Gradle Kotlin DSL, version catalog
- Dependencies: androidx.activity-compose, androidx.webkit, com.google.android.gms:play-services-code-scanner, androidx.datastore:datastore-preferences
- No other third-party libraries

How OpenCode pairing works:
- Server runs opencode pair --remote, printing a one-time link (valid 5 min) and a QR code.
- QR payload JSON: {"code":"<code>","urls":["https://<host>.opentunnel.xyz"]}
- Opening <url>/auth/connect/<code> signs in with a session cookie (valid 30 days) and loads web UI.
- Path template configurable via a single constant (AUTH_CONNECT_PATH_TEMPLATE = "/auth/connect/%s"), logged in debug.

Features:
1. Setup screen (when no saved server URL):
   - Button "Scan QR" using GmsBarcodeScanning (Play Services Code Scanner, no camera permission)
   - Text field "Paste pair link" + Connect button (accepts full https link or QR URL)
   - Parse QR JSON: take urls[0] and code, build <base>/auth/connect/<code>
   - If urls has multiple entries, try them in order until one loads
   - Show clear error if JSON/link is invalid
2. WebView screen:
   - Load pair URL on first connect; afterwards load only saved base URL
   - Enable JavaScript & DOM storage
   - CookieManager: accept cookies and third-party cookies, call flush() after page finish
   - Back button: webView.goBack() if possible, otherwise exit
   - Support file uploads via WebChromeClient.onShowFileChooser
   - Swipe-to-refresh
   - Keep screen on option (off by default)
   - Open external links (different host) in system browser
3. Server management:
   - Menu action "Change server" clears saved URL and returns to Setup
   - Menu action "Log out" clears cookies and saved URL
   - Page load failure or 401/403 shows error screen with "Re-pair" and "Retry"
4. Persistence: save base URL in DataStore Preferences. Never store pair code/passwords.
5. Security:
   - HTTPS only (cleartextTrafficPermitted=false)
   - No hardcoded URLs/tokens
   - Disable file/content access in WebSettings, no JS interfaces
   - allowBackup=false
   - Only load URLs matching saved base URL host inside WebView
6. UI: Material 3, light/dark follow system, edge-to-edge
7. Deliverables: buildable with ./gradlew assembleDebug, short README, no extra features.

## Project Brief

# OpenCode WebView - Project Brief (MVP)

## Features
- **Server Pairing & Setup**: Fast server pairing via Play Services Code Scanner (QR scan) or manual pair link input, automatically parsing JSON payloads and fallback URL logic to establish auth connections.
- **OpenCode Web Interface**: Full-featured WebView client supporting JavaScript, DOM storage, session cookie retention, file upload support (`WebChromeClient.onShowFileChooser`), and swipe-to-refresh capability.
- **Security & Domain Isolation**: Confines navigation strictly to the paired host URL, enforces HTTPS-only traffic, disables local file/content access, and redirects third-party links to the system browser.
- **Session & State Management**: Persistent base URL storage across launches with built-in connection handling, error states ("Re-pair" / "Retry"), server switching, and cookie-clearing sign-out flows.

## High-Level Technical Stack
- **Language & Concurrency**: Kotlin, Kotlin Coroutines
- **UI Framework**: Jetpack Compose, Material 3
- **Navigation & Adaptive Strategy**: Jetpack Navigation 3 (state-driven navigation architecture), Compose Material Adaptive library (`androidx.compose.material3.adaptive`)
- **Core Components & Integration**:
  - `androidx.webkit` (WebView & CookieManager integration)
  - `com.google.android.gms:play-services-code-scanner` (QR code scanning without camera permissions)
- **Persistence**: `androidx.datastore:datastore-preferences` (Securely saving base URL)

## Implementation Steps
**Total Duration:** 20m 7s

### Task_1_DependenciesAndDataStore: Configure Gradle build dependencies (androidx.webkit, play-services-code-scanner, datastore-preferences), Manifest settings (cleartextTrafficPermitted=false, allowBackup=false), and create DataStore repository for managing saved base URL.
- **Status:** COMPLETED
- **Updates:** Configured Gradle dependencies (androidx.webkit, play-services-code-scanner, datastore-preferences), updated AndroidManifest with allowBackup=false and INTERNET permission, created network_security_config with cleartextTrafficPermitted=false, and implemented ServerPreferencesRepository for managing saved base URL. Verified with unit tests and assembleDebug.
- **Acceptance Criteria:**
  - project builds successfully
  - DataStore repository implemented for storing and clearing base URL
  - AndroidManifest updated with required settings
- **Duration:** 6m 19s

### Task_2_SetupAndPairingScreen: Implement the Setup/Pairing screen with GmsBarcodeScanning (Play Services Code Scanner) QR scan and manual pair link/QR JSON text input parsing, saving base URL on success.
- **Status:** COMPLETED
- **Updates:** Implemented OpenCodeUtils with AUTH_CONNECT_PATH_TEMPLATE constant and debug logging, PairingParser for parsing QR JSON and manual links with unit tests, SetupViewModel, and SetupScreen with Play Services Code Scanner and M3 UI. Integrated with MainActivity and verified with assembleDebug and testDebugUnitTest.
- **Acceptance Criteria:**
  - Setup UI with Scan QR button and Paste pair link text field
  - QR JSON payload parsing logic correctly extracts urls and code to construct connect URL
  - Base URL persistence on pairing success
- **Duration:** 8m 8s

### Task_3_WebViewAndServerManagement: Implement the WebView screen with CookieManager, WebChromeClient file chooser, swipe-to-refresh, external URL interception, load failure error screen, and top menu for server switching/logout.
- **Status:** COMPLETED
- **Updates:** Implemented WebViewScreen with CookieManager (cookies and third-party cookies + flush), WebChromeClient file upload support, swipe-to-refresh, domain isolation and external URL interception, back button handling, error handling screen for load errors and HTTP 401/403, keep screen on toggle, and top menu with Change server and Log out actions. Wired everything in MainActivity and verified with assembleDebug.
- **Acceptance Criteria:**
  - WebView configured with JS, DOM storage, CookieManager, and file chooser
  - Domain isolation limits WebView to base URL host and routes third-party links to system browser
  - Swipe-to-refresh and keep screen on options supported
  - Menu actions for Change Server and Log Out properly clear state and cookies
  - Page load failures display error screen with Re-pair and Retry options
- **Duration:** 4m 25s

### Task_4_RunAndVerify: Build assembleDebug, run tests, and perform final verification. Instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Ran assembleDebug and testDebugUnitTest successfully. Critic agent verified app stability, startup, and full alignment with all requirements. Created README.md with pairing, build, and auth path configuration instructions.
- **Acceptance Criteria:**
  - build pass
  - make sure all existing tests pass
  - app does not crash
  - critic_agent verifies application stability and user requirements alignment
- **Duration:** 1m 15s

