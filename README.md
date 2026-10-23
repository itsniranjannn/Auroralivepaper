<div align="center">

# 🌌 Aurora Live Paper

### *Bring Your Screens to Life with Pure Motion & Cosmic Brilliance*

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-7F2CCB?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-2C666E?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-90DDF0?style=for-the-badge&logo=jetpackcompose&logoColor=0A090C)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-97D8B2?style=for-the-badge&logo=apache&logoColor=0A090C)](LICENSE)

<br/>

```
  ✦   .       .   *    .      .   ✦   .       *   .      .   *
       .   *   ═══════════════════════════════════   *   .
               A U R O R A   L I V E   P A P E R
       *   .   ═══════════════════════════════════   .   *
  *       .   ✦       *   .       .   ✦   .       *       .   ✦
```

<p align="center">
  <b>Aurora Live Paper</b> is a high-performance Android live wallpaper engine engineered for fluid, high-frame-rate video, animated GIF, and image rendering on both Home and Lock screens without compromising battery life or device responsiveness.
</p>

---

</div>

## ✨ Key Highlights

<table>
  <tr>
    <td width="50%">
      <h3 align="center">🎬 Universal Media Engine</h3>
      <ul>
        <li><b>High-Definition Video Playback:</b> Ultra-smooth playback powered by Android <code>MediaPlayer</code> & Media3 ExoPlayer.</li>
        <li><b>Zero-Lag GIF Support:</b> Native <code>Movie</code> streaming decoder optimized for dynamic animated loop assets.</li>
        <li><b>High-Res Static Imagery:</b> Multi-aspect ratio scaling (<i>Fit</i> vs. <i>Fill</i>) with memory-safe sub-sampling.</li>
      </ul>
    </td>
    <td width="50%">
      <h3 align="center">⚡ Intelligent Power Guard</h3>
      <ul>
        <li><b>Zero Background Overhead:</b> Immediate rendering suspension when screen turns off or launcher is obscured.</li>
        <li><b>Battery Saver Synced:</b> Automatic hardware-level freeze when system Battery Saver engages.</li>
        <li><b>Low Battery Protection:</b> Customizable low-battery threshold freeze to prevent battery drain.</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3 align="center">🔒 Native Lock Screen Support</h3>
      <ul>
        <li><b>One-Tap Simultaneous Setup:</b> Apply seamlessly to <i>Home Screen</i>, <i>Lock Screen</i>, or <i>Both</i>.</li>
        <li><b>Audio Isolation Guard:</b> Audio automatically mutes while the keyguard is active and only resumes upon secure unlock.</li>
        <li><b>Modern API 34+ Aware:</b> Tailored detection for Android 14+ lock screen wallpaper managers and Samsung One UI.</li>
      </ul>
    </td>
    <td width="50%">
      <h3 align="center">🛡️ Resilient Recovery & Safety</h3>
      <ul>
        <li><b>Autonomous Player Recovery:</b> Exponential backoff ladder (1s, 2s, 4s, 8s, 30s) prevents looping crashes.</li>
        <li><b>Sliding-Window Rate Limiter:</b> Throttles consecutive recovery loops to preserve OS stability.</li>
        <li><b>Background Diagnostic Logger:</b> Asynchronous daemon log pipeline with zero UI thread blocking.</li>
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

## 🔒 Security & Privacy

* **Zero Tracking:** No analytics libraries, third-party trackers, or background telemetry.
* **Storage Isolation:** Media assets are staged securely within internal application storage (`filesDir`), preventing tampering or accidental external deletion.
* **Hardware Mute Protection:** Audio stream is hardware-muted whenever the screen turns off or lock screen wakes up, ensuring no accidental sound leakages occur in public spaces.

---

<div align="center">

Crafted with 💜 for a smoother, more vibrant Android experience.

</div>
