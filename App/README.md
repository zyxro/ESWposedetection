# ESW Posture Detection (QIDK-ready)

This app demonstrates posture detection for a seated person with a camera preview and a pose skeleton overlay. It uses ML Kit Pose for on-device development and provides a stub to swap in QIDK (YOLO-NAS + HRNet) on the Qualcomm Innovators Development Kit.

## Build & Run (Android Studio)

- Open the `project/` folder in Android Studio.
- Use a device/emulator with Camera and Android 7.0+ (API 24+).
- Grant the camera permission when prompted.
- Switch to the Camera tab to see the live preview and the pose skeleton overlay.

## Features

- CameraX preview with overlayed pose keypoints and skeleton lines.
- Simple posture score demo on Home screen.
- Toggle skeleton overlay visibility on the Camera screen.

## QIDK Integration Plan (Device)

1. Models
   - Place QNN-converted models in `app/src/main/assets/` as e.g. `yolo_nas_person.qnn` and `hrnet_pose.qnn`.
   - Ensure labels/metadata if needed by your conversion flow.

2. Native Backends
   - Integrate QNN/SNPE or QIDK runtime native libs for the board’s SoC.
   - Update `app/build.gradle.kts` packaging (jniLibs) if required.

3. Backend Swap
   - Implement `QidkBackends.detectAndPose(...)` to run YOLO-NAS person detection then HRNet pose estimation on the ROI.
   - Publish results as `PoseOverlay` (keypoints with screen-space coordinates and scores).
   - Toggle ML Kit vs QIDK backend via a build flag or a runtime switch.

4. Performance Notes
   - Use `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` and suitable analysis resolution (e.g., 640x480).
   - Prefer YUV to RGB conversion directly in native for QNN input.
   - Consider downscaling for detection, then refine pose at a higher resolution ROI.

## File Map

- `MainActivity.kt`: UI navigation, camera setup, analyzer, and overlay.
- `CameraPreview.kt`: CameraX preview via `PreviewView`.
- `app/src/main/assets/`: location for QIDK models.

## Next Steps

- Add real posture scoring from keypoints (slouch/lean detection).
- Implement QIDK device backend and a runtime backend switch.
- Persist history to local storage and render real graphs.
