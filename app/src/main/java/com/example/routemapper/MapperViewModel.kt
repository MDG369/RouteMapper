package com.example.routemapper

import android.graphics.Point
import android.graphics.PointF
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MapperViewModel : ViewModel() {
    var counterState = mutableStateOf(0)
    var rotState = mutableStateOf(0f)

    var msg = mutableStateOf("init")

    var points = mutableStateListOf<PointF>()


    // Function to increment the counter
    fun incrementCounter(steps: Int) {
        counterState.value += steps
        msg.value = "cntr: ${counterState.value}; rot: ${rotState.value}"

        if(points.isEmpty()) {
            points.add(PointF(0f, 0f))
        }

        val x = 0.025 * Math.cos(rotState.value.toDouble())
        val y = 0.025 * Math.sin(rotState.value.toDouble())

        points.add(PointF(x.toFloat(), y.toFloat()))
    }

    fun setMsg(newMsg: String) {
        msg.value = newMsg
    }

    fun setRotation(rot: Float) {
        rotState.value = rot
        msg.value = "cntr: ${counterState.value}; rot: ${rotState.value}"
    }
}