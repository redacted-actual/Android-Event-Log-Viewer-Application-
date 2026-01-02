Android Event Log Viewer

Intuitive on-device access to Android system logs
A simple, beautiful application that lets any Android user view, search, and understand their device's event logs, no computer or command line required. Whether you're curious about what's happening behind the scenes, troubleshooting an app, or just exploring how your phone works, this viewer makes system logs readable and approachable. Why This App? Android devices generate thousands of internal "event logs" every minute (app activity, system messages, errors, battery events, etc.).
Normally, these are hidden behind technical tools like adb logcat, requiring a computer, cables, and commands.

This app brings those logs directly to your phone with a clean, intuitive interface anyone can use.

FeaturesLive log streaming — Watch events happen in real time

Easy search & filtering — Find specific apps, messages, or error types instantly

Color-coded severity levels — Errors in red, warnings in yellow, info in blue — spot issues at a glance

Material 3 design — Modern, dark/light theme, smooth scrolling

No root required — Works on standard Android devices (with one-time permission setup)

Permission Note: To read system logs, Android requires the READ_LOGS permission.
This can be granted easily via a one-time ADB command (no root needed):  

adb shell pm grant com.redacted.androidlogviewer android.permission.READ_LOGS

For Developers & Advanced UsersFilter by PID, tag, or log level
Pause/resume streaming
Copy or share log snippets
Great companion for testing your own apps on-device

Quick Setup (For Testing)

git clone https://github.com/redacted-actual/Android-Event-Log-Viewer-Application-.git
cd Android-Event-Log-Viewer-Application-
# Open in Android Studio
# Run on device or emulator
# Grant READ_LOGS permission via ADB (see note above)

- Redacted 
