# Alpha SuperApp - Code Description

## Overview

**Alpha SuperApp** is a feature-rich Android application built with modern development practices. It demonstrates professional architecture patterns, cutting-edge Android technologies, and seamless integration with cloud services. The app provides users with intelligent tools for budgeting, calculations, gesture-based device control, and AI-powered web search.

---

## Core Architecture

### Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (modern declarative UI)
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Build System**: Gradle with Kotlin DSL
- **Data Persistence**: Android DataStore, Repository Pattern
- **Minimum API Level**: Android 21+

### Key Design Patterns

1. **MVVM Architecture**: Separation of concerns with ViewModels handling business logic and state management
2. **Repository Pattern**: Data access abstraction layer managing local and remote data
3. **State-based UI**: Reactive UI updates using Kotlin Flow and StateFlow
4. **Dependency Injection**: Application-level dependencies like repositories and managers
5. **Composable Functions**: Reusable UI components following Compose best practices

---

## Main Application Entry Point

### MainActivity (com.alpha.MainActivity)

- **Purpose**: Root activity that initializes the application and sets up the theme system
- **Responsibilities**:
  - Handles system-wide theme configuration (light/dark mode)
  - Manages theme persistence using DataStore
  - Enables edge-to-edge layout for modern Android devices
  - Routes navigation through the main app navigation graph

**Key Features**:
- Real-time theme switching between light and dark modes
- System theme detection with manual override capability
- Persistent theme preferences using AppSettings

---

## Features Overview

### 1. **Budget Management Module**
**Location**: `features/budget/`

#### Purpose
Comprehensive personal finance tracking system that helps users manage expenses, analyze spending patterns, and synchronize financial data across devices.

#### Core Components

**BudgetViewModel** (BudgetViewModel.kt)
- Central state management for all budget-related operations
- Maintains transaction lists, import previews, and sync status
- Handles duplicate detection and conflict resolution during imports
- Manages Google Drive authentication and synchronization

**BudgetRepository** (BudgetRepository.kt)
- Data access layer for persistent transaction storage
- Manages CRUD operations on transaction data
- Provides reactive data streams via StateFlow

**BudgetScreen** (BudgetScreen.kt)
- User interface for viewing and managing transactions
- Displays budget overview and transaction history
- Provides import and synchronization controls

#### Key Sub-Features

**Bill Photo Manager** (BillPhotoManager.kt)
- Captures photos of receipts and bills
- Uses camera integration for image acquisition
- Manages file storage and retrieval

**Google Drive Sync** (DriveSync.kt)
- Authenticates with Google Drive API
- Synchronizes budget data with cloud storage
- Enables cross-device data consistency

**Email Parsers**
- **GmailParser.kt**: Extracts transaction data from Gmail messages (bank statements, receipts)
- **EsewaXlsParser.kt**: Parses Excel (.xls) files from Esewa payment service
- Automatically imports formatted transaction data with smart category detection

**Data Models** (models/)
- `Transaction.kt`: Individual expense/income record with metadata
- `BudgetState.kt`: Overall budget UI state and transaction collections
- `TransactionCategory`: Enum classifying transaction types
- `TransactionSource`: Enum tracking data origin (manual, email, file import)

---

### 2. **Calculator Module**
**Location**: `features/calculator/`

#### Purpose
Simple arithmetic calculator for quick mathematical calculations.

#### Components

**CalculatorScreen** (CalculatorScreen.kt)
- Touch-friendly interface with standard calculator operations
- Real-time calculation display
- Clear and intuitive button layout

---

### 3. **SBR Control Module (Gesture-Based Device Control)**
**Location**: `features/sbrcontrol/`

#### Purpose
Innovative hand gesture recognition system enabling users to control Bluetooth devices hands-free using real-time camera input and AI-powered gesture detection.

#### Core Components

**SbrControlViewModel** (SbrControlViewModel.kt)
- Manages gesture detection state and Bluetooth connection status
- Processes real-time gesture recognition results
- Coordinates between camera pipeline and Bluetooth communication

**SbrControlScreen** (SbrControlScreen.kt)
- Live camera preview with real-time gesture overlay
- Displays current detected gesture and device connection status
- Color-coded status indicators (green for connected, red for disconnected)
- Uses CameraX for camera access and image analysis

**Hand Gesture Detection Pipeline**

**HandGestureProcessor** (HandGestureProcessor.kt)
- Orchestrates the complete gesture recognition workflow
- Manages MediaPipe hand landmark detection
- Processes camera frames and extracts hand position data

**GestureLogic** (GestureLogic.kt)
- Interprets hand landmarks into recognizable gestures
- Maps detected gestures to control commands
- Implements gesture vocabulary (e.g., thumbs up, peace sign, etc.)

**GestureStability** (GestureStability.kt)
- Filters unstable gesture detections
- Applies temporal smoothing to reduce false positives
- Ensures reliable gesture recognition despite hand tremors or quick movements
- Implements debouncing for command execution

**BluetoothComm** (BluetoothComm.kt)
- Handles Bluetooth device discovery and pairing
- Sends gesture-derived commands to connected devices
- Manages connection lifecycle and error handling
- Supports standard Bluetooth serial communication

#### Technical Details

- **Hand Landmark Model**: Uses MediaPipe hand_landmarker.task (21-point hand skeleton detection)
- **Real-time Processing**: Camera frames analyzed at ~30 FPS for responsive gesture detection
- **Stability Filtering**: Temporal analysis prevents accidental gesture triggers
- **Bluetooth Protocols**: Serial communication for compatible devices

---

### 4. **Web Search Module (AI-Powered)**
**Location**: `features/websearch/`

#### Purpose
Intelligent search interface powered by Google Gemini AI, enabling natural language queries with optional image attachment for multimodal search.

#### Core Components

**WebSearchViewModel** (WebSearchViewModel.kt)
- Manages search query state and results display
- Handles loading states and error messaging
- Manages image attachment for multimodal queries

**WebSearchScreen** (WebSearchScreen.kt)
- Text input field for search queries
- Real-time results display
- Image picker integration for vision-based search

**WebSearchUiState** (Data Class)
- `query`: Current search query
- `result`: Search results from Gemini AI
- `isLoading`: Loading indicator state
- `error`: Error messages
- `imageBytes`: Attached image data for image-based searches

#### AI Integration

**GeminiClient** (core/ai/GeminiClient.kt)
- Direct integration with Google Gemini API
- Supports both text-only and image+text queries
- Generates contextual responses using latest AI models
- Handles API authentication and request formatting
- Implements error handling and timeout management

---

### 5. **Settings Module**
**Location**: `features/settings/`

#### Purpose
User preferences management with focus on theme customization and visual experience configuration.

#### Components

**SettingsScreen** (SettingsScreen.kt)
- User interface for preference adjustment
- Toggle controls for theme settings

**SettingsViewModel** (SettingsViewModel.kt)
- State management for user preferences
- Preference update logic and validation

**AppSettings** (AppSettings.kt)
- DataStore wrapper for persistent preference storage
- Theme preference management
- System theme detection
- Dark mode preference
- Application-wide settings access

#### Theme System

- **Light Mode**: Optimized color palette for daylight viewing
- **Dark Mode**: Eye-friendly colors for low-light environments
- **System-Aware**: Automatically matches device system theme when enabled
- **Manual Override**: Users can disable system sync and set explicit theme preference

---

## UI Layer

### Navigation System
**Location**: `ui/navigation/NavGraph.kt`

- **Purpose**: Central navigation orchestration for the application
- **Navigation Pattern**: Jetpack Compose NavHost with route-based navigation
- **Routes**:
  - `home`: Main home screen with feature tiles
  - `web_search`: AI-powered search interface
  - `calculator`: Calculator tool
  - `budget`: Budget management interface
  - `sbrcontrol`: Gesture control setup and usage
  - `settings`: Preference management

### Home Screen
**Location**: `ui/home/HomeScreen.kt`

- Central hub displaying all available features
- Feature tile grid with navigation shortcuts
- Theme toggle button (quick access)
- Settings navigation

### Theme System
**Location**: `ui/theme/`

**Color.kt**: Defines the application color palette
**Theme.kt**: Material 3 theme configuration with light/dark variants
**Type.kt**: Typography scale and text styles for consistent text rendering

---

## Data Layer

### Models
**Location**: `features/budget/models/`

**Transaction.kt**
- Represents a single financial transaction
- Fields: amount, date, category, description, source, ID

**BudgetState.kt**
- Container for all budget-related UI state
- Transactions list, totals, filtered views

**CategoryBudget**: Budget allocation per expense category
**TransactionCategory**: Enum of transaction types (food, transport, entertainment, etc.)
**TransactionSource**: Origin of transaction data (manual entry, email parse, file import)

---

## Core Services

### AI Integration
**Location**: `core/ai/GeminiClient.kt`

- Google Gemini API client wrapper
- Handles authentication and API communication
- Supports multimodal queries (text + image)
- Implements response parsing and error handling

---

## File Resources

### Assets
**Location**: `assets/models/`

- `hand_landmarker.task`: Pre-trained MediaPipe hand gesture recognition model
  - Detects 21 hand landmark points
  - Enables real-time gesture detection
  - Used by gesture recognition pipeline

### Drawables
**Location**: `res/drawable/`

- `ic_alpha_logo.xml`: Application logo (vector drawable)
- `ic_launcher_foreground.xml`: App icon foreground
- `ic_launcher_background.xml`: App icon background

### Fonts
**Location**: `res/font/`

- Custom typefaces for consistent brand typography

### Manifest
**Location**: `AndroidManifest.xml`

**Permissions Requested**:
- `INTERNET`: API calls (Google services, Gemini AI)
- `CAMERA`: Hand gesture recognition and bill photo capture
- `BLUETOOTH`: Device pairing and communication
- `BLUETOOTH_CONNECT`: Active device communication
- `BLUETOOTH_SCAN`: Device discovery

**Key Declarations**:
- FileProvider for secure file sharing
- MainActivity as entry point
- Theme and branding configuration

---

## Build Configuration

### Build Files
- **root** `build.gradle.kts`: Root project configuration
- **app** `build.gradle.kts`: Application-specific dependencies and configuration

### Gradle Files
- `settings.gradle.kts`: Project structure definition
- `gradle/libs.versions.toml`: Centralized dependency version management
- `app/proguard-rules.pro`: Code obfuscation rules for release builds

---

## Data Flow Architecture

### State Management Flow

```
User Interaction
        ↓
UI Layer (Composable)
        ↓
ViewModel (State Management)
        ↓
Repository/Service Layer
        ↓
Data Sources (Local/Remote)
        ↓
StateFlow Updates
        ↓
UI Re-composition
```

### Real-time Communication

- **Budget Module**: StateFlow for transaction updates
- **Gesture Module**: Real-time camera frame processing → gesture detection → command execution
- **Web Search**: Query submission → API call → result streaming → UI update
- **Settings**: DataStore preference changes → theme re-application

---

## Key Workflows

### Budget Transaction Import Workflow
1. User selects Excel file or email to parse
2. Parser extracts transaction records
3. ViewModel detects duplicate transactions
4. UI presents import preview with duplicates
5. User confirms/rejects duplicate handling
6. New transactions persisted to database
7. Optional: Sync to Google Drive

### Gesture Control Workflow
1. Camera permission requested from user
2. Camera feed captured and analyzed in real-time
3. Hand landmarks detected by MediaPipe model
4. Hand positions converted to gesture classifications
5. Gesture stability filters applied to prevent false triggers
6. Confirmed gesture command sent to Bluetooth device
7. Status displayed in real-time overlay

### AI Web Search Workflow
1. User enters natural language query
2. Optional: User attaches image for visual search
3. Query submitted to Google Gemini API
4. AI generates contextual response
5. Results displayed in formatted text
6. User can refine search or navigate away

---

## Code Quality & Best Practices

### Architectural Principles
- **Single Responsibility**: Each class/component has one clear purpose
- **Dependency Injection**: Dependencies passed to constructors
- **Reactive Programming**: Using Flow/StateFlow for observable data
- **Type Safety**: Leverage Kotlin's type system
- **Error Handling**: Graceful degradation and user-friendly error messages

### Developer Experience
- Organized package structure by feature
- Clear naming conventions following Kotlin standards
- Separation of concerns (UI, ViewModel, Repository, Model layers)
- Reusable components and utility functions
- DataStore for resilient preference management

### Security
- FileProvider for secure file sharing
- Bluetooth permissions properly scoped
- API authentication handled securely
- Camera permissions follow Android best practices

---

## Future Extensibility

The modular architecture allows for easy addition of:
- New budget parsers for additional financial services
- Additional gesture commands for Bluetooth devices
- More AI-powered features via Gemini integration
- Export functionality for tax preparation
- Budget forecasting and analytics
- Multi-user support with cloud synchronization

---

## Summary

Alpha SuperApp is a sophisticated Android application demonstrating:
- ✅ Modern Jetpack Compose UI framework
- ✅ Clean MVVM architecture
- ✅ Real-time gesture recognition with MediaPipe
- ✅ AI-powered features via Google Gemini
- ✅ Cloud synchronization with Google Drive
- ✅ Bluetooth device control
- ✅ Email and file parsing for data import
- ✅ Theme customization with system integration
- ✅ Professional code organization and best practices

The app serves as both a practical tool for personal finance management and a reference implementation for modern Android development patterns.
