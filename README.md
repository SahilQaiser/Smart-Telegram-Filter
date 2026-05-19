# Smart Filter — Telegram Channel Monitor

A native Android app that runs a persistent background service connected to the Telegram API (via TDLib) and surfaces only the messages you actually care about. Configure keyword/regex filters per channel, receive rich notifications, and browse a clean feed — all without a server.

---

## Platform

| | |
|---|---|
| **OS** | Android 7.0 Nougat and above (minSdk 24) |
| **Target SDK** | 35 (Android 15) |
| **Architecture** | arm64-v8a · armeabi-v7a · x86\_64 · x86 |
| **Language** | Kotlin |

---

## Problem Statement

Telegram channels are high-volume. A single channel can push hundreds of messages per day, burying the few posts that are actually relevant to you. The native Telegram app has no keyword filtering, no feed view, and no per-channel notification rules. Power users who monitor trading signals, job boards, news feeds, or community channels are left either drowning in noise or muting everything and missing what matters.

---

## Solution

Smart Filter connects to your Telegram account in the background using TDLib — the official Telegram client library — and evaluates every incoming message from your selected channels against your filter rules. When a message matches, it:

- Saves the message to a local database
- Fires a rich notification (with a one-tap "Open in Telegram" deeplink)
- Adds the message to the in-app feed

Your own account, your own device, no server, no subscription.

---

## Features

### Filtering
- **Keyword filters** — plain-text match (case-insensitive) across message text
- **Regex filters** — prefix a keyword with `r/` to use a full regular expression (e.g. `r/\b(buy|sell)\b`)
- **Per-channel scope** — each filter is tied to a specific channel; browsing your subscribed channels (including private ones) is built in
- **Match count badge** — the Filters screen shows how many messages each filter has matched

### Feed
- **Unified message feed** — all matched messages in reverse-chronological order
- **Full-text search** — searches across message content, channel name, keyword, and sender name simultaneously
- **Channel chips** — tap a chip to narrow the feed to one channel; "All" resets
- **Starred messages** — tap the star icon on any card to pin it; toggle "starred only" mode from the TopAppBar
- **Swipe-to-archive** — swipe a message card left or right to archive it
- **Archive sheet** — bottom sheet showing archived messages, with per-message restore and clear-all actions
- **Share** — share message text directly from the card

### Notifications
- **Rich notifications** — BigText style with channel name, matched keyword in sub-text, and full message body
- **Open in Telegram** — notification action deeplinks directly to the source post (public channels via `t.me`, private channels via `tg://privatepost`)
- **Quiet hours** — suppress all filter notifications between configurable start/end hours (supports midnight wrap-around)

### Navigation
- **Filters screen** — create, edit, and delete filter rules; browse and add channels
- **Channel picker** — loads your Telegram subscriptions including private channels; tap to add to a filter
- **Settings screen** — configure quiet hours with hour-resolution sliders

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose |
| DI | Hilt |
| Local DB | Room (v4) |
| Preferences | DataStore Preferences |
| Async | Kotlin Coroutines + Flow |
| Telegram API | TDLib (via libtdjni.so + tdlib.jar) |
| Notifications | NotificationCompat + NotificationChannel |

---

## Project Setup

### Prerequisites

| Tool | Notes |
|---|---|
| Android Studio Hedgehog or newer | Includes a bundled JBR that the setup script can auto-detect |
| JDK 8+ (`javac` and `jar`) | Must be on PATH **or** Android Studio's JBR will be used automatically |
| Docker Desktop | Must be running before you execute the TDLib setup script |
| Git | Used by the setup script to clone the TDLib source |

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/SmartTelegramFilter.git
cd SmartTelegramFilter
```

### 2. Obtain Telegram API credentials

1. Go to [https://my.telegram.org/apps](https://my.telegram.org/apps) and sign in
2. Create a new application (any name/platform)
3. Note your **App api\_id** and **App api\_hash**

### 3. Add credentials to `local.properties`

Create or open `local.properties` at the project root and add:

```properties
TELEGRAM_API_ID=<your_api_id>
TELEGRAM_API_HASH=<your_api_hash>
```

> `local.properties` is git-ignored. Never commit this file.

### 4. Build TDLib

TDLib's native binaries are not included in the repository (they are ~100 MB binary blobs). A setup script automates the entire build:

**Windows (PowerShell — run from the project root):**

```powershell
.\scripts\setup_tdlib.ps1
```

**Linux / macOS:**

```bash
chmod +x scripts/setup_tdlib.sh
./scripts/setup_tdlib.sh
```

The script will:
1. Check prerequisites (Docker, Git, JDK)
2. Sparse-clone the official `tdlib/td` repository (Android example only)
3. Run a Docker build that cross-compiles TDLib for all four ABIs — **this takes 20–40 minutes on the first run**; subsequent runs reuse the Docker layer cache and are much faster
4. Extract the build output and install:
   - `app/libs/tdlib.jar` — Java bindings
   - `app/src/main/jniLibs/<ABI>/libtdjni.so` — native libraries (arm64-v8a, armeabi-v7a, x86\_64, x86)

> **Docker must be running** before you start the script. The build requires a few gigabytes of disk space for intermediate layers.

### 5. Open in Android Studio and build

1. Open the project root in Android Studio
2. Let Gradle sync complete
3. Select a device or emulator (API 24+)
4. Run → **app** or `./gradlew assembleDebug`

---

## First-Time Use

1. Launch the app — you will see the **Auth screen**
2. Enter your Telegram phone number (international format, e.g. `+1234567890`)
3. Enter the verification code sent to your Telegram account
4. If you have two-step verification enabled, enter your cloud password
5. Once authenticated, the app navigates to the **Feed screen** and starts the background service automatically

---

## Architecture Overview

```
MainActivity
├── AuthScreen          ← TDLib auth flow (phone → code → 2FA password)
├── FeedScreen          ← Matched message feed with search + filters
│   └── FAB             ← FiltersScreen navigation with unread badge
├── FiltersScreen       ← Create / edit keyword filters per channel
│   └── ChannelPickerScreen
└── SettingsScreen      ← Quiet hours configuration

TelegramService (Foreground Service)
└── TDLib client        ← Persistent connection; evaluates incoming messages
    └── NotificationHelper  ← Fires notifications for matches

Room Database
├── FilterEntry         ← User-defined keyword/regex rules + channel binding
└── MatchedMessage      ← Persisted matching messages (starred, archived flags)

DataStore Preferences   ← Quiet hours settings (enabled, start hour, end hour)
```

---

## Known Limitations

- **One account only** — TDLib is initialised with a single phone number; multi-account support is not implemented
- **Channels only** — the filter service monitors channels (broadcasts); group chats and direct messages are not scanned
- **TDLib is not in git** — the native library must be built locally via the setup script (see Step 4 above)

---

## Contributing

Pull requests are welcome. For major changes please open an issue first to discuss what you would like to change.

---

## License

This project is for personal use. TDLib is licensed under the [Boost Software License 1.0](https://www.boost.org/LICENSE_1_0.txt).
