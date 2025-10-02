# Firefly SMS (Android)

Small Android app that forwards HSBC SMS to your webhook (Google Apps Script) and lets you pick historical SMS to send.

## Build
- Open in Android Studio (JDK 17, Android Gradle Plugin 8.5).
- Or use GitHub Actions provided in `.github/workflows/android.yml`.

## Usage
- Enter Webhook URL and optional token.
- Toggle **Auto-forward** to send new HSBC SMS automatically.
- Tap Refresh to list recent SMS; select some and **Send Selected** to push.
