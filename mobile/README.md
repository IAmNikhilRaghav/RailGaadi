# RailGaadi — Native Multiplatform App (Android & iOS)

Built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**.
100% Kotlin shared UI & logic across both operating systems.

---

## Architecture Overview

```
mobile/
├── composeApp/                     # 100% Shared Kotlin Multiplatform Codebase
│   └── src/
│       ├── commonMain/kotlin/      # Shared UI & Business Logic
│       │   └── com/railgaadi/app/
│       │       ├── App.kt          # Root Composable (Header, Tabs, Theme, Language Modal)
│       │       ├── model/          # Train, Station, LiveStatus, Language models
│       │       ├── theme/          # Material 3 Color & Theme palette
│       │       ├── i18n/           # 10-language dictionary & Indic runtime translator
│       │       └── ui/screens/     # LiveMap, Routes, Analytics, Weather, StationBoard
│       ├── androidMain/            # Android Entry Point (MainActivity, Manifest)
│       └── iosMain/                # iOS Entry Point (MainViewController)
├── iosApp/                         # Native Xcode Project (SwiftUI wrapper)
└── gradle/                         # Centralized Version Catalog (libs.versions.toml)
```

---

## 10 Supported Indian Languages with Native Script Badges

1. **English** (`EN`)
2. **Hindi** (`हिंद` - हिन्दी)
3. **Bengali** (`বাং` - বাংলা)
4. **Marathi** (`मरा` - मराठी)
5. **Telugu** (`తెలు` - తెలుగు)
6. **Tamil** (`தமி` - தமிழ்)
7. **Gujarati** (`ગુજ` - ગુજરાતી)
8. **Kannada** (`ಕನ್ನ` - ಕನ್ನಡ)
9. **Malayalam** (`മല` - മലയാളം)
10. **Punjabi** (`ਪੰਜ` - ਪੰਜਾਬੀ)

---

## How to Run & Build

### 1. Android
1. Open the `/Users/nikhil/TrainTracker/mobile` directory in **Android Studio**.
2. Select `composeApp` in the run configurations dropdown.
3. Choose an Android emulator or plugged-in Android device.
4. Click **Run (▶)**.

### 2. iOS
1. Open `/Users/nikhil/TrainTracker/mobile/iosApp/iosApp.xcodeproj` in **Xcode** (on your Mac).
2. Select any iPhone simulator (e.g. iPhone 16 Pro).
3. Click **Run (Cmd + R)**.

