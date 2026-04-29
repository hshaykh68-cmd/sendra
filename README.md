# Sendra - Offline-First P2P File Transfer

A production-grade, offline-first, peer-to-peer file transfer application for Android built with Kotlin and Jetpack Compose.

## Architecture

Sendra follows **Clean Architecture** with a **MVVM + MVI** hybrid pattern:

```
┌─────────────────────────────────────────────────────────────────┐
│                         APP MODULE                              │
│              (Application, DI, MainActivity)                     │
├─────────────────────────────────────────────────────────────────┤
│  UI       │ Discovery │ Connection │ Transfer │ Platform        │
│  (Compose)│ (NSD/mDNS)│ (LAN/WiFi  │ (Chunked   │ (Foreground   │
│           │           │  Direct/   │  Parallel   │  Service,     │
│           │           │  Hotspot)    │  Resume)    │  Controllers) │
├─────────────────────────────────────────────────────────────────┤
│                            DATA MODULE                          │
│              (Room Database, DAOs, Repositories)               │
├─────────────────────────────────────────────────────────────────┤
│                           DOMAIN MODULE                         │
│              (Models, Use Cases, Repository Interfaces)      │
├─────────────────────────────────────────────────────────────────┤
│                            CORE MODULE                          │
│              (Result, Dispatchers, Constants, Extensions)      │
└─────────────────────────────────────────────────────────────────┘
```

## Modules

- **app**: Application entry point, DI configuration, MainActivity
- **core**: Utilities, Result wrapper, Dispatchers, Constants
- **domain**: Business logic, models, repository interfaces, use cases
- **data**: Room database, DAOs, repository implementations
- **discovery**: NSD/mDNS device discovery with smoothing
- **connection**: Transport layer (LAN, WiFi Direct, Hotspot)
- **transfer**: Transfer engine with chunked parallel streams, resume, integrity
- **ui**: Jetpack Compose screens and ViewModels
- **platform**: Foreground service, platform controllers

## Key Features

- **Offline-First**: No internet required, works on LAN/WiFi Direct/Hotspot
- **Chunked Transfer**: Files split into configurable chunks (default 256KB)
- **Parallel Streams**: Up to 3 concurrent streams per transfer
- **Resume Support**: Persistent chunk state tracking
- **Integrity Checks**: CRC32 verification for each chunk
- **Real-time Radar**: Visual device discovery with signal strength
- **State Management**: Reactive UI with StateFlow and Compose

## Tech Stack

- Kotlin 1.9.22
- Jetpack Compose (BOM 2024.02.00)
- Hilt 2.50 (DI)
- Room 2.6.1 (Persistence)
- Kotlin Coroutines + Flow
- NSD/mDNS (Device Discovery)
- TCP Sockets (Data Transfer)

## Building

```bash
# Sync Gradle
./gradlew :app:dependencies

# Build debug APK
./gradlew :app:assembleDebug

# Install on device
./gradlew :app:installDebug
```

## Architecture Decisions

1. **Modular Structure**: Clear separation of concerns, enables parallel development
2. **Clean Architecture**: Domain-driven, testable, framework-independent business logic
3. **Reactive State**: StateFlow for UI state, SharedFlow for events
4. **Chunked Transfers**: Resume capability, parallelization, integrity verification
5. **Foreground Service**: Reliable background operation with notification controls
