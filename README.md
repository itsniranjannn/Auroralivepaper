
<p align="center">
  <img src="docs/assets/aurora_banner.svg" width="100%" alt="Aurora Live Paper Banner" />
</p>

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-7F2CCB?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-2C666E?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-90DDF0?style=for-the-badge&logo=jetpackcompose&logoColor=0A090C)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-97D8B2?style=for-the-badge&logo=opensourceinitiative&logoColor=0A090C)](LICENSE)

<br/>

<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=20&duration=3000&pause=1000&color=90DDF0&center=true&vCenter=true&multiline=true&repeat=true&width=620&height=65&lines=Ultra-smooth+Video%2C+GIF%2C+and+Image+Wallpapers.;Zero+Battery+Drain+When+Off-Screen+or+Locked.;Engineered+for+Android+Home+%26+Lock+Screens." alt="Typing SVG Animation" />
</p>

---

</div>

## 📱 Visual Showcase & Design System

<div align="center">
  <table>
    <tr>
      <td align="center" width="50%">
        <b>✨ Welcome & Onboarding Experience</b>
        <br/><br/>
        <img src="docs/assets/aurora_splash.png" width="310" alt="Aurora Splash Onboarding" style="border-radius: 18px; box-shadow: 0px 8px 30px rgba(127, 44, 203, 0.45);" />
        <br/>
        <sub><i>Cosmic gradient intro with fluid wave motion</i></sub>
      </td>
      <td align="center" width="50%">
        <b>🎛️ Wallpaper Dashboard & Media Hub</b>
        <br/><br/>
        <img src="docs/assets/aurora_home.png" width="310" alt="Aurora Dashboard UI" style="border-radius: 18px; box-shadow: 0px 8px 30px rgba(44, 102, 110, 0.45);" />
        <br/>
        <sub><i>Quick video/GIF/image picker with live playback manager</i></sub>
      </td>
    </tr>
  </table>
</div>

---

## ⚡ Key Highlights

<table>
  <tr>
    <td width="50%">
      <h3 align="center">🎬 Universal Media Engine</h3>
      <ul>
        <li><b>High-Definition Video Playback:</b> Ultra-smooth playback powered by Android <code>MediaPlayer</code> & Media3 ExoPlayer.</li>
        <li><b>Zero-Lag GIF Support:</b> Native <code>Movie</code> streaming decoder optimized for dynamic animated loop assets.</li>
        <li><b>Dynamic Scaling (Fit / Fill):</b> Live aspect ratio adjustments without interrupting video playback or rebuilding player instances.</li>
      </ul>
    </td>
    <td width="50%">
      <h3 align="center">⚡ Intelligent Power Guard</h3>
      <ul>
        <li><b>Zero Background Overhead:</b> Hardware-level pause when screen turns off or launcher is obscured.</li>
        <li><b>Battery Saver Synced:</b> Automatic rendering freeze when system Battery Saver is engaged.</li>
        <li><b>Low Battery Protection:</b> Customizable low-battery threshold freeze to prevent battery drain.</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3 align="center">🔒 Native Lock Screen Support</h3>
      <ul>
        <li><b>Simultaneous Setup:</b> Apply seamlessly to <i>Home Screen</i>, <i>Lock Screen</i>, or <i>Both</i>.</li>
        <li><b>Audio Isolation Guard:</b> Audio automatically mutes while the keyguard is active and only resumes upon secure unlock.</li>
        <li><b>Modern API 34+ Aware:</b> Tailored detection for Android 14+ lock screen wallpaper managers and Samsung One UI.</li>
      </ul>
    </td>
    <td width="50%">
      <h3 align="center">🛡️ Resilient Recovery & Safety</h3>
      <ul>
        <li><b>Autonomous Recovery:</b> Exponential backoff ladder (1s, 2s, 4s, 8s, 30s) prevents crash loops.</li>
        <li><b>Sliding-Window Limiter:</b> Rate-limits consecutive recovery cycles to protect OS stability.</li>
        <li><b>Async Daemon Logger:</b> Non-blocking background log pipeline with zero main thread disk I/O.</li>
      </ul>
    </td>
  </tr>
</table>

---

## 🎨 Cosmic Color Palette

Aurora Live Paper's design language draws inspiration from polar auroras dancing across dark night skies:

| Swatch | Color Name | Hex Token | Material Role |
| :---: | :--- | :---: | :--- |
| ![#0A090C](https://placehold.co/24x24/0A090C/0A090C.png) | **Aurora Night** | `#0A090C` | Root Background |
| ![#2A2D43](https://placehold.co/24x24/2A2D43/2A2D43.png) | **Surface Indigo** | `#2A2D43` | Cards & Elevated Surfaces |
| ![#7F2CCB](https://placehold.co/24x24/7F2CCB/7F2CCB.png) | **Royal Violet** | `#7F2CCB` | Primary Accent & Highlights |
| ![#2C666E](https://placehold.co/24x24/2C666E/2C666E.png) | **Stormy Teal** | `#2C666E` | Secondary Surface & Toggles |
| ![#90DDF0](https://placehold.co/24x24/90DDF0/90DDF0.png) | **Frosted Blue** | `#90DDF0` | Tertiary & Active Indicators |
| ![#97D8B2](https://placehold.co/24x24/97D8B2/97D8B2.png) | **Aurora Glow** | `#97D8B2` | Status Highlights & Badges |

---

## 🏗️ Architecture & Core Components

```
com.niranjan.auroralivepaper/
├── AuroraWallpaperService.kt      # Core WallpaperService engine (lifecycle, scaling, playback)
├── MainActivity.kt                # Jetpack Compose single-activity coordinator & navigation
├── data/
│   ├── AuroraDebugLogger.kt       # Daemon-thread async rotating log system (200 KB cap)
│   └── SettingsPrefs.kt           # SharedPreferences layer for audio, power & scaling prefs
├── ui/
│   ├── picker/                    # WallpaperPickerScreen (media import, gallery, safe staging)
│   ├── preview/                   # PreviewScreen (ExoPlayer video/GIF preview with live aspect toggle)
│   ├── settings/                  # WallpaperSettingsScreen (battery threshold, audio volume, logs)
│   └── theme/                     # Aurora dark design system (Color, Theme, Typography)
└── util/
    ├── MediaValidator.kt          # Pre-flight asset dimension, FPS & size sanity checks
    ├── RecoveryLimiter.kt         # Sliding-window rate limiter with exponential backoff sequence
    └── WebVideoExtractor.kt       # Direct CDN video extraction & staging pipeline
```

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio Ladybug (or newer)**
* **JDK 21 / 25** (Android Studio Bundled JBR recommended)
* **Android SDK**: `compileSdk 35`, `minSdk 26` (Android 8.0 Oreo+)

### Build & Run

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/aurora-live-paper.git
   cd aurora-live-paper
   ```

2. **Run Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. **Assemble Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Assemble Signed Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```
   *The optimized, minified APK will be generated at:*  
   `app/build/outputs/apk/release/app-release.apk`

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for full details.

```text
MIT License
Copyright (c) 2026 Niranjan.K

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so...
```

---

## 🔒 Security & Privacy

* **Zero Tracking:** No analytics libraries, third-party trackers, or background telemetry.
* **Storage Isolation:** Media assets are staged securely within internal application storage (`filesDir`), preventing tampering or accidental external deletion.
* **Hardware Mute Protection:** Audio stream is hardware-muted whenever the screen turns off or lock screen wakes up, ensuring no accidental sound leakages occur in public spaces.

---

<div align="center">

---

Crafted with 💜 for a smoother, more vibrant Android experience.

</div>



