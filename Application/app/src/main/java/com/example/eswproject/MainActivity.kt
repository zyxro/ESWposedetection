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

                val appContext = context.applicationContext
                val cameraController = remember {
                    LifecycleCameraController(context).apply {
                        setImageAnalysisAnalyzer(Dispatchers.Default.asExecutor(), FrameAnalyzer(appContext))
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
class FrameAnalyzer(private val appContext: android.content.Context) : ImageAnalysis.Analyzer {
    private val detector by lazy {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    private var poseEngine: PoseEngine? = null

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
                // Fall through
            }
        }

        // Try TensorFlow Lite HRNet fallback if present
        try {
            if (poseEngine == null) {
                poseEngine = PoseEngine(appContext).also { it.load() }
            }
            val engine = poseEngine
            if (engine != null && engine.isReady()) {
                val nv21 = yuv420ToNV21(mediaImage)
                val roi = engine.detectPerson(width, height)
                val kp = engine.runHrnet(nv21, width, height, roi)
                if (kp.isNotEmpty()) {
                    val map = mutableMapOf<Int, Keypoint>()
                    // Map HRNet COCO order directly to overlay ML Kit ids subset
                    val ids = arrayOf(
                        PoseLandmark.NOSE,
                        PoseLandmark.LEFT_EYE,
                        PoseLandmark.RIGHT_EYE,
                        PoseLandmark.LEFT_EAR,
                        PoseLandmark.RIGHT_EAR,
                        PoseLandmark.LEFT_SHOULDER,
                        PoseLandmark.RIGHT_SHOULDER,
                        PoseLandmark.LEFT_ELBOW,
                        PoseLandmark.RIGHT_ELBOW,
                        PoseLandmark.LEFT_WRIST,
                        PoseLandmark.RIGHT_WRIST,
                        PoseLandmark.LEFT_HIP,
                        PoseLandmark.RIGHT_HIP,
                        PoseLandmark.LEFT_KNEE,
                        PoseLandmark.RIGHT_KNEE,
                        PoseLandmark.LEFT_ANKLE,
                        PoseLandmark.RIGHT_ANKLE
                    )
                    for (i in ids.indices) {
                        val (x, y, s) = kp[i]
                        map[ids[i]] = Keypoint(x, y, s)
                    }
                    val rotated = rotateKeypoints(map, width, height, rotation)
                    val out = PoseOverlay(rotated.first, rotated.second.first, rotated.second.second, isFrontCamera = false)
                    _state.value = out
                    image.close()
                    return
                }
            }
        } catch (_: Throwable) {
            // ignore and fall back
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
