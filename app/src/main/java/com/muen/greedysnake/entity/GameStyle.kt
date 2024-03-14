package com.muen.greedysnake.entity

import android.graphics.Color

class GameStyle(var type: Int) {
    fun getColor() = when (type) {
        Type.BODY -> Color.BLUE
        Type.HEAD -> Color.RED
        Type.GRID -> Color.GRAY
        Type.FOOD -> Color.YELLOW
        else -> Color.GRAY
    }
}