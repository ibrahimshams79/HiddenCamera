package com.example.myapplication

import androidx.annotation.DrawableRes
import androidx.camera.core.Camera
import androidx.camera.core.CameraX
import androidx.compose.ui.graphics.Color

data class Feature(
    val title: String,
    @DrawableRes val iconId: Int,
    val lightColor: Color,
    val mediumColor: Color,
    val darkColor: Color,
    val whichCamera: Int,
    var camSelected: Boolean
    )
