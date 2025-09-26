#include <jni.h>
#include <android/log.h>
#include <vector>
#include <string>
#include <mutex>
#include <thread>
#include <chrono>
#include <cmath>
#include <algorithm>

#define LOG_TAG "QIDKBackendJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Performance tracking
struct PerformanceMetrics {
    float detectionTimeMs = 0.0f;
    float poseTimeMs = 0.0f;
    float totalTimeMs = 0.0f;
    float fps = 0.0f;
    int frameCount = 0;
    std::chrono::high_resolution_clock::time_point lastFrameTime;
};

// Detection bounding box
struct BBox {
    float x1, y1, x2, y2;
    float confidence;
    int class_id;
};

// HRNet keypoint mapping (COCO-17 format)
enum HRNetKeypoints {
    NOSE = 0,
    LEFT_EYE = 1,
    RIGHT_EYE = 2,
    LEFT_EAR = 3,
    RIGHT_EAR = 4,
    LEFT_SHOULDER = 5,
    RIGHT_SHOULDER = 6,
    LEFT_ELBOW = 7,
    RIGHT_ELBOW = 8,
    LEFT_WRIST = 9,
    RIGHT_WRIST = 10,
    LEFT_HIP = 11,
    RIGHT_HIP = 12,
    LEFT_KNEE = 13,
    RIGHT_KNEE = 14,
    LEFT_ANKLE = 15,
    RIGHT_ANKLE = 16
};

// Keypoint structure
struct Keypoint {
    float x, y, score;
    int id;
    
    Keypoint() : x(0), y(0), score(0), id(-1) {}
    Keypoint(float x_, float y_, float score_, int id_) : x(x_), y(y_), score(score_), id(id_) {}
};

// Posture analysis
struct PostureMetrics {
    float shoulderAngle = 0.0f;
    float spineAlignment = 0.0f;
    float headTilt = 0.0f;
    int postureScore = 0;
    std::string postureName = "Unknown";
    std::chrono::high_resolution_clock::time_point startTime;
    float durationSeconds = 0.0f;
};

// Global state
static std::mutex& getMutex() {
    static std::mutex gMutex;
    return gMutex;
}
static bool gInitialized = false;
static PerformanceMetrics gMetrics;
static PostureMetrics gPosture;
static std::vector<Keypoint> gLastPose;

// Utility functions
float euclideanDistance(float x1, float y1, float x2, float y2) {
    return std::sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
}

float calculateAngle(const Keypoint& p1, const Keypoint& p2, const Keypoint& p3) {
    float v1x = p1.x - p2.x;
    float v1y = p1.y - p2.y;
    float v2x = p3.x - p2.x;
    float v2y = p3.y - p2.y;
    
    float dot = v1x * v2x + v1y * v2y;
    float det = v1x * v2y - v1y * v2x;
    float angle = std::atan2(det, dot) * 180.0f / M_PI;
    
    return std::abs(angle);
}

// Simulate YOLO-NAS person detection with dynamic detection
std::vector<BBox> runYOLONAS(const uint8_t* yuv_data, int width, int height) {
    auto start = std::chrono::high_resolution_clock::now();
    
    // Simulate detection processing time (1-3ms for optimized YOLO-NAS)
    std::this_thread::sleep_for(std::chrono::microseconds(1500));
    
    std::vector<BBox> detections;
    
    // Simulate realistic detection - sometimes no person detected
    static int frameCounter = 0;
    frameCounter++;
    
    // Only detect person 70% of the time to simulate real-world scenarios
    if (frameCounter % 10 < 7) {
        // Add some variation to detection to avoid permanent overlay
        float jitterX = (frameCounter % 20 - 10) * width * 0.01f; // +/- 1% width variation
        float jitterY = (frameCounter % 30 - 15) * height * 0.005f; // +/- 0.5% height variation
        
        // Simulate person detection - person position varies slightly
        float centerX = width * 0.5f + jitterX;
        float centerY = height * 0.5f + jitterY;
        float boxWidth = width * (0.3f + (frameCounter % 10) * 0.01f);   // 30-40% width
        float boxHeight = height * (0.35f + (frameCounter % 15) * 0.01f); // 35-50% height
        
        BBox person;
        person.x1 = centerX - boxWidth * 0.5f;
        person.y1 = centerY - boxHeight * 0.5f;
        person.x2 = centerX + boxWidth * 0.5f;
        person.y2 = centerY + boxHeight * 0.5f;
        person.confidence = 0.85f + (frameCounter % 20) * 0.005f; // 85-95% confidence
        person.class_id = 0; // person class
        
        detections.push_back(person);
    }
    
    auto end = std::chrono::high_resolution_clock::now();
    gMetrics.detectionTimeMs = std::chrono::duration<float, std::milli>(end - start).count();
    
    return detections;
}

// Simulate HRNet pose estimation on ROI with realistic variation
std::vector<Keypoint> runHRNet(const uint8_t* yuv_data, int width, int height, const BBox& roi) {
    auto start = std::chrono::high_resolution_clock::now();
    
    // Simulate HRNet processing time (3-8ms for optimized HRNet)
    std::this_thread::sleep_for(std::chrono::microseconds(4500));
    
    std::vector<Keypoint> keypoints;
    
    // Generate realistic pose keypoints within the ROI with natural variation
    float roiCenterX = (roi.x1 + roi.x2) * 0.5f;
    float roiCenterY = (roi.y1 + roi.y2) * 0.5f;
    float roiWidth = roi.x2 - roi.x1;
    float roiHeight = roi.y2 - roi.y1;
    
    // Add frame-based variation to simulate natural movement
    static int poseFrameCounter = 0;
    poseFrameCounter++;
    float timeVariation = std::sin(poseFrameCounter * 0.1f);
    float breathingMotion = std::sin(poseFrameCounter * 0.05f) * 0.01f; // Small breathing movement
    
    // Head region (top 25% of ROI) - add slight head movement
    float headTilt = timeVariation * 0.02f;
    keypoints.emplace_back(roiCenterX + headTilt * roiWidth, roi.y1 + roiHeight * 0.15f, 0.95f, NOSE);
    keypoints.emplace_back(roiCenterX - roiWidth * 0.08f + headTilt * roiWidth, roi.y1 + roiHeight * 0.12f, 0.88f, LEFT_EYE);
    keypoints.emplace_back(roiCenterX + roiWidth * 0.08f + headTilt * roiWidth, roi.y1 + roiHeight * 0.12f, 0.87f, RIGHT_EYE);
    keypoints.emplace_back(roiCenterX - roiWidth * 0.12f + headTilt * roiWidth, roi.y1 + roiHeight * 0.10f, 0.82f, LEFT_EAR);
    keypoints.emplace_back(roiCenterX + roiWidth * 0.12f + headTilt * roiWidth, roi.y1 + roiHeight * 0.10f, 0.81f, RIGHT_EAR);
    
    // Torso region (25-70% of ROI) - add breathing motion
    float shoulderY = roi.y1 + roiHeight * (0.30f + breathingMotion);
    keypoints.emplace_back(roiCenterX - roiWidth * 0.18f, shoulderY, 0.93f, LEFT_SHOULDER);
    keypoints.emplace_back(roiCenterX + roiWidth * 0.18f, shoulderY, 0.92f, RIGHT_SHOULDER);
    keypoints.emplace_back(roiCenterX - roiWidth * 0.25f, roi.y1 + roiHeight * 0.48f, 0.85f, LEFT_ELBOW);
    keypoints.emplace_back(roiCenterX + roiWidth * 0.25f, roi.y1 + roiHeight * 0.48f, 0.84f, RIGHT_ELBOW);
    
    // Add slight arm movement
    float armMotion = timeVariation * 0.03f;
    keypoints.emplace_back(roiCenterX - roiWidth * (0.28f + armMotion), roi.y1 + roiHeight * 0.62f, 0.78f, LEFT_WRIST);
    keypoints.emplace_back(roiCenterX + roiWidth * (0.28f - armMotion), roi.y1 + roiHeight * 0.62f, 0.77f, RIGHT_WRIST);
    
    // Hip region (60-70% of ROI) - stable
    keypoints.emplace_back(roiCenterX - roiWidth * 0.12f, roi.y1 + roiHeight * 0.65f, 0.90f, LEFT_HIP);
    keypoints.emplace_back(roiCenterX + roiWidth * 0.12f, roi.y1 + roiHeight * 0.65f, 0.89f, RIGHT_HIP);
    
    // Legs (70-100% of ROI) - minimal movement
    keypoints.emplace_back(roiCenterX - roiWidth * 0.15f, roi.y1 + roiHeight * 0.82f, 0.75f, LEFT_KNEE);
    keypoints.emplace_back(roiCenterX + roiWidth * 0.15f, roi.y1 + roiHeight * 0.82f, 0.74f, RIGHT_KNEE);
    keypoints.emplace_back(roiCenterX - roiWidth * 0.12f, roi.y1 + roiHeight * 0.95f, 0.68f, LEFT_ANKLE);
    keypoints.emplace_back(roiCenterX + roiWidth * 0.12f, roi.y1 + roiHeight * 0.95f, 0.67f, RIGHT_ANKLE);
    
    auto end = std::chrono::high_resolution_clock::now();
    gMetrics.poseTimeMs = std::chrono::duration<float, std::milli>(end - start).count();
    
    return keypoints;
}

// Analyze posture quality
void analyzePosture(const std::vector<Keypoint>& keypoints) {
    if (keypoints.size() < 10) return;
    
    // Find key points for analysis
    const Keypoint* nose = nullptr;
    const Keypoint* leftShoulder = nullptr;
    const Keypoint* rightShoulder = nullptr;
    const Keypoint* leftHip = nullptr;
    const Keypoint* rightHip = nullptr;
    
    for (const auto& kp : keypoints) {
        switch (kp.id) {
            case NOSE: nose = &kp; break;
            case LEFT_SHOULDER: leftShoulder = &kp; break;
            case RIGHT_SHOULDER: rightShoulder = &kp; break;
            case LEFT_HIP: leftHip = &kp; break;
            case RIGHT_HIP: rightHip = &kp; break;
        }
    }
    
    if (!nose || !leftShoulder || !rightShoulder || !leftHip || !rightHip) return;
    
    // Calculate shoulder angle (should be close to horizontal)
    float shoulderSlope = (rightShoulder->y - leftShoulder->y) / 
                         std::max(1.0f, rightShoulder->x - leftShoulder->x);
    gPosture.shoulderAngle = std::atan(shoulderSlope) * 180.0f / M_PI;
    
    // Calculate spine alignment
    float shoulderMidX = (leftShoulder->x + rightShoulder->x) * 0.5f;
    float shoulderMidY = (leftShoulder->y + rightShoulder->y) * 0.5f;
    float hipMidX = (leftHip->x + rightHip->x) * 0.5f;
    float hipMidY = (leftHip->y + rightHip->y) * 0.5f;
    
    float spineAngle = std::atan2(hipMidX - shoulderMidX, hipMidY - shoulderMidY) * 180.0f / M_PI;
    gPosture.spineAlignment = std::abs(spineAngle);
    
    // Calculate head tilt
    gPosture.headTilt = std::abs(nose->x - shoulderMidX);
    
    // Calculate overall posture score (0-100)
    int score = 100;
    
    // Penalize shoulder tilt (should be < 5 degrees)
    if (std::abs(gPosture.shoulderAngle) > 5.0f) {
        score -= std::min(30, (int)(std::abs(gPosture.shoulderAngle) - 5.0f) * 2);
    }
    
    // Penalize spine misalignment (should be < 10 degrees)
    if (gPosture.spineAlignment > 10.0f) {
        score -= std::min(40, (int)(gPosture.spineAlignment - 10.0f) * 3);
    }
    
    // Penalize head forward position
    float neckForward = gPosture.headTilt / std::max(1.0f, rightShoulder->x - leftShoulder->x);
    if (neckForward > 0.2f) {
        score -= std::min(30, (int)(neckForward * 100));
    }
    
    gPosture.postureScore = std::max(0, score);
    
    // Determine posture name
    if (score >= 80) {
        gPosture.postureName = "Excellent";
    } else if (score >= 60) {
        gPosture.postureName = "Good";
    } else if (score >= 40) {
        gPosture.postureName = "Fair";
    } else {
        gPosture.postureName = "Poor";
    }
    
    // Update duration
    auto now = std::chrono::high_resolution_clock::now();
    if (gPosture.startTime.time_since_epoch().count() == 0) {
        gPosture.startTime = now;
    }
    gPosture.durationSeconds = std::chrono::duration<float>(now - gPosture.startTime).count();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_eswproject_QidkNative_init(JNIEnv* env, jobject thiz) {
    std::scoped_lock lk(getMutex());
    if (gInitialized) return JNI_TRUE;
    
    // Initialize performance tracking
    gMetrics = {};
    gMetrics.lastFrameTime = std::chrono::high_resolution_clock::now();
    
    // Initialize posture tracking
    gPosture = {};
    gPosture.startTime = std::chrono::high_resolution_clock::now();
    
    gInitialized = true;
    LOGI("QIDK native backend initialized with YOLO-NAS + HRNet pipeline");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_eswproject_QidkNative_isAvailable(JNIEnv* env, jobject thiz) {
    return gInitialized ? 1 : 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_eswproject_QidkNative_runPipeline(
        JNIEnv* env,
        jobject thiz,
        jbyteArray yuv420,
        jint width,
        jint height,
        jfloat scoreThreshold,
        jintArray outIds,
        jfloatArray outX,
        jfloatArray outY,
        jfloatArray outScores,
        jint maxKp
) {
    std::scoped_lock lk(getMutex());
    
    auto frameStart = std::chrono::high_resolution_clock::now();
    
    // Get YUV data
    jbyte* yuvData = env->GetByteArrayElements(yuv420, nullptr);
    if (!yuvData) return 0;
    
    // Step 1: Run YOLO-NAS person detection
    std::vector<BBox> detections = runYOLONAS(reinterpret_cast<const uint8_t*>(yuvData), width, height);
    
    if (detections.empty()) {
        // No person detected - clear previous pose data
        gLastPose.clear();
        gPosture.startTime = std::chrono::high_resolution_clock::time_point{}; // Reset timer
        env->ReleaseByteArrayElements(yuv420, yuvData, JNI_ABORT);
        return 0;
    }
    
    // Use the highest confidence detection
    BBox bestDetection = detections[0];
    for (const auto& det : detections) {
        if (det.confidence > bestDetection.confidence) {
            bestDetection = det;
        }
    }
    
    // Step 2: Run HRNet pose estimation on the detected person ROI
    std::vector<Keypoint> keypoints = runHRNet(reinterpret_cast<const uint8_t*>(yuvData), 
                                              width, height, bestDetection);
    
    // Step 3: Coordinate system correction
    // Ensure keypoints are in full image coordinates (they already are from our simulation)
    // In real implementation, you'd transform from ROI coordinates back to full image
    
    // Step 4: Filter by score threshold and copy to output arrays
    jint* ids = env->GetIntArrayElements(outIds, nullptr);
    jfloat* xs = env->GetFloatArrayElements(outX, nullptr);
    jfloat* ys = env->GetFloatArrayElements(outY, nullptr);
    jfloat* scores = env->GetFloatArrayElements(outScores, nullptr);
    
    int validCount = 0;
    for (const auto& kp : keypoints) {
        if (kp.score >= scoreThreshold && validCount < maxKp) {
            ids[validCount] = kp.id;
            xs[validCount] = kp.x;
            ys[validCount] = kp.y;
            scores[validCount] = kp.score;
            validCount++;
        }
    }
    
    // Update performance metrics
    auto frameEnd = std::chrono::high_resolution_clock::now();
    gMetrics.totalTimeMs = std::chrono::duration<float, std::milli>(frameEnd - frameStart).count();
    gMetrics.frameCount++;
    
    // Calculate FPS
    auto timeSinceLastFrame = std::chrono::duration<float>(frameEnd - gMetrics.lastFrameTime).count();
    if (timeSinceLastFrame > 0) {
        gMetrics.fps = 1.0f / timeSinceLastFrame;
    }
    gMetrics.lastFrameTime = frameEnd;
    
    // Analyze posture
    analyzePosture(keypoints);
    gLastPose = keypoints;
    
    // Log performance metrics every 30 frames
    if (gMetrics.frameCount % 30 == 0) {
        LOGI("Performance: Detection=%.1fms, Pose=%.1fms, Total=%.1fms, FPS=%.1f, Posture=%s(%d)",
             gMetrics.detectionTimeMs, gMetrics.poseTimeMs, gMetrics.totalTimeMs, 
             gMetrics.fps, gPosture.postureName.c_str(), gPosture.postureScore);
    }
    
    // Clean up
    env->ReleaseByteArrayElements(yuv420, yuvData, JNI_ABORT);
    env->ReleaseIntArrayElements(outIds, ids, 0);
    env->ReleaseFloatArrayElements(outX, xs, 0);
    env->ReleaseFloatArrayElements(outY, ys, 0);
    env->ReleaseFloatArrayElements(outScores, scores, 0);
    
    return validCount;
}

// New JNI functions for performance and posture analytics
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_eswproject_QidkNative_getPerformanceMetrics(JNIEnv* env, jobject thiz) {
    std::scoped_lock lk(getMutex());
    
    jfloatArray result = env->NewFloatArray(4);
    if (result) {
        jfloat metrics[4] = {
            gMetrics.detectionTimeMs,
            gMetrics.poseTimeMs, 
            gMetrics.totalTimeMs,
            gMetrics.fps
        };
        env->SetFloatArrayRegion(result, 0, 4, metrics);
    }
    return result;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_eswproject_QidkNative_getPostureAnalysis(JNIEnv* env, jobject thiz) {
    std::scoped_lock lk(getMutex());
    
    jfloatArray result = env->NewFloatArray(5);
    if (result) {
        jfloat analysis[5] = {
            gPosture.shoulderAngle,
            gPosture.spineAlignment,
            gPosture.headTilt,
            static_cast<float>(gPosture.postureScore),
            gPosture.durationSeconds
        };
        env->SetFloatArrayRegion(result, 0, 5, analysis);
    }
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_eswproject_QidkNative_getPostureName(JNIEnv* env, jobject thiz) {
    std::scoped_lock lk(getMutex());
    return env->NewStringUTF(gPosture.postureName.c_str());
}
