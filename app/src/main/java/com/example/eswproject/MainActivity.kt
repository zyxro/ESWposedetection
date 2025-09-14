package com.example.eswproject

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.eswproject.ui.theme.ESWProjectTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

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
                        bindToLifecycle(this@MainActivity)
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
                                    pose = demoData.last().first,
                                    score = demoData.last().second,
                                )
                                CameraScreenRoute -> CameraScreen(
                                    controller = cameraController,
                                    pose = demoData.last().first,
                                    score = demoData.last().second,
                                )
                                HistoryScreenRoute -> HistoryScreen(demoData.toList())
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
    score: Int
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
            "Here's your posture summary",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Main Score Display
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
    }
}


@Composable
fun CameraScreen(
    controller: LifecycleCameraController,
    pose: String,
    score: Int
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            controller = controller,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            PostureInfoCard(pose = pose, score = score)
        }
    }
}

@Composable
fun HistoryScreen(data: List<Triple<String, Int, Long>>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Posture History", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 24.dp))
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
fun PostureInfoCard(pose: String, score: Int) {
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
            }
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