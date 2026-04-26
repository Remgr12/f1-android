# F1 Android

A modern Formula 1 companion application for Android, built with Jetpack Compose and the [OpenF1 API](https://openf1.org/).

## Features

- **Standings**: Real-time Driver and Constructor leaderboards.
- **Race Calendar & Results**: Complete history of past races with detailed session data, including lap times and pit stop information.
- **Session Reminders**: Get notified 30 minutes before a session starts so you never miss a moment of the action.
- **Live Race Hub**: Follow live sessions with real-time driver intervals and race control messages (requires API configuration).
- **Interactive Track Map**: Visualize driver positions on the circuit during live or past sessions.
- **Driver & Constructor Details**: In-depth statistics and information for every driver and team on the grid.
- **Modern UI**: Fully responsive interface built with Material 3, supporting Light, Dark, and Dynamic (Material You) theming.
- **Offline Support**: Local caching powered by Room for viewing data without an active connection.

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
- **Local Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Serialization**: [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
- **Async Programming**: [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

## Getting Started

### Prerequisites

- Android Studio Ladybug | 2024.2.1 or newer
- JDK 21+ (Project uses Java Toolchain 26)
- Android SDK 28+

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/remgr/f1-android.git
   ```
2. Open the project in Android Studio.
3. Sync Project with Gradle Files.
4. Run the `app` module on your device or emulator.

### API Configuration

The app uses the OpenF1 API. While most data is public, live race features may require specific API configurations or keys depending on the provider used. These can be configured in the **Settings** screen within the app.

## Project Structure

- `core`: Shared components including network, database, notifications, and settings.
- `feature`: Module-based feature implementation (Leaderboard, Live Hub, Past Races, etc.).
- `navigation`: Jetpack Navigation Compose implementation.
- `ui.theme`: Material 3 theme definitions and typography.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Disclaimer: This is an unofficial fan app and is not affiliated with the Formula 1 companies. F1, FORMULA ONE, FORMULA 1, FIA FORMULA ONE WORLD CHAMPIONSHIP, GRAND PRIX and related marks are trade marks of Formula One Licensing B.V.*
