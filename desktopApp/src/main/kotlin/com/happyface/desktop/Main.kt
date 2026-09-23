package com.happyface.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import androidx.compose.ui.window.WindowState
import com.happyface.ui.FaceApp

fun main() = singleWindowApplication(
    title = "HappyFace · 表情预览",
    state = WindowState(size = DpSize(400.dp, 300.dp)),
) {
    FaceApp()
}
