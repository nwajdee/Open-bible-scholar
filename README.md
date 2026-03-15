# 📖 OpenBible Scholar — v1.1
### Free • Offline • AI-Powered Bible Study for Android

---

## Overview

OpenBible Scholar is a **100% free, offline-first** Android Bible study app that delivers professional-grade study resources without any in-app purchases, subscriptions, or ads. It combines the offline simplicity of Olive Tree with the depth of Logos, enhanced with optional AI study tools and full Text-to-Speech accessibility.

---

## Features

### 📱 Bible Reader
- Verse-by-verse reader with clean serif typography
- Customizable font size (12sp–30sp), night mode, sepia mode
- Paragraph mode or verse-by-verse display
- Section headings, verse numbers (toggleable)
- Swipe left/right to navigate chapters
- Tap verse for quick actions (highlight, note, bookmark, AI, share)

### 🎨 Highlights & Notes
- 6 highlight colors: Yellow, Green, Blue, Pink, Purple, Orange
- Rich text notes per verse with tagging and cross-linking
- Bookmarks with custom labels
- Browse all highlights/notes/bookmarks in one place
- Export to Markdown

### 📚 Free SWORD Library
| Category | Modules |
|---|---|
| Bibles (Public Domain) | KJV, ASV, WEB, YLT, Darby, Webster, KJVA, LXX, SBLGNT, BHS |
| Commentaries | Matthew Henry (full + concise), JFB, Adam Clarke, Wesley |
| Dictionaries | Easton's, Smith's, ISBE |
| Lexicons | Strong's Greek & Hebrew |
| Church Fathers | Ante-Nicene Fathers (38 vols), Nicene & Post-Nicene Fathers |

### 🔊 Text-to-Speech (100% Offline)
- One-tap "Read Aloud" for verse, chapter, or continuous reading
- Word-by-word karaoke highlighting synchronized with audio
- Speed control: 0.5× – 2.0×
- Pitch control
- Voice selection (device-installed voices)
- Background playback with media notification controls
- Reads Bible text, commentaries, notes, and Church Fathers

### 🤖 AI Study Tools (Optional, Internet + Free API Key)
| Tool | Description |
|---|---|
| Contextual Explanation | Historical & cultural context, key word meanings, application |
| Cross References | 5-8 thematic and typological connections |
| Word Study | Deep Hebrew/Greek linguistic analysis |
| Historical Background | Archaeological and historical context |
| Passage Guide | Full study guide with structure, themes, application |
| Daily Devotional | Personal reflection + prayer prompt |
| Sermon Outline | Expository preaching outline |

**Free AI providers supported:**
- [Groq](https://console.groq.com) (LLaMA 3, Mixtral — very fast, free tier)
- [OpenRouter](https://openrouter.ai) (multiple free models)
- Ollama (local, for advanced users)
- Custom endpoint

### 📅 Reading Plans
- Bible in One Year (365 days)
- New Testament in 90 Days
- Psalms & Proverbs (150 days)
- The Four Gospels (30 days)
- Chronological Bible (365 days)
- Daily reminders via WorkManager

### 🔍 Search
- Full-text search across all downloaded Bible translations
- Highlighted search term in results
- Debounced live search (400ms)

### 📊 Parallel Bibles
- Compare up to 4 translations side by side
- Verse-aligned columns
- Add/remove translations dynamically

### 🛡️ Privacy
- **No account required** — no sign-up, no email
- **No analytics** — zero tracking of Bible reading habits
- **Encrypted API keys** — stored in Android Keystore (AES-256-GCM), never transmitted to us
- **Offline first** — all core features work without internet
- All data stays on device unless user chooses Google Drive backup

---

## Architecture

```
OpenBibleScholar/
├── app/
│   ├── src/main/
│   │   ├── java/com/openbiblescholar/
│   │   │   ├── data/
│   │   │   │   ├── db/           # Room database, DAOs, entities
│   │   │   │   ├── model/        # Domain models
│   │   │   │   └── repository/   # BibleRepository (single source of truth)
│   │   │   ├── di/               # Hilt modules, ApiKeyStore
│   │   │   ├── services/
│   │   │   │   ├── ai/           # AiStudyService (Groq/OpenRouter)
│   │   │   │   ├── tts/          # BibleTtsService, TTSForegroundService
│   │   │   │   └── sword/        # SwordModuleManager, ModuleDownloadService
│   │   │   └── ui/
│   │   │       ├── components/   # Reusable Compose components
│   │   │       ├── navigation/   # NavGraph with all routes
│   │   │       ├── screens/      # All screens + ViewModels
│   │   │       ├── theme/        # Material3 theme, typography, colors
│   │   │       └── widget/       # Daily verse home screen widget
│   │   └── res/
│   │       ├── values/           # strings.xml, colors.xml, themes.xml
│   │       ├── values-night/     # Dark theme override
│   │       └── xml/              # widget_info, backup_rules
│   ├── build.gradle              # Dependencies
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── gradle.properties
```

**Stack:**
- **UI:** Kotlin + Jetpack Compose + Material3
- **Architecture:** MVVM + Hilt DI
- **Storage:** Room (notes, highlights, settings) + File system (SWORD modules)
- **Networking:** OkHttp + Retrofit (AI only, optional)
- **TTS:** Android native `TextToSpeech` API with `UtteranceProgressListener`
- **Security:** `EncryptedSharedPreferences` with `MasterKey` (AES-256-GCM)
- **Background:** WorkManager (reading plan reminders)
- **Minimum SDK:** 26 (Android 8.0), **Target:** 34 (Android 14)

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Build & Run
```bash
git clone https://github.com/yourusername/OpenBibleScholar.git
cd OpenBibleScholar
./gradlew assembleDebug
```

### Configure AI (Optional)
1. Get a **free** API key from [Groq](https://console.groq.com/keys) or [OpenRouter](https://openrouter.ai/keys)
2. In the app: **Settings → AI Settings → Enter API Key**
3. Test the connection
4. Enable "AI Features" toggle in Settings

### Download Bible Modules
1. Open the app → **Library**
2. Select **Bibles** tab
3. Tap **Download** on KJV or any translation
4. Modules are downloaded over Wi-Fi and stored offline

---

## SWORD Module Integration

The app uses the [SWORD Project](https://www.crosswire.org/sword/) open standard for Bible modules. Modules are:
- **Free and public domain** (KJV, ASV, WEB, commentaries, etc.)
- Downloaded on-demand from CrossWire mirrors
- Stored as SQLite files in `files/sword_modules/`
- Parsed using a custom Kotlin SWORD reader

To add new SWORD modules, extend `SwordModuleManager.availableModules`.

---

## Roadmap

- [ ] v1.2: Passage Guide AI deep-dive, export to PDF, community module suggestions
- [ ] v1.3: Greek/Hebrew morphology browser, original language interlinear
- [ ] v2.0: Desktop/Web companion app, collaboration features, sermon builder
- [ ] Future: Multiple languages, iOS, offline LLM (when device hardware allows)

---

## License

This app is free and open source. The app code is released under the **MIT License**.

All Bible modules, commentaries, and lexicons used are in the **public domain** or are freely redistributable under open licenses (as noted per module).

The AI service integration is provided as-is. AI providers are third-party services — refer to their respective terms of service.

---

## Contributing

Pull requests welcome! Please see the roadmap above for priorities.

---

*"Your word is a lamp to my feet and a light to my path." — Psalm 119:105 (KJV)*
