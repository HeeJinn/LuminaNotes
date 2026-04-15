# Lumina Notes 📝

Lumina Notes is an expressive, Material 3-based note-taking application for Android. It focuses on dynamic UI personalities, accessibility-first design, and seamless navigation.

## ✨ Features

- **Expressive Design**: Implements "Material 3 Expressive" guidelines with bold typography and dynamic layouts.
- **Adaptive Note Personalities**: Note cards change their geometric shape based on their color (e.g., sharp corners for urgent tasks, organic rounded shapes for ideas).
- **Smart Contrast System**: Real-time luminance calculation ensures text is always readable (automatic white/black switching) regardless of the note's background color.
- **Advanced Filtering**: Quickly toggle between "All", "Pinned", and "Archived" notes.
- **Global Transitions**: Smooth 400ms slide-and-fade animations powered by Jetpack Compose Navigation.
- **Safety Net**: Undo functionality for destructive actions like deleting or archiving notes via a Snackbar system.
- **Dark Mode**: Full support for dark themes with dedicated high-contrast color palettes.

## 🛠 Tech Stack & Packages

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) (Material 3)
- **Navigation**: [androidx.navigation:navigation-compose](https://developer.android.com/jetpack/compose/navigation)
- **Local Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Architecture**: MVVM with StateFlow
- **Image/Iconography**: Material Symbols & Icons Extended

## 📂 Package Structure

```text
com.example.agenttest
├── data            # Room entities, DAOs, and Database configuration
├── di              # Dependency Injection (Hilt Modules)
├── ui
│   ├── home        # Main dashboard and note grid components
│   ├── detail      # Note editor (Add/Edit)
│   ├── view        # Full-screen note reading view
│   ├── viewmodel   # Business logic and UI state management
│   ├── navigation  # NavHost and Route definitions
│   └── theme       # M3 Color palettes, Typography, and Adaptive Shapes
└── util            # Date formatting and Color luminance utilities
```

## 🤝 Contributing

We welcome contributions! To maintain the project's "Expressive" quality, please follow these guidelines:

1. **Feature Additions**:
   - If adding a UI component, ensure it supports both Light and Dark modes.
   - Use `ColorUtils.getContrastingColor()` if your component uses custom background colors.
   - For new note categories, update `NoteShapes.kt` to define a unique geometric identity.

2. **Branching Strategy**:
   - Create a feature branch: `feature/your-feature-name`.
   - Ensure all `Hilt` and `Room` annotations are correctly applied before submitting.

3. **Code Style**:
   - Follow the standard Kotlin style guide.
   - Prefer `val` over `var` and use immutable StateFlows where possible.

## 🚀 Getting Started

1. Clone the repository.
2. Open in **Android Studio Ladybug (or newer)**.
3. Build and Run on an emulator or device (API 26+).
4. The database uses `fallbackToDestructiveMigration` for easy schema updates during development.

---
*Lumina Notes — Where every note has its own shape.*
