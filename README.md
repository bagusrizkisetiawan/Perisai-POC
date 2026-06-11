# PERISAI POC — Mobile Live Streaming & Telemetry Ground Station

> An Android proof-of-concept that turns a phone into a field streaming unit: live RTMP video, real-time telemetry over WebSocket, two-way LiveKit audio/video, and a live tactical map — all behind QR-code authentication.

<p align="left">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Language" src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white">
  <img alt="UI" src="https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Min SDK" src="https://img.shields.io/badge/minSdk-24-green">
  <img alt="Architecture" src="https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-orange">
</p>

---

## 📖 Overview

**PERISAI POC** is the mobile client of a surveillance / streaming platform. Running on a phone in the field, it captures and broadcasts the camera feed while continuously transmitting device telemetry (orientation, battery, GPS) to a backend, and keeps a live two-way audio/video channel open with a control room. An operator sees everything in real time on a satellite map.

It is built as a **proof of concept (POC)** to validate the end-to-end pipeline: capture → stream → telemetry → realtime comms → map visualization.

> **Why "ground station"?** The main screen behaves like a remote-control / ground-control console: live video out, telemetry out, comms in/out, and a map with the unit's position and heading.

---

## ✨ Features

- 🔐 **QR-code authentication** — log in by scanning a QR (OTP), with secure token storage and automatic token refresh on `401`.
- 📹 **Live RTMP streaming** — broadcasts the phone camera to an RTMP server with selectable resolution/bitrate (powered by RootEncoder).
- 📡 **Real-time telemetry** — publishes orientation (pitch/roll/yaw), battery state, and GPS (lat/long/altitude) every 100 ms over a Centrifugo WebSocket channel.
- 🎙️ **Two-way comms** — in-app audio/video call with a control room via LiveKit, including participant list and mute controls.
- 🗺️ **Tactical map** — OpenStreetMap (OSMDroid) with Google satellite tiles, live device marker with heading, and server-driven overlays (pins, lines, areas, circles) fetched by map bounding box.
- ⚙️ **In-app configuration** — change backend, WebSocket, RTMP, and LiveKit URLs at runtime without rebuilding.
- 📍 **Dual location provider** — works on both Google-services and **Huawei HMS** devices.

---

## 🏗️ Architecture

The project follows a **Clean Architecture + MVVM** structure with a single consolidated network layer.

```
id.co.tigabersama.pochuaweistream
├── data
│   ├── local/            # SharedPreferences-based storage (settings, secure tokens)
│   ├── remote
│   │   ├── api/          # ApiConfig (Retrofit/OkHttp provider) + ApiService (single API surface)
│   │   ├── interceptor/  # AuthInterceptor (Bearer token)
│   │   ├── request/      # Request DTOs
│   │   └── response/     # Response DTOs
│   └── repository/       # AuthRepository
├── domain
│   └── model/            # Domain models (auth state, telemetry, battery)
├── realtime/             # Centrifugo WebSocket client
├── ui
│   ├── components/       # Reusable Composables (map, dialogs, scanner, status bar…)
│   ├── screen/           # Splash, Home, Settings screens
│   ├── theme/            # Compose theme
│   └── viewmodel/        # ViewModels (Auth, User, Livekit, Draw)
├── utils/                # Location helpers, map tiles
├── MainActivity.kt       # Navigation host (splash → home → settings)
└── RCScreenActivity.kt   # Main streaming / ground-station screen
```

**Key idea:** every REST endpoint lives in one place — [`data/remote/api/ApiService.kt`](app/src/main/java/id/co/tigabersama/pochuaweistream/data/remote/api/ApiService.kt) — and is created once by `ApiConfig`, so the whole app shares a single authenticated Retrofit client.

---

## 🛠️ Tech Stack

| Area | Technology |
|------|-----------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | Clean Architecture, MVVM, `StateFlow` |
| Networking | Retrofit 2 + OkHttp + Gson |
| Realtime telemetry | Centrifugo (`centrifuge-java`) over WebSocket |
| Live video call | LiveKit (`livekit-android` + compose components) |
| RTMP streaming | RootEncoder (`com.github.pedroSG94.RootEncoder`) |
| Maps | OSMDroid + Google satellite tiles |
| Location | Huawei HMS Location + Google Play Services Location |
| Camera / QR | CameraX, ZXing |
| Secure storage | AndroidX Security (`EncryptedSharedPreferences`) |
| Misc UI | Haze (blur), Core SplashScreen |
| Build | Gradle (AGP 8.13.2), compileSdk 36, minSdk 24 |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (Ladybug or newer recommended)
- JDK 11
- An Android device/emulator (Android 7.0 / API 24+)
- Access to the backend services (REST API, Centrifugo, RTMP, LiveKit)

### 1. Clone
```bash
git clone <your-repo-url>
cd "PERISAI POC"
```

### 2. Configure secrets
Create a `secrets.properties` file in the project root (it is **not** committed). These values are injected into `BuildConfig` at build time:

```properties
BASE_URL = https://your-api-host/
CENTRIFUGO_WEBSOCKET_URL = wss://your-centrifugo-host/connection/websocket
RTMP_URL = rtmp://your-rtmp-host/live
LIVEKIT_URL = wss://your-livekit-host
```

> All four URLs can also be changed later at runtime from the in-app **Settings** screen.

### 3. Build & run
```bash
./gradlew assembleDebug      # build the debug APK
# or open the project in Android Studio and press Run ▶
```

---

## 📱 How to Use

1. **Splash** → the app checks for an existing session.
2. **Home** → tap to open the **QR scanner** and scan your login QR (OTP). On success, tokens are stored securely.
3. Grant the requested **Camera, Microphone, and Location** permissions.
4. The **ground-station screen** opens:
   - Pick a resolution and **start the RTMP stream**.
   - Telemetry (orientation, battery, GPS) is streamed automatically once Centrifugo is connected.
   - Open the **call** panel for two-way LiveKit comms.
   - Open the **map** to see your position, heading, and server overlays.
5. **Settings** (from Home) → adjust API / WebSocket / RTMP / LiveKit URLs on the fly.

---

## 🔌 Backend Endpoints (consumed)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/v1/mobile/auth/login` | Log in with OTP |
| `POST` | `/v1/mobile/auth/refresh` | Refresh access token |
| `GET`  | `/v1/mobile/auth/me` | Current user / device info |
| `POST` | `/v1/mobile/auth/gentoken-centrifugo` | Get Centrifugo connection token |
| `GET`  | `/v1/draw` | Map overlays within a bounding box |
| `GET`  | `/v1/livekit/join` | LiveKit room token |
| `GET`  | `/v1/livekit/participant` | LiveKit participants |

---

## 📝 Notes & Limitations

- This is a **proof of concept**, not a production release.
- The app uses `usesCleartextTraffic` for flexibility during development.
- Two bundled third-party native libraries (CameraX `camera-core`, Huawei HMS `core`) are not yet aligned for **16 KB memory pages** required by Google Play for Android 15+ submissions. This does not affect normal devices and is fixed by upgrading those dependencies.

---

## 👤 Author

**Bagus Rizki Setiawan**
Built as a portfolio project demonstrating Clean Architecture, Jetpack Compose, real-time streaming, and multi-protocol integration (RTMP / WebSocket / WebRTC) on Android.

---

<sub>README available in English. Need a Bahasa Indonesia version? Just ask. 🇮🇩</sub>
