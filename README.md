<div align="center">
  <img src="app/src/main/res/drawable/ic_shield_emblem.jpg" alt="ShieldCore Logo" width="110" height="110" />
  <h1>ShieldCore — Android Security &amp; Cleanup Suite</h1>
  <p>
    <strong>আধুনিক অ্যান্ড্রয়েড ভাইরাস স্ক্যানার, রিয়েল-টাইম মেমোরি ও ব্যাকগ্রাউন্ড শিল্ড এবং স্টোরেজ ক্লিনআপ ড্যাশবোর্ড</strong><br />
    Founded by <strong>শেখ ফরিদ (Sheikh Farid)</strong> •
    <a href="https://github.com/rsk934128-dot/ShieldCore">github.com/rsk934128-dot/ShieldCore</a>
  </p>

  <p>
    <a href="https://github.com/rsk934128-dot/ShieldCore"><img src="https://img.shields.io/badge/GitHub-rsk934128--dot%2FShieldCore-181717?style=for-the-badge&logo=github" alt="GitHub Repo" /></a>
    <a href="https://github.com/rsk934128-dot/ShieldCore/releases"><img src="https://img.shields.io/badge/APK-GitHub%20Releases-00E5A3?style=for-the-badge&logo=android&logoColor=090E17" alt="GitHub Releases" /></a>
    <img src="https://img.shields.io/badge/Language-Kotlin%202.2-38BDF8?style=for-the-badge&logo=kotlin&logoColor=090E17" alt="Kotlin 2.2" />
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-00E5A3?style=for-the-badge&logo=jetpackcompose&logoColor=090E17" alt="Jetpack Compose M3" />
    <img src="https://img.shields.io/badge/WorkManager-Battery%20Safe-F59E0B?style=for-the-badge" alt="WorkManager" />
  </p>
</div>

<br />

<p align="center">
  <img src="docs/images/hero_banner.jpg" alt="ShieldCore Cybersecurity Banner" width="100%" />
</p>

---

## পরিচিতি ও প্রজেক্ট তথ্য (Overview & Attribution)

- **প্রজেক্টের নাম (Project):** ShieldCore
- **প্রতিষ্ঠাতা ও নির্মাতা (Founder & Creator):** **শেখ ফরিদ (Sheikh Farid)**
- **অফিশিয়াল GitHub রিপোজিটরি:** [https://github.com/rsk934128-dot/ShieldCore](https://github.com/rsk934128-dot/ShieldCore)
- **APK ডাউনলোড (Releases):** [https://github.com/rsk934128-dot/ShieldCore/releases](https://github.com/rsk934128-dot/ShieldCore/releases)

**ShieldCore** হলো একটি প্রাইভেসি-ফার্স্ট ও Google Play Policy-সম্মত অ্যান্ড্রয়েড সিকিউরিটি, রিয়েল-টাইম RAM মনিটরিং এবং স্টোরেজ অপ্টিমাইজেশন স্যুইট। এটি `java.io.File` ডিরেক্টরি স্ক্যানিং, **Storage Access Framework (SAF)**, **Room** ভিত্তিক লোকাল ম্যালওয়্যার সিগনেচার ডেটাবেস (`SHA-256` ও `MD5`), `PackageManager` অ্যাপ পারমিশন অডিটিং, `ActivityManager.MemoryInfo` লাইভ RAM ড্যাশবোর্ড এবং **WorkManager** ভিত্তিক লো-ব্যাটারি সেফ ব্যাকগ্রাউন্ড অটো-স্ক্যানকে একত্রিত করে একটি রেসপন্সিভ **Cyber-Vault** ইন্টারফেসে উপস্থাপন করে।

---

## মূল ফিচারসমূহ (Core Security Pillars)

<p align="center">
  <img src="docs/images/security_pillars.jpg" alt="ShieldCore Four Security Pillars" width="100%" />
</p>

| মডিউল (Module) | বিবরণ (Description) | টেকনোলজি (Implementation) |
| :--- | :--- | :--- |
| **Deep Scan Engine** | রিকার্সিভ ফাইল-সিস্টেম স্ক্যানিং, সিঙ্গেল-পাস ডুয়াল হ্যাশিং (`SHA-256` + `MD5`), সাইকেল/সিমলিংক প্রোটেকশন এবং ডাবল-এক্সটেনশন হিউরিস্টিক চেক | `CoreFileScanner`, `java.io.File`, `MessageDigest`, SAF (`OpenDocumentTree` / `OpenDocument`) |
| **WorkManager Background Shield** | নির্ধারিত সময়সূচি (`6h` / `12h` / `24h`) ও অন-ডিমান্ড ব্যাকগ্রাউন্ড অটো-স্ক্যান, থ্রেট নোটিফিকেশন এবং **Low-Battery Safety Mode** (`<20%` বা Power Saver-এ অটো-পজ) | `WorkManager`, `CoroutineWorker`, `BatterySafetyInspector`, `RequiresBatteryNotLow` |
| **Live RAM Usage Dashboard** | রিয়েল-টাইম RAM ব্যবহারের শতাংশ, অ্যানিমেটেড গেজ এবং ওয়ান-ট্যাপ **Clear RAM** ক্যাশ ও মেমোরি ট্রিম | `ActivityManager.MemoryInfo`, Compose `Canvas`, `Dispatchers.IO` |
| **Junk Cleaner** | ডিভাইসের আসল ফ্রি/ব্যবহৃত স্টোরেজ পরিমাপ ও অ্যাপ ক্যাশ, কোড ক্যাশ এবং ডায়াগনস্টিক লগ পরিষ্কার | `StatFs`, `cacheDir`, `codeCacheDir`, `externalCacheDir` |
| **App Permission Auditor** | ইনস্টল থাকা লঞ্চেবল অ্যাপগুলোর সংবেদনশীল পারমিশন (Camera, Mic, Location, SMS) বিশ্লেষণ ও রিস্ক লেভেল নির্ণয় | `PackageManager`, Play-compliant `<queries>` filter |
| **Threat Signature Vault** | লোকাল ম্যালওয়্যার হ্যাশ ইনডেক্স ও স্ক্যান হিস্টোরি সংরক্ষণ, পাশাপাশি ফ্ল্যাগড ফাইল ডিলিট/রিমুভাল | `Room Database` (`MalwareSignatureEntity`, `ScanHistoryEntity`), `DataStore` |

---

## উন্নয়ন রোডম্যাপ (ShieldCore Roadmap)

### Phase 1 — Core Android Security & Optimization Foundation (Completed)
- [x] **Core File Scanner (Kotlin):** রিকার্সিভ `java.io.File` ট্রাভার্সাল, সিমলিংক/সাইকেল সুরক্ষা, সিঙ্গেল-পাস `SHA-256` + `MD5` হ্যাশিং এবং হিউরিস্টিক ডিটেকশন।
- [x] **Room Signature Vault & Scan History:** অফলাইন থ্রেট ইনডেক্স ও স্ক্যান রিপোর্ট সংরক্ষণ।
- [x] **Play-Compliant SAF & App Auditor:** Broad storage permission ছাড়াই ফোল্ডার/APK স্ক্যান এবং `<queries>` ভিত্তিক পারমিশন অডিট।
- [x] **Live RAM Usage Dashboard:** `ActivityManager.MemoryInfo` ভিত্তিক রিয়েল-টাইম RAM গেজ এবং `Clear RAM` অপ্টিমাইজেশন।
- [x] **WorkManager Background Shield:** অটো-স্ক্যান শিডিউলিং ও **Low-Battery Safety Mode** (`<20%` / Power Saver সুরক্ষা)।
- [x] **Bilingual Cyber-Vault UI & CI/CD:** বাংলা/ইংরেজি ভাষা পরিবর্তন, ডার্ক/লাইট থিম এবং GitHub Actions থেকে অটোমেটেড APK রিলিজ।

### Phase 2 — Modular Micro-Engine & Native C++/JNI Acceleration (Planned)
- [ ] **Native C++/JNI Scanning Engine:** অতি দ্রুতগতির ফাইল বাইনারি স্ট্রিমিং ও মেমোরি প্যাটার্ন ম্যাচিংয়ের জন্য C++ নেটিভ কোর মডিউল।
- [ ] **Extended Heuristic & YARA-Style Rules:** বাইনারি হেডার, এনক্রিপ্টেড পেলোড এবং সন্দেহজনক ম্যানিফেস্ট অ্যানোমালি শনাক্তকরণ।
- [ ] **Encrypted Quarantine Vault:** শনাক্তকৃত ফাইলগুলো সরাসরি ডিলিট না করে আইসোলেটেড ও এনক্রিপ্টেড কোয়ারেন্টাইন বক্সে সংরক্ষণের সুবিধা।

### Phase 3 — Cloud Threat Intelligence & Cross-Platform Ecosystem (Vision)
- [ ] **Incremental Signature Updates:** ভেরিফায়েড ও সাইনড থ্রেট ফিডের মাধ্যমে লাইটওয়েট সিগনেচার সিঙ্ক।
- [ ] **Privacy Score & Weekly Digest:** সাপ্তাহিক নিরাপত্তা সারসংক্ষেপ ও অ্যাপ পারমিশন পরিবর্তনের রিয়েল-টাইম অ্যালার্ট।
- [ ] **Linux & Cross-Platform Core:** ShieldCore-এর একই হ্যাশিং ও হিউরিস্টিক ইঞ্জিনকে Linux CLI টুলের সাথে সমন্বিত করা।

---

## ড্যাশবোর্ড ও ইউজার এক্সপেরিয়েন্স (Responsive UI & UX)

- **অ্যাডাপ্টিভ ও রেসপন্সিভ ডিজাইন (`BoxWithConstraints`)**:
  - **Compact (Phone Portrait)**: সিঙ্গেল-কলাম ড্যাশবোর্ড + কাস্টম অ্যানিমেটেড `SecurityScoreRing` (`0–100`) + **WorkManager Shield Card** + **Live RAM Dashboard** + নিচের **Material 3 NavigationBar**।
  - **Medium / Expanded (Tablet, Foldable, Landscape ≥ 680dp)**: বাম পাশে **NavigationRail** সহ **Two-Pane** ক্যাননিক্যাল ড্যাশবোর্ড লেআউট।
- **Bilingual Support (`EN` / `বাংলা`)**: টপ অ্যাপ বার থেকে এক ট্যাপেই ইংরেজি এবং বাংলা ভাষায় সুইচ করার সুবিধা।
- **Dark & Light Theme**: Cyber-Vault **Obsidian Emerald** ডার্ক থিম এবং ক্লিন **Ice-Slate** লাইট থিম, **Space Grotesk**, **Plus Jakarta Sans** এবং **JetBrains Mono** ফন্টসহ।

---

## প্রজেক্ট আর্কিটেকচার (Architecture)

```text
com.example
├── data
│   ├── engine
│   │   ├── CoreFileScanner.kt          # Recursive java.io.File traversal, SHA-256/MD5 & heuristics
│   │   └── DeviceSecurityEngine.kt     # SAF document scanning, StatFs storage, PackageManager audit
│   ├── local
│   │   ├── MalwareSignatureEntity.kt   # Room entity storing SHA-256 & MD5 threat indicators
│   │   ├── ScanHistoryEntity.kt        # Room entity storing scan audit logs
│   │   ├── SecurityDao.kt              # Reactive Flow & suspend hash lookup queries
│   │   └── SecurityDatabase.kt         # Room database singleton with default signatures
│   ├── model
│   │   └── SecurityModels.kt           # Domain models for files, apps, junk categories & storage
│   ├── preferences
│   │   └── SecurityPreferences.kt      # Jetpack DataStore for shield, battery safety, theme & language
│   ├── repository
│   │   └── SecurityRepository.kt       # Unified repository layer
│   └── worker
│       ├── BackgroundShieldScheduler.kt # WorkManager periodic & immediate auto-scan scheduler
│       ├── BackgroundShieldWorker.kt    # CoroutineWorker with low-battery check & alert notifications
│       └── BatterySafetyInspector.kt    # Real battery level, charging & Power Saver state evaluator
└── ui
    ├── components
    │   ├── SecurityScoreRing.kt        # Custom Canvas circular radar & animated score gauge
    │   ├── RamUsageDashboard.kt        # ActivityManager.MemoryInfo live RAM monitor & Clear RAM CTA
    │   └── FeatureModuleCards.kt       # Interactive M3 module cards
    ├── screens
    │   ├── DashboardScreen.kt          # Responsive single-column / two-pane dashboard
    │   ├── DeepScanScreen.kt           # File/Folder scanner, hash log & threat removal
    │   ├── JunkCleanerScreen.kt        # RAM monitor, storage meter & cache cleaner
    │   └── AppManagerScreen.kt         # App permission audit & system settings deep-link
    ├── theme                           # Color.kt, Type.kt, Theme.kt
    ├── i18n/AppStrings.kt              # English & Bengali localization
    ├── SecurityViewModel.kt            # StateFlow state management + WorkManager sync
    └── ShieldCoreApp.kt                # Adaptive NavigationBar / NavigationRail host
```

---

## Google Play প্রাইভেসি ও পলিসি কমপ্লায়েন্স

- **কোনো Broad Storage Permission নেই**: `MANAGE_EXTERNAL_STORAGE` বা `READ_EXTERNAL_STORAGE` ব্যবহার না করে শূন্য-পারমিশনের **Storage Access Framework (SAF)** ব্যবহার করা হয়েছে।
- **কোনো `QUERY_ALL_PACKAGES` নেই**: `AndroidManifest.xml`-এ নির্দিষ্ট `<queries>` (`ACTION_MAIN` + `CATEGORY_LAUNCHER`) ফিল্টার ব্যবহার করে কেবল লঞ্চেবল অ্যাপগুলোর পারমিশন অডিট করা হয়।
- **ব্যাটারি-বান্ধব ব্যাকগ্রাউন্ড এক্সিকিউশন**: অননুমোদিত পারসিস্টেন্ট ফোরগ্রাউন্ড সার্ভিসের বদলে Android **WorkManager** এবং **Low-Battery Safety Mode** ব্যবহার করা হয়েছে; শুধুমাত্র থ্রেট অ্যালার্টের জন্য রানটাইম `POST_NOTIFICATIONS` পারমিশন অনুরোধ করা হয়।

---

## বিল্ড ও টেস্ট (Build & Verification)

```bash
# প্রজেক্ট কম্পাইল ও APK বিল্ড করতে
gradle :app:assembleDebug

# ইউনিট এবং Robolectric UI টেস্ট চালাতে
gradle :app:testDebugUnitTest
```

### অন্তর্ভুক্ত টেস্ট স্যুট:
- `CoreFileScannerTest`: ডিরেক্টরি ট্রি রিকার্সিভ স্ক্যান, EICAR `SHA-256` / `MD5` সিগনেচার ডিটেকশন, ডাবল-এক্সটেনশন হিউরিস্টিক এবং লোকাল থ্রেট ডিলিশন টেস্ট।
- `BackgroundShieldWorkerTest`: `BatterySafetyInspector` লো-ব্যাটারি/পাওয়ার-সেভার লজিক, `WorkManager` পিরিয়ডিক কনস্ট্রেইন্ট (`RequiresBatteryNotLow`) এবং ব্যাকগ্রাউন্ড অটো-স্ক্যান ওয়ার্কার টেস্ট।
- `ShieldCoreRobolectricTest`: Compose ড্যাশবোর্ড রেন্ডারিং, `RamUsageDashboard`, `Low-Battery Safety` সুইচ, স্ক্রল, নেভিগেশন এবং Room ডেটাবেস কোয়েরি টেস্ট।
- `ExampleRobolectricTest`: অ্যাপ রিসোর্স ও কনটেক্সট ভেরিফিকেশন।

---

## GitHub থেকে Android APK রিলিজ গাইড (CI/CD Release)

**Repository:** [https://github.com/rsk934128-dot/ShieldCore](https://github.com/rsk934128-dot/ShieldCore)

এই প্রজেক্টে `.github/workflows/release-apk.yml` নামে একটি স্বয়ংক্রিয় **GitHub Actions** রিলিজ পাইপলাইন যুক্ত করা হয়েছে।

### ১. GitHub-এ পুশ করলেই অটো-রিলিজ
- `main` ব্রাঞ্চে কোড পুশ করলে ওয়ার্কফ্লোটি স্বয়ংক্রিয়ভাবে ইউনিট/Robolectric টেস্ট চালিয়ে `ShieldCore-debug.apk` বিল্ড করে এবং `v1.0.<run_number>` ট্যাগসহ [GitHub Releases](https://github.com/rsk934128-dot/ShieldCore/releases) পেজে প্রকাশ করে।

### ২. Signed Release APK-এর জন্য GitHub Secrets সেটআপ (ঐচ্ছিক তবে প্রোডাকশনের জন্য সুপারিশকৃত)
আপনার পিসিতে একটি রিলিজ কীস্টোর তৈরি করে সেটিকে Base64-এ কনভার্ট করুন (`keyAlias` অবশ্যই `upload` রাখতে হবে):

```bash
keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
base64 -w 0 my-upload-key.jks
```

এরপর আপনার GitHub রিপোজিটরির **Settings → Secrets and variables → Actions → New repository secret**-এ নিচের ৩টি Secret যোগ করুন:
- `KEYSTORE_BASE64`: উপরের `base64` আউটপুট
- `STORE_PASSWORD`: কীস্টোর পাসওয়ার্ড
- `KEY_PASSWORD`: কী পাসওয়ার্ড

*(Secret সেট না করলেও `ShieldCore-debug.apk` স্বয়ংক্রিয়ভাবে GitHub Releases ও Build Artifacts-এ যুক্ত হবে।)*

### ৩. ম্যানুয়াল বা ট্যাগ ভিত্তিক রিলিজ
- **পদ্ধতি A (Git Tag):** `git tag v1.0.0 && git push origin v1.0.0` — সাথে সাথেই **GitHub Releases** পেজে `ShieldCore-release.apk` ও `ShieldCore-release.aab` যুক্ত হয়ে যাবে।
- **পদ্ধতি B (ম্যানুয়াল):** GitHub রিপোজিটরির **Actions → Release Android APK → Run workflow** বাটনে ক্লিক করে কাস্টম `release_tag` দিয়ে রিলিজ প্রকাশ করুন।

---

<div align="center">
  <sub>Built with ♥ for Android Security by <strong>শেখ ফরিদ (Sheikh Farid)</strong> • <a href="https://github.com/rsk934128-dot/ShieldCore">rsk934128-dot/ShieldCore</a></sub>
</div>
