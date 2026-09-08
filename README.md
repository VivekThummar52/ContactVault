# 🛡️ ContactVault

> **Privacy-First, Local-First, Data-Safety-First Android Contacts Management Application**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room-2.6.1-4285F4.svg?logo=android)](https://developer.android.com/training/data-storage/room)
[![Offline First](https://img.shields.io/badge/Network-0%20Permissions-success.svg)](#privacy--data-safety-guarantee)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 🌟 Overview

**ContactVault** is a modern, high-performance, completely offline Android application designed to view, organize, analyze, and safely manage contacts stored on Android devices.

Unlike traditional contact managers that depend on cloud synchronization, third-party analytics, or remote servers, **ContactVault keeps 100% of user data strictly on the local device.** It provides advanced utility features—such as multi-signal duplicate detection, safe data preservation merges, contact health analysis, custom tagging, offline notes, and local JSON/VCF backups—with zero network exposure.

---

## 🚀 Key Features

```
               ┌────────────────────────────────────────────────────────┐
               │                      ContactVault                      │
               └───────────────────┬────────────────────────────────────┘
                                   │
       ┌───────────────────────────┼───────────────────────────┐
       ▼                           ▼                           ▼
📊 Dashboard & Health      🔍 Smart Duplicates         🛡️ Recovery & Backups
 • Health Score Meter       • Multi-Signal Match        • Pre-Merge Snapshots
 • Quality Breakdown        • Confirmed/Possible        • Offline JSON Backup
 • Quick Metrics            • Side-by-Side Review       • VCF vCard Portability
```

### 1. 📊 Contacts Dashboard & Health Analyzer
* **Health Score Meter (`0–100%`):** Evaluates overall contact book health based on missing phone numbers, missing emails, incomplete names, and duplicates.
* **Issue Filtering:** Instantly filter and inspect contacts belonging to specific health issue categories.
* **Dashboard Overview:** Displays total contacts, favorites, contact groups, potential duplicates, and horizontal avatar row for recently viewed contacts.

### 2. 🔍 Advanced Multi-Signal Duplicate Detector
* **Multi-Signal Engine:** Detects duplicates using normalized phone numbers, normalized email addresses, and exact display name matching.
* **Confidence Scoring:** Categorizes candidates into **Confirmed Duplicates** (90–95% confidence) and **Possible Duplicates** (80% confidence).
* **Side-by-Side Comparison:** Interactive comparison view highlighting matching and conflicting fields between primary and secondary records.

### 3. 🛡️ Data-Safety-First Safe Merge Engine
* **Zero Data Loss Guarantee:** Combines records while preserving all unique phone numbers, email addresses, postal addresses, websites, and notes.
* **Pre-Merge Recovery Snapshots:** Automatically creates a full local JSON snapshot in the Room database (`DeletedContactSnapshotEntity`) before executing a merge or deletion.
* **Automatic Re-linking:** Re-links local ContactVault tags and application notes to the primary record.

### 4. 🏷️ Custom Tags & Offline Notes (Room Database)
* **Custom Tags:** Create custom color-coded tags (e.g., `[Work]`, `[Family]`, `[Important]`) to organize contacts independently from Android Contact Groups.
* **Local Application Notes:** Attach private, timestamped notes to contacts stored locally in Room.

### 5. 📦 Local Backup, Restore & VCF Portability
* **Offline JSON Backup:** Create and restore complete structured backup files (`.json`) containing device contacts, tags, cross-references, and notes using Android's Storage Access Framework (SAF).
* **vCard (VCF v3.0) Support:** Export or import contacts via standard `.vcf` files for cross-device portability.
* **Recently Deleted Vault:** Inspect and restore deleted or merged contact snapshots back into the Android Contacts Provider.

---

## 🏗️ Clean Architecture

ContactVault follows Android's recommended **Modern App Architecture** with strict layer separation and single direction data flow.

```
                  ┌─────────────────────────────────────┐
                  │          UI Layer (Compose)         │
                  │   HomeScreen, ContactsList, etc.    │
                  └──────────────────┬──────────────────┘
                                     │ StateFlow / Actions
                  ┌──────────────────▼──────────────────┐
                  │            ViewModel Layer          │
                  │  HomeVM, ContactsVM, HealthVM, etc. │
                  └──────────────────┬──────────────────┘
                                     │ UseCases / Coroutines
                  ┌──────────────────▼──────────────────┐
                  │            Domain Layer             │
                  │   Models, Repositories, UseCases    │
                  └─────────┬─────────────────┬─────────┘
                            │                 │
            ┌───────────────▼─┐             ┌─▼───────────────┐
            │   Data Layer    │             │   Data Layer    │
            │ ContactsProvider│             │  Room Database  │
            │ (System Truth)  │             │ (Local Storage) │
            └─────────────────┘             └─────────────────┘
```

---

## 🛠️ Technology Stack

| Component | Library / Framework | Purpose |
| :--- | :--- | :--- |
| **Language** | Kotlin `2.2.10` | Coroutines, Flow, StateFlow, Pattern Matching |
| **UI Framework** | Jetpack Compose `2026.02.01` (Material 3) | Declarative UI, Dynamic Color, Adaptive Layouts |
| **Navigation** | Navigation Compose `2.8.7` | Type-safe Screen Transitions & Bottom Navigation |
| **Architecture** | MVVM + Use Cases | Clean Architecture, Lifecycle ViewModel |
| **Local Database** | Room `2.6.1` + KSP `2.0.21-1.0.27` | Offline storage for Tags, Notes, Snapshots, History |
| **System API** | Android `ContactsContract` | `ContentResolver` interaction for device contacts |
| **Image Loading** | Coil Compose `2.7.0` | Asynchronous contact thumbnail & photo rendering |
| **File I/O** | Storage Access Framework (SAF) | Offline file picker for JSON backups and VCF files |

---

## 🔒 Privacy & Data Safety Guarantee

* 🚫 **No Internet Permission:** The application does not declare `android.permission.INTERNET` in its manifest.
* 🚫 **No Analytics or SDKs:** Zero third-party trackers, Firebase, advertising SDKs, or cloud services.
* 🔒 **100% Local Processing:** Contact analysis, duplicate detection, and backups are processed entirely on device CPU and local storage.

---

## 🧪 Testing & Quality Assurance

The codebase includes unit tests covering core utility and business logic:

* **Phone & Email Normalization:** Digits extraction, country code stripping, and duplicate matching logic.
* **VCF Generation & Parsing:** vCard v3.0 syntax formatting and line parsing.
* **Backup Schema Validation:** JSON header and structure validation.
* **Domain & Room Models:** Tags, Notes, Groups, and Health Score metrics.

Run unit tests locally via Gradle:
```bash
./gradlew :app:testDebugUnitTest
```

---

## 💻 Building the Project

### Prerequisites
* **Android Studio:** Panda (2026.1.1+) or latest stable release.
* **JDK:** Java 11 or higher.
* **SDK:** Compile SDK `37`, Min SDK `24`.

### Build Commands
```bash
# Clone the repository
git clone https://github.com/example/ContactVault.git
cd ContactVault

# Build Debug APK
./gradlew :app:assembleDebug

# Run Unit Tests
./gradlew :app:testDebugUnitTest
```

---

## 📜 License

```
MIT License

Copyright (c) 2026 ContactVault Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to do so.
```
