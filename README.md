<div align="center">
  <img src="app/src/main/res/drawable/ic_shield_emblem.jpg" alt="ShieldCore Logo" width="110" height="110" />
  <h1>ShieldCore — Android Security & Cleanup Suite</h1>
  <p>
    <strong>আধুনিক অ্যান্ড্রয়েড ভাইরাস স্ক্যানার, অ্যাপ পারমিশন অডিটর এবং স্টোরেজ ক্লিনআপ ড্যাশবোর্ড</strong><br />
    Built with Kotlin, Jetpack Compose, Material 3, Room Database &amp; DataStore
  </p>

  ![Platform](https://img.shields.io/badge/Platform-Android%20(Min%20SDK%2024)-00E5A3?style=for-the-badge&logo=android&logoColor=090E17)
  ![Language](https://img.shields.io/badge/Language-Kotlin%202.2-38BDF8?style=for-the-badge&logo=kotlin&logoColor=090E17)
  ![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-00E5A3?style=for-the-badge&logo=jetpackcompose&logoColor=090E17)
  ![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Room-F59E0B?style=for-the-badge)
</div>

<br />

<p align="center">
  <img src="docs/images/hero_banner.jpg" alt="ShieldCore Cybersecurity Banner" width="100%" />
</p>

---

## পরিচিতি (Overview)

**ShieldCore** হলো একটি প্রাইভেসি-ফার্স্ট ও Play-Policy সম্মত অ্যান্ড্রয়েড সিকিউরিটি ও অপ্টিমাইজেশন অ্যাপ। এটি `java.io.File` সিস্টেম ফাইল স্ক্যানিং, **Storage Access Framework (SAF)**, **Room** ভিত্তিক লোকাল ম্যালওয়্যার সিগনেচার ডেটাবেস (`SHA-256` ও `MD5`), এবং `PackageManager` অ্যাপ পারমিশন অডিটিং একত্রিত করে একটি রেসপন্সিভ **Cyber-Vault** ড্যাশবোর্ডে উপস্থাপন করে।

---

## মূল ফিচারসমূহ (Core Security Pillars)

<p align="center">
  <img src="docs/images/security_pillars.jpg" alt="ShieldCore Four Security Pillars" width="100%" />
</p>

| মডিউল (Module) | বিবরণ (Description) | টেকনোলজি (Implementation) |
| :--- | :--- | :--- |
| **Deep Scan Engine** | রিকার্সিভ ফাইল-সিস্টেম স্ক্যানিং, সিঙ্গেল-পাস ডুয়াল হ্যাশিং (`SHA-256` + `MD5`), সাইকেল/সিমলিংক প্রোটেকশন এবং ডাবল-এক্সটেনশন হিউরিস্টিক চেক | `CoreFileScanner`, `java.io.File`, `MessageDigest`, SAF (`OpenDocumentTree` / `OpenDocument`) |
| **Junk Cleaner** | ডিভাইসের আসল ফ্রি/ব্যবহৃত স্টোরেজ পরিমাপ ও অ্যাপ ক্যাশ, কোড ক্যাশ এবং ডায়াগনস্টিক লগ পরিষ্কার | `StatFs`, `cacheDir`, `codeCacheDir`, `externalCacheDir` |
| **App Permission Auditor** | ইনস্টল থাকা লঞ্চেবল অ্যাপগুলোর সংবেদনশীল পারমিশন (Camera, Mic, Location, SMS) বিশ্লেষণ ও রিস্ক লেভেল নির্ণয় | `PackageManager`, Play-compliant `<queries>` filter |
| **Threat Signature Vault** | লোকাল ম্যালওয়্যার হ্যাশ ইনডেক্স ও স্ক্যান হিস্টোরি সংরক্ষণ, পাশাপাশি সক্রিয় ফ্ল্যাগড ফাইল ডিলিট/কোয়ারেন্টাইন | `Room Database` (`MalwareSignatureEntity`, `ScanHistoryEntity`), `DataStore` |

---

## ড্যাশবোর্ড ও ইউজার এক্সপেরিয়েন্স (Responsive UI & UX)

- **অ্যাডাপ্টিভ ও রেসপন্সিভ ডিজাইন (`BoxWithConstraints`)**:
  - **Compact (Phone Portrait)**: সিঙ্গেল-কলাম ড্যাশবোর্ড + কাস্টম অ্যানিমেটেড `SecurityScoreRing` (`0–100`) + নিচের **Material 3 NavigationBar**।
  - **Medium / Expanded (Tablet, Foldable, Landscape ≥ 680dp)**: বাম পাশে **NavigationRail** সহ **Two-Pane** ক্যাননিক্যাল ড্যাশবোর্ড লেআউট।
- **Bilingual Support (`EN` / `বাংলা`)**: টপ অ্যাপ বার থেকে এক ট্যাপেই ইংরেজি এবং বাংলা ভাষায় সুইচ করার সুবিধা।
- **Dark & Light Theme**: Cyber-Vault **Obsidian Emerald** ডার্ক থিম এবং ক্লিন **Ice-Slate** লাইট থিম, **Space Grotesk**, **Plus Jakarta Sans** এবং **JetBrains Mono** ফন্টসহ।

---

## প্রজেক্ট আর্কিটেকচার (Architecture)

```text
com.example
├── data
│   ├── engine
│   │   ├── CoreFileScanner.kt        # Recursive java.io.File traversal, SHA-256/MD5 & heuristics
│   │   └── DeviceSecurityEngine.kt   # SAF document scanning, StatFs storage, PackageManager audit
│   ├── local
│   │   ├── MalwareSignatureEntity.kt # Room entity storing SHA-256 & MD5 threat indicators
│   │   ├── ScanHistoryEntity.kt      # Room entity storing scan audit logs
│   │   ├── SecurityDao.kt            # Reactive Flow & suspend hash lookup queries
│   │   └── SecurityDatabase.kt       # Room database singleton with default signatures
│   ├── model
│   │   └── SecurityModels.kt         # Domain models for files, apps, junk categories & storage
│   ├── preferences
│   │   └── SecurityPreferences.kt    # Jetpack DataStore for shield, theme & language settings
│   └── repository
│       └── SecurityRepository.kt     # Unified repository layer
└── ui
    ├── components
    │   ├── SecurityScoreRing.kt      # Custom Canvas circular radar & animated score gauge
    │   └── FeatureModuleCards.kt     # Interactive M3 module cards
    ├── screens
    │   ├── DashboardScreen.kt        # Responsive single-column / two-pane dashboard
    │   ├── DeepScanScreen.kt         # File/Folder scanner, hash log & threat removal
    │   ├── JunkCleanerScreen.kt      # Storage meter & cache cleaner
    │   └── AppManagerScreen.kt       # App permission audit & system settings deep-link
    ├── theme                         # Color.kt, Type.kt, Theme.kt
    ├── i18n/AppStrings.kt            # English & Bengali localization
    ├── SecurityViewModel.kt          # StateFlow state management
    └── ShieldCoreApp.kt              # Adaptive NavigationBar / NavigationRail host
```

---

## Google Play প্রাইভেসি ও পলিসি কমপ্লায়েন্স

- **কোনো Broad Storage Permission নেই**: `MANAGE_EXTERNAL_STORAGE` বা `READ_EXTERNAL_STORAGE` ব্যবহার না করে শূন্য-পারমিশনের **Storage Access Framework (SAF)** ব্যবহার করা হয়েছে।
- **কোনো `QUERY_ALL_PACKAGES` নেই**: `AndroidManifest.xml`-এ নির্দিষ্ট `<queries>` (`ACTION_MAIN` + `CATEGORY_LAUNCHER`) ফিল্টার ব্যবহার করে কেবল লঞ্চেবল অ্যাপগুলোর পারমিশন অডিট করা হয়।

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
- `ShieldCoreRobolectricTest`: Compose ড্যাশবোর্ড রেন্ডারিং, স্ক্রল, নেভিগেশন এবং Room ডেটাবেস কোয়েরি টেস্ট।
- `ExampleRobolectricTest`: অ্যাপ রিসোর্স ও কনটেক্সট ভেরিফিকেশন।

---

## GitHub থেকে Android APK রিলিজ গাইড (CI/CD Release)

এই প্রজেক্টে `.github/workflows/release-apk.yml` নামে একটি স্বয়ংক্রিয় **GitHub Actions** রিলিজ পাইপলাইন যুক্ত করা হয়েছে।

### ১. প্রজেক্ট GitHub-এ পুশ করুন
- AI Studio-র Settings মেনু থেকে **Push to GitHub** ব্যবহার করে কোডটি আপনার GitHub রিপোজিটরিতে পুশ করুন (অথবা Settings থেকে সরাসরি APK/AAB জেনারেট করতে পারেন)।

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

*(যদি Secret সেট না করেন, তবুও ওয়ার্কফ্লোটি টেস্ট চালিয়ে `ShieldCore-debug.apk` বিল্ড ও রিলিজ করবে।)*

### ৩. GitHub Release থেকে APK প্রকাশ করুন
- **পদ্ধতি A (Git Tag):** `git tag v1.0.0 && git push origin v1.0.0` — সাথে সাথেই **GitHub Releases** পেজে `ShieldCore-release.apk` ও `ShieldCore-release.aab` যুক্ত হয়ে যাবে।
- **পদ্ধতি B (ম্যানুয়াল):** GitHub রিপোজিটরির **Actions → Release Android APK → Run workflow** বাটনে ক্লিক করে `release_tag` (যেমন `v1.0.0`) দিয়ে রিলিজ প্রকাশ করুন।

