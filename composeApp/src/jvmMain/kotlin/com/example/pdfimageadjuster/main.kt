package com.example.pdfimageadjuster

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.res.painterResource
import ui.screens.MainScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PDF 颜色调整器",
        icon = painterResource("drawable/app_icon.png")
    ) {
        MainScreen()
    }
}