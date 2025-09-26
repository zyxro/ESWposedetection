package com.example.eswproject

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView.ScaleType

@Composable
fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = {
            PreviewView(it).apply {
                this.scaleType = ScaleType.FIT_CENTER
                this.controller = controller
            }
        },
        modifier = modifier
    )
}