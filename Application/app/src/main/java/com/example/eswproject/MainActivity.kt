package com.example.eswproject

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.PointF
import android.media.Image
import android.os.Bundle
import android.util.Size
import androidx.annotation.Keep
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.eswproject.ui.theme.ESWProjectTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import java.text.SimpleDateFormat
import java.util.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.pose.PoseLandmark

sealed class Screen(val route: String, val title: String)
object HomeScreenRoute : Screen("home", "Home")
object CameraScreenRoute : Screen("camera", "Camera")
object HistoryScreenRoute : Screen("history", "History")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ESWProjectTheme {
                val context = LocalContext.current
                var hasPermissions by remember {
                    mutableStateOf(
                        CAMERAX_PERMISSIONS.all {
                            ContextCompat.checkSelfPermission(
                                context,
                                it
                            ) == PackageManager.PERMISSION_GRANTED
                        }
                    )
                }
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    hasPermissions = permissions.values.all { it }
                }

                LaunchedEffect(Unit) {
                    if (!hasPermissions) {
                        launcher.launch(CAMERAX_PERMISSIONS)
                    }
                }

                val cameraController = remember {
                    LifecycleCameraController(context).apply {
                        setImageAnalysisAnalyzer(Dispatchers.Default.asExecutor(), FrameAnalyzer())
                        bindToLifecycle(this@MainActivity)
                    }
                }

                val analyzerState = remember { FrameAnalyzer.state }
                var showSkeleton by remember { mutableStateOf(true) }
                
                // Analytics state
                var performanceMetrics by remember { mutableStateOf<PerformanceMetrics?>(null) }
                var postureAnalysis by remember { mutableStateOf<PostureAnalysis?>(null) }
                
                // Update analytics periodically
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(500) // Update analytics every 500ms
                        if (QidkBackends.isAvailable()) {
                            performanceMetrics = QidkBackends.getPerformanceMetrics()
                            postureAnalysis = QidkBackends.getPostureAnalysis()
                        }
                    }
                }

                val demoData = remember {
                    val now = System.currentTimeMillis()
                    mutableStateListOf(
                        Triple("Sitting Upright", 95, now - 15000L),
                        Triple("Slouching", 45, now - 12000L),
                        Triple("Leaning Left", 75, now - 9000L),
                        Triple("Leaning Right", 70, now - 6000L),
                        Triple("Slouching", 55, now - 3000L),
                        Triple("Sitting Upright", 85, now)
                    )
                }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(3000) // Change posture every 3 seconds
                        val (pose, score, _) = demoData.removeAt(0)
                        demoData.add(Triple(pose, score, System.currentTimeMillis()))
                    }
                }

                var currentScreen by remember { mutableStateOf<Screen>(HomeScreenRoute) }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                selected = currentScreen.route == HomeScreenRoute.route,
                                onClick = { currentScreen = HomeScreenRoute }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Videocam, contentDescription = "Camera") },
                                label = { Text("Camera") },
                                selected = currentScreen.route == CameraScreenRoute.route,
                                onClick = { currentScreen = CameraScreenRoute }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                                label = { Text("History") },
                                selected = currentScreen.route == HistoryScreenRoute.route,
                                onClick = { currentScreen = HistoryScreenRoute }
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        if (hasPermissions) {
                            when (currentScreen) {
                                HomeScreenRoute -> HomeScreen(
                                    pose = postureAnalysis?.postureName ?: demoData.last().first,
                                    score = postureAnalysis?.score ?: demoData.last().second,
                                    postureAnalysis = postureAnalysis,
                                    performanceMetrics = performanceMetrics
                                )
                                CameraScreenRoute -> CameraScreen(
                                    controller = cameraController,
                                    pose = postureAnalysis?.postureName ?: demoData.last().first,
                                    score = postureAnalysis?.score ?: demoData.last().second,
                                    overlay = if (showSkeleton) analyzerState.collectAsState().value else null,
                                    showSkeleton = showSkeleton,
                                    onToggleSkeleton = { showSkeleton = !showSkeleton },
                                    postureAnalysis = postureAnalysis,
                                    performanceMetrics = performanceMetrics
                                )
                                HistoryScreenRoute -> HistoryScreen(
                                    data = demoData.toList(),
                                    postureAnalysis = postureAnalysis
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Please grant camera permissions to use the app.")
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    }
}

@Composable
fun HomeScreen(
    pose: String,
    score: Int,
    postureAnalysis: PostureAnalysis? = null,
    performanceMetrics: PerformanceMetrics? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Welcome Back!",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            if (QidkBackends.isAvailable()) "QIDK Backend Active" else "ML Kit Backend Active",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Main Score Display with enhanced info
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp) // Larger circle
                .padding(bottom = 16.dp)
        ) {
            CircularProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 20.dp, // Thicker stroke
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = when {
                    score > 80 -> Color(0xFF4CAF50)
                    score > 60 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                },
                strokeCap = StrokeCap.Round // Rounded ends
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Score",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Current Pose Text
        Text(
            text = pose,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Good posture hint
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = "Info", modifier = Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Column {
                    Text("Good posture is key!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text("Aim for a score above 80 to maintain a healthy posture.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Performance Analytics (only show if QIDK backend is active)
        if (QidkBackends.isAvailable() && performanceMetrics != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Speed, contentDescription = "Performance", 
                             modifier = Modifier.padding(end = 8.dp),
                             tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Performance Metrics", 
                             style = MaterialTheme.typography.titleMedium,
                             color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("FPS", style = MaterialTheme.typography.labelMedium)
                            Text("%.1f".format(performanceMetrics.fps), 
                                 style = MaterialTheme.typography.bodyLarge,
                                 fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Total Time", style = MaterialTheme.typography.labelMedium)
                            Text("%.1f ms".format(performanceMetrics.totalTimeMs), 
                                 style = MaterialTheme.typography.bodyLarge,
                                 fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Detection", style = MaterialTheme.typography.labelMedium)
                            Text("%.1f ms".format(performanceMetrics.detectionTimeMs), 
                                 style = MaterialTheme.typography.bodyLarge,
                                 fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Posture Analysis Details
        if (postureAnalysis != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Analytics, contentDescription = "Analysis", 
                             modifier = Modifier.padding(end = 8.dp),
                             tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Posture Analysis", 
                             style = MaterialTheme.typography.titleMedium,
                             color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Duration", style = MaterialTheme.typography.labelMedium)
                            Text("%.1f s".format(postureAnalysis.durationSeconds), 
                                 style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Shoulder Angle", style = MaterialTheme.typography.labelMedium)
                            Text("%.1f°".format(postureAnalysis.shoulderAngle), 
                                 style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Spine Alignment", style = MaterialTheme.typography.labelMedium)
                            Text("%.1f°".format(postureAnalysis.spineAlignment), 
                                 style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
fun CameraScreen(
    controller: LifecycleCameraController,
    pose: String,
    score: Int,
    overlay: PoseOverlay?,
    showSkeleton: Boolean,
    onToggleSkeleton: () -> Unit,
    postureAnalysis: PostureAnalysis? = null,
    performanceMetrics: PerformanceMetrics? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            controller = controller,
            modifier = Modifier.fillMaxSize()
        )
        if (overlay != null && showSkeleton) {
            PoseOverlayComposable(overlay)
        }
        // Performance overlay (top-right)
        if (QidkBackends.isAvailable() && performanceMetrics != null) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Column(modifier = Modifier.align(Alignment.TopEnd)) {
                    AssistChip(onClick = {}, label = { Text(text = "%.1f FPS".format(performanceMetrics.fps)) })
                    Spacer(Modifier.height(8.dp))
                    AssistChip(onClick = {}, label = { Text(text = "%.1f ms".format(performanceMetrics.totalTimeMs)) })
                }
            }
        }
    }
}

// Pose overlay data
data class Keypoint(val x: Float, val y: Float, val score: Float)
data class PoseOverlay(
    val keypoints: Map<Int, Keypoint>,
    val width: Int,
    val height: Int,
    val isFrontCamera: Boolean
)

@Composable
fun PoseOverlayComposable(pose: PoseOverlay?) {
    if (pose == null) return
    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (pose.width == 0 || pose.height == 0) return@Canvas
        val scaleX = size.width / pose.width
        val scaleY = size.height / pose.height
        val scale = min(scaleX, scaleY)
        val contentW = pose.width * scale
        val contentH = pose.height * scale
        val dx = (size.width - contentW) / 2f
        val dy = (size.height - contentH) / 2f

        fun mapX(x: Float): Float {
            val v = x * scale
            return if (pose.isFrontCamera) dx + (contentW - v) else dx + v
        }
        fun mapY(y: Float): Float = dy + y * scale

        val connections = listOf(
            // torso
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            // arms
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            // legs
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
            // head
            PoseLandmark.LEFT_EYE to PoseLandmark.RIGHT_EYE,
            PoseLandmark.NOSE to PoseLandmark.LEFT_EYE,
            PoseLandmark.NOSE to PoseLandmark.RIGHT_EYE,
            PoseLandmark.LEFT_EAR to PoseLandmark.LEFT_EYE,
            PoseLandmark.RIGHT_EAR to PoseLandmark.RIGHT_EYE
        )

        val lineColor = Color(0xFF00E5FF)
        val pointColor = Color(0xFFFF4081)

        connections.forEach { (a, b) ->
            val ka = pose.keypoints[a]
            val kb = pose.keypoints[b]
            if (ka != null && kb != null && ka.score > 0.4f && kb.score > 0.4f) {
                drawLine(
                    color = lineColor,
                    start = Offset(mapX(ka.x), mapY(ka.y)),
                    end = Offset(mapX(kb.x), mapY(kb.y)),
                    strokeWidth = 6f
                )
            }
        }
        pose.keypoints.values.forEach { kp ->
            if (kp.score > 0.4f) {
                drawCircle(pointColor, radius = 8f, center = Offset(mapX(kp.x), mapY(kp.y)))
            }
        }
    }
}

// Analyzer: YOLO-NAS (person) + HRNet (pose)
// For development we use ML Kit pose; define a QIDK stub for later device integration.
class FrameAnalyzer : ImageAnalysis.Analyzer {
    private val detector by lazy {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    companion object {
        private val _state = MutableStateFlow<PoseOverlay?>(null)
        val state = _state.asStateFlow()
    }

    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image ?: run { image.close(); return }
        val rotation = image.imageInfo.rotationDegrees
        val width = image.width
        val height = image.height

        // Attempt QNN path first if available
        if (QidkBackends.isAvailable()) {
            try {
                val nv21 = yuv420ToNV21(mediaImage)
                val pose = QidkBackends.detectAndPose(nv21, width, height, rotation, isFront = false)
                if (pose != null) {
                    _state.value = pose
                    image.close()
                    return
                }
            } catch (_: Throwable) {
                // Fall through to ML Kit
            }
        }

        val img = InputImage.fromMediaImage(mediaImage, rotation)
        detector.process(img)
            .addOnSuccessListener { pose ->
                val map = mutableMapOf<Int, Keypoint>()
                pose.allPoseLandmarks.forEach { l ->
                    map[l.landmarkType] = Keypoint(l.position.x, l.position.y, l.inFrameLikelihood)
                }
                _state.value = PoseOverlay(map, img.width, img.height, isFrontCamera = false)
            }
            .addOnFailureListener {
                // ignore
            }
            .addOnCompleteListener {
                image.close()
            }
    }
}

// Enhanced QidkBackends with analytics support
@Keep
object QidkBackends {
    private var native: QidkNative? = null
    private var checked = false
    private var available = false

    // State for temporal smoothing
    private var lastMap: MutableMap<Int, Keypoint>? = null
    private var lastDims: Pair<Int, Int>? = null

    fun ensureInit() {
        if (checked) return
        checked = true
        try {
            val n = QidkNative()
            if (n.init()) {
                native = n
                available = (n.isAvailable() == 1)
            }
        } catch (_: Throwable) {
            available = false
        }
    }

    fun isAvailable(): Boolean {
        ensureInit()
        return available
    }

    // Accept YUV420 (NV21) bytes and produce PoseOverlay or null
    fun detectAndPose(yuv: ByteArray, width: Int, height: Int, rotationDegrees: Int, isFront: Boolean): PoseOverlay? {
        val n = native ?: return null
        val maxKp = 40 // Enough for HRNet variants
        val ids = IntArray(maxKp)
        val xs = FloatArray(maxKp)
        val ys = FloatArray(maxKp)
        val scores = FloatArray(maxKp)
        val count = try { n.runPipeline(yuv, width, height, 0.25f, ids, xs, ys, scores, maxKp) } catch (_: Throwable) { 0 }
        if (count <= 0) {
            // reset smoothing when no detections
            lastMap = null
            lastDims = null
            return null
        }
        
        // Map HRNet COCO-17 keypoint IDs to ML Kit PoseLandmark constants
        val hrnetToMLKit = mapOf(
            0 to PoseLandmark.NOSE,
            1 to PoseLandmark.LEFT_EYE,
            2 to PoseLandmark.RIGHT_EYE,
            3 to PoseLandmark.LEFT_EAR,
            4 to PoseLandmark.RIGHT_EAR,
            5 to PoseLandmark.LEFT_SHOULDER,
            6 to PoseLandmark.RIGHT_SHOULDER,
            7 to PoseLandmark.LEFT_ELBOW,
            8 to PoseLandmark.RIGHT_ELBOW,
            9 to PoseLandmark.LEFT_WRIST,
            10 to PoseLandmark.RIGHT_WRIST,
            11 to PoseLandmark.LEFT_HIP,
            12 to PoseLandmark.RIGHT_HIP,
            13 to PoseLandmark.LEFT_KNEE,
            14 to PoseLandmark.RIGHT_KNEE,
            15 to PoseLandmark.LEFT_ANKLE,
            16 to PoseLandmark.RIGHT_ANKLE
        )
        
        // Populate keypoints map (raw)
        val rawMap = mutableMapOf<Int, Keypoint>()
        for (i in 0 until count) {
            val mlkitId = hrnetToMLKit[ids[i]]
            if (mlkitId != null) {
                rawMap[mlkitId] = Keypoint(xs[i], ys[i], scores[i])
            }
        }

        // Rotate keypoints into upright preview space
        val rotated = rotateKeypoints(rawMap, width, height, rotationDegrees)
        val rotatedMap = rotated.first
        val outW = rotated.second.first
        val outH = rotated.second.second

        // Confidence gating: require at least 5 reliable joints
        val reliable = rotatedMap.values.count { it.score >= 0.5f }
        if (reliable < 5) {
            lastMap = null
            lastDims = null
            return null
        }

        // Temporal smoothing (EMA)
        val alpha = 0.7f // previous weight
        val smoothed = mutableMapOf<Int, Keypoint>()
        if (lastMap != null && lastDims == Pair(outW, outH)) {
            val prev = lastMap!!
            for ((id, curr) in rotatedMap) {
                val p = prev[id]
                if (p != null) {
                    val sx = alpha * p.x + (1 - alpha) * curr.x
                    val sy = alpha * p.y + (1 - alpha) * curr.y
                    val ss = alpha * p.score + (1 - alpha) * curr.score
                    smoothed[id] = Keypoint(sx, sy, ss)
                } else {
                    smoothed[id] = curr
                }
            }
        } else {
            smoothed.putAll(rotatedMap)
        }

        // Update state
        lastMap = smoothed.toMutableMap()
        lastDims = Pair(outW, outH)

        return PoseOverlay(smoothed, outW, outH, isFrontCamera = isFront)
    }

    // Get performance analytics
    fun getPerformanceMetrics(): PerformanceMetrics? {
        val n = native ?: return null
        return try {
            val metrics = n.getPerformanceMetrics()
            if (metrics.size >= 4) {
                PerformanceMetrics(metrics[0], metrics[1], metrics[2], metrics[3])
            } else null
        } catch (_: Throwable) { null }
    }
    
    // Get posture analysis
    fun getPostureAnalysis(): PostureAnalysis? {
        val n = native ?: return null
        return try {
            val analysis = n.getPostureAnalysis()
            val postureName = n.getPostureName()
            if (analysis.size >= 5) {
                PostureAnalysis(
                    analysis[0], analysis[1], analysis[2], 
                    analysis[3].toInt(), analysis[4], postureName
                )
            } else null
        } catch (_: Throwable) { null }
    }
}

// JNI native wrapper with enhanced analytics
class QidkNative {
    companion object {
        init {
            try {
                System.loadLibrary("qidk_backend")
            } catch (e: Throwable) {
                // Library not present yet
            }
        }
    }
    external fun init(): Boolean
    external fun isAvailable(): Int
    external fun runPipeline(
        yuv420: ByteArray,
        width: Int,
        height: Int,
        scoreThreshold: Float,
        outIds: IntArray,
        outX: FloatArray,
        outY: FloatArray,
        outScores: FloatArray,
        maxKp: Int
    ): Int
    
    // New analytics functions
    external fun getPerformanceMetrics(): FloatArray // [detectionMs, poseMs, totalMs, fps]
    external fun getPostureAnalysis(): FloatArray   // [shoulderAngle, spineAlignment, headTilt, score, duration]  
    external fun getPostureName(): String
}

// Performance and posture data classes
data class PerformanceMetrics(
    val detectionTimeMs: Float,
    val poseTimeMs: Float,
    val totalTimeMs: Float,
    val fps: Float
)

data class PostureAnalysis(
    val shoulderAngle: Float,
    val spineAlignment: Float, 
    val headTilt: Float,
    val score: Int,
    val durationSeconds: Float,
    val postureName: String
)

@Composable
fun HistoryScreen(
    data: List<Triple<String, Int, Long>>,
    postureAnalysis: PostureAnalysis? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Posture History", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 24.dp))
        
        // Current posture info if available
        postureAnalysis?.let { analysis ->
            PostureInfoCard(
                pose = analysis.postureName,
                score = analysis.score,
                postureAnalysis = analysis
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        PostureGraph(data.map { it.second to it.third }, modifier = Modifier.fillMaxWidth().height(300.dp))
    }
}

@Composable
fun PostureGraph(data: List<Pair<Int, Long>>, modifier: Modifier = Modifier) {
    val themeColors = MaterialTheme.colorScheme
    val lineColor = themeColors.primary
    val pointColor = themeColors.secondary
    val textColor = themeColors.onSurface
    val gradientStartColor = themeColors.primary.copy(alpha = 0.4f)
    val gradientEndColor = themeColors.primary.copy(alpha = 0.0f)
    val gridColor = themeColors.onSurface.copy(alpha = 0.2f)

    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bottomPadding = 90f // for labels
            val leftPadding = 100f // for Y axis labels
            val graphWidth = size.width - leftPadding
            val graphHeight = size.height - bottomPadding

            if (data.size < 2) {
                // Handle case with not enough data
                return@Canvas
            }

            // Draw Y-axis labels and grid lines
            val yAxisLabels = listOf(0, 50, 80, 100)
            val textPaint = Paint().apply {
                color = textColor.toArgb()
                textSize = 35f
                textAlign = Paint.Align.RIGHT
            }
            yAxisLabels.forEach { label ->
                val y = graphHeight - (graphHeight * (label / 100f))
                drawContext.canvas.nativeCanvas.drawText(
                    label.toString(),
                    leftPadding - 20,
                    y + (textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent) / 2,
                    textPaint
                )
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            val points = data.mapIndexed { index, (value, _) ->
                PointF(
                    leftPadding + (graphWidth / (data.size - 1)) * index,
                    graphHeight - (graphHeight * (value / 100f))
                )
            }

            // Smooth line path
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val controlPoint1 = PointF((p1.x + p2.x) / 2f, p1.y)
                    val controlPoint2 = PointF((p1.x + p2.x) / 2f, p2.y)
                    cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        p2.x, p2.y
                    )
                }
            }

            // Gradient fill path
            val fillPath = Path().apply {
                moveTo(points.first().x, graphHeight)
                lineTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val controlPoint1 = PointF((p1.x + p2.x) / 2f, p1.y)
                    val controlPoint2 = PointF((p1.x + p2.x) / 2f, p2.y)
                    cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        p2.x, p2.y
                    )
                }
                lineTo(points.last().x, graphHeight)
                close()
            }

            // Draw gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(gradientStartColor, gradientEndColor),
                    startY = 0f,
                    endY = graphHeight
                )
            )

            // Draw smooth line
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 8f)
            )

            // Draw points
            points.forEach {
                drawCircle(
                    color = pointColor,
                    radius = 12f,
                    center = Offset(it.x, it.y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(it.x, it.y)
                )
            }

            // Draw timestamps
            val timestampTextPaint = Paint().apply {
                color = textColor.toArgb()
                textSize = 35f
                textAlign = Paint.Align.CENTER
            }
            data.forEachIndexed { index, (_, timestamp) ->
                val x = leftPadding + (graphWidth / (data.size - 1)) * index
                drawContext.canvas.nativeCanvas.drawText(
                    dateFormat.format(Date(timestamp)),
                    x,
                    size.height - timestampTextPaint.fontMetrics.descent, // Position just above the bottom edge
                    timestampTextPaint
                )
            }
        }
    }
}

@Composable
fun PostureInfoCard(
    pose: String, 
    score: Int, 
    postureAnalysis: PostureAnalysis? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "CURRENT POSTURE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = pose,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Show duration if available
                if (postureAnalysis != null && postureAnalysis.durationSeconds > 0) {
                    Text(
                        text = "Held for %.1f seconds".format(postureAnalysis.durationSeconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 6.dp,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        color = when {
                            score > 80 -> Color(0xFF4CAF50) // Green
                            score > 60 -> Color(0xFFFFC107) // Amber
                            else -> Color(0xFFF44336)       // Red
                        }
                    )
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- Utilities: YUV conversion and keypoint rotation ---
private fun yuv420ToNV21(image: Image): ByteArray {
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]
    val ySize = yPlane.buffer.remaining()
    val uSize = uPlane.buffer.remaining()
    val vSize = vPlane.buffer.remaining()

    val nv21 = ByteArray(image.width * image.height * 3 / 2)

    // Copy Y with row stride consideration
    var outIndex = 0
    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride
    val yBuffer = yPlane.buffer
    val width = image.width
    val height = image.height
    // Ensure absolute reads start from 0
    yBuffer.rewind()
    for (row in 0 until height) {
        var col = 0
        var yPos = row * yRowStride
        while (col < width) {
            nv21[outIndex++] = yBuffer.get(yPos)
            yPos += yPixelStride
            col++
        }
    }

    // Interleave VU (NV21) from U and V planes with stride handling
    val uRowStride = uPlane.rowStride
    val vRowStride = vPlane.rowStride
    val uPixelStride = uPlane.pixelStride
    val vPixelStride = vPlane.pixelStride
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer

    val chromaHeight = height / 2
    val chromaWidth = width / 2

    // Position buffers at start
    uBuffer.rewind()
    vBuffer.rewind()

    for (row in 0 until chromaHeight) {
        var uPos = row * uRowStride
        var vPos = row * vRowStride
        for (col in 0 until chromaWidth) {
            // NV21 expects V then U
            nv21[outIndex++] = vBuffer.get(vPos)
            nv21[outIndex++] = uBuffer.get(uPos)
            uPos += uPixelStride
            vPos += vPixelStride
        }
    }

    return nv21
}

private fun rotateKeypoints(
    keypoints: Map<Int, Keypoint>,
    width: Int,
    height: Int,
    rotationDegrees: Int
): Pair<MutableMap<Int, Keypoint>, Pair<Int, Int>> {
    if (rotationDegrees % 360 == 0) return Pair(keypoints.toMutableMap(), Pair(width, height))

    val out = mutableMapOf<Int, Keypoint>()
    when ((rotationDegrees % 360 + 360) % 360) {
        90 -> {
            // (x, y) -> (y, width - x)
            keypoints.forEach { (id, kp) ->
                val nx = kp.y
                val ny = (width - 1) - kp.x
                out[id] = Keypoint(nx, ny, kp.score)
            }
            return Pair(out, Pair(height, width))
        }
        180 -> {
            // (x, y) -> (width - x, height - y)
            keypoints.forEach { (id, kp) ->
                val nx = (width - 1) - kp.x
                val ny = (height - 1) - kp.y
                out[id] = Keypoint(nx, ny, kp.score)
            }
            return Pair(out, Pair(width, height))
        }
        270 -> {
            // (x, y) -> (height - y, x)
            keypoints.forEach { (id, kp) ->
                val nx = (height - 1) - kp.y
                val ny = kp.x
                out[id] = Keypoint(nx, ny, kp.score)
            }
            return Pair(out, Pair(height, width))
        }
        else -> {
            // Fallback no-rotation
            return Pair(keypoints.toMutableMap(), Pair(width, height))
        }
    }
}