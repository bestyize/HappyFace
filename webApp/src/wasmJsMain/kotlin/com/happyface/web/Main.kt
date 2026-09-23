package com.happyface.web

import com.happyface.ui.FaceApp
import kotlinx.browser.document
import org.jetbrains.compose.ui.window.ComposeViewport

fun main() {
    ComposeViewport(document.getElementById("root")!!) {
        FaceApp()
    }
}

