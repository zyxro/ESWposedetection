# ESW Pose Detection Project

This repository contains a comprehensive pose detection system with Android application and ML model components.

## Project Structure

```
ESWposedetection/
├── Application/          # Android Application with YOLO-NAS + HRNet pose detection
│   ├── app/             # Main Android app module
│   │   ├── src/main/cpp/        # C++ backend with QIDK integration
│   │   ├── src/main/java/       # Kotlin UI and camera integration
│   │   └── build.gradle.kts     # App-level build configuration
│   ├── gradle/          # Gradle wrapper and dependencies
│   ├── gradlew*         # Gradle wrapper scripts
│   └── settings.gradle.kts      # Project settings
│
├── Model/               # ML Model training and dataset
│   ├── webcam_captures/ # Training data with pose keypoints
│   ├── create_dataset.py        # Dataset creation script
│   ├── posture_dataset.csv      # Processed dataset
│   └── README.md        # Model documentation
│
└── README.md           # This file
```

## Getting Started

### Android Application
```bash
cd Application
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### ML Model Training
```bash
cd Model
python create_dataset.py
```

## Features

- **Real-time pose detection** with YOLO-NAS and HRNet
- **Performance analytics** with FPS and timing metrics
- **Posture analysis** with quality scoring and duration tracking
- **Professional UI** with Material 3 design
- **ML model training** pipeline for custom posture classification

## Technical Stack

- **Android**: Kotlin, Jetpack Compose, CameraX, Material 3
- **Backend**: C++17, Android NDK, CMake
- **ML Framework**: Python, OpenCV, MediaPipe
- **Build System**: Gradle 8.13, CMake 3.22

## Recent Updates

- ✅ Fixed C++ mutex linking issues with proper NDK library configuration
- ✅ Enhanced YOLO-NAS + HRNet coordinate mapping for accurate overlays  
- ✅ Added comprehensive performance monitoring and posture analytics
- ✅ Restructured repository with clean Application/Model separation