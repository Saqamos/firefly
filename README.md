# SMS Forwarder (Manual Only)

- Captures incoming SMS notifications (no SMS permission) via NotificationListenerService.
- Shows a list in the app; you pick messages and tap **Forward selected** to POST them to your webhook (Apps Script). 
- In-app **Settings** to edit the webhook URL/token.

## Build on GitHub

1. Create a new GitHub repo.
2. Upload/unzip this project (files visible; not a zip inside the repo).
3. Go to **Actions** tab → if asked, **Enable** workflows → run **Android CI**.
4. Download the artifact **`app-debug.apk`** and install it on your phone.

## First run

1. Open the app → menu **⋮ → Notification Access** → enable it for *SMS Forwarder*.
2. Menu **⋮ → Settings** → paste your webhook URL, 
3. Back to main; select messages → **Forward selected**.

## Payload

```json
{ "from": "<notification title>", "sms_id": "<sha256-8>", "message": "<full text>" }
```
