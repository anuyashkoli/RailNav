# RailNav

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84.svg?style=for-the-badge&logo=android-studio&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)
![OpenStreetMap](https://img.shields.io/badge/OpenStreetMap-7EBC6F?style=for-the-badge&logo=OpenStreetMap&logoColor=white)
![GeoJSON](https://img.shields.io/badge/GeoJSON-000000?style=for-the-badge&logo=json&logoColor=white)
![Material Design](https://img.shields.io/badge/Material%20Design-757575.svg?style=for-the-badge&logo=material-design&logoColor=white)

RailNav is an Android application designed to assist commuters in navigating complex railway station layouts. It provides offline-capable, graph-based walking directions to facilities such as platforms, exits, ticket counters, and lifts. By leveraging pathfinding algorithms and interactive mapping, the application solves the challenge of locating specific amenities within high-density transit hubs, specifically optimized for Thane Station.

## Features

* **Graph-Based Pathfinding:** Utilizes the A* algorithm to calculate the shortest walking path between station nodes (e.g., Entrance to Platform 1).
* **Interactive Station Map:** Visualizes the user's current location, station facilities, and the calculated route using OpenStreetMap data.
* **Smart Search:** Allows users to locate facilities by name or type with real-time filtering and distance estimations.
* **Real-Time Navigation:** Integrates Android Location Services to detect user position and suggest the nearest starting node.
* **Turn-by-Turn Directions:** Generates step-by-step textual instructions derived from path geometry.

## Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material3)
* **Map Engine:** OSMDroid (OpenStreetMap for Android)
* **Architecture:** MVVM (Model-View-ViewModel) with Unidirectional Data Flow
* **Data Format:** GeoJSON (parsed via Kotlinx Serialization)
* **Build System:** Gradle with Version Catalogs (`libs.versions.toml`)

## Getting Started

Follow these instructions to set up the project locally for development and testing.

### Prerequisites

Ensure your development environment meets the following requirements:

* **Android Studio:** Latest stable version recommended.
* **Java Development Kit (JDK):** Version 11.
* **Android SDK:** API Level 36 (Baklava) is required for compilation.
* **Min SDK:** API Level 24 (Android 7.0).

### Installation

1. **Clone the repository:**
```bash
git clone https://github.com/anuyashkoli/railnav.git
cd railnav

```


2. **Open in Android Studio:**
Open Android Studio and select **File > Open**, then navigate to the cloned directory.
3. **Sync Gradle:**
Allow Android Studio to download dependencies and sync the project. This may take a few minutes.
4. **Build and Run:**
Connect an Android device (or use an emulator) and execute the run command:
```bash
./gradlew installDebug

```


*Note: A physical device with GPS capabilities is recommended to test location-based features effectively.*

## Project Structure

The project follows a standard Android MVVM architecture. Below is a simplified overview of the core file structure:

```text
railnav/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   ├── nodes.geojson       # Station nodes (rooms, platforms, exits)
│   │   │   │   └── edges.geojson       # Walkable paths connecting nodes
│   │   │   ├── java/com/app/railnav/
│   │   │   │   ├── data/
│   │   │   │   │   ├── Graph.kt        # Network topology builder
│   │   │   │   │   ├── PathFinder.kt   # A* algorithm implementation
│   │   │   │   │   └── GraphRepository.kt # Data loading from assets
│   │   │   │   ├── ui/                 # Theme and styling
│   │   │   │   ├── LocationHandler.kt  # GPS and permission logic
│   │   │   │   ├── MainActivity.kt     # Entry point and Compose UI host
│   │   │   │   ├── MainViewModel.kt    # State management and logic
│   │   │   │   └── MapView.kt          # OSMDroid map composable wrapper
│   │   │   └── AndroidManifest.xml     # App permissions and configuration
│   └── build.gradle.kts                # Module-level build configuration
├── gradle/
│   └── libs.versions.toml              # Dependency version catalog
└── build.gradle.kts                    # Project-level build configuration

```

## Permissions

The application requires the following permissions to function correctly:

* `ACCESS_FINE_LOCATION`: To determine the user's precise position within the station.
* `INTERNET`: Required by OSMDroid to fetch map tiles.
* `WRITE_EXTERNAL_STORAGE`: Used for caching map tiles (max SDK 32).
