package com.brunet.henault.laurent.runtrainer

import android.app.Activity
import android.content.Context
import android.hardware.SensorManager
import android.widget.Toast
import android.hardware.Sensor.TYPE_LIGHT
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import android.view.WindowManager
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC






class AmbiantLightManager(private val activity: Activity) {
    private var oldBrightness = 0.0f
    private var isDimmed = false
    private val sensorManager : SensorManager
    private val lightSensor : Sensor
    private val lightSensorEventListener: SensorEventListener

    init{
        sensorManager = activity.applicationContext.getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(TYPE_LIGHT)
        lightSensorEventListener = object : SensorEventListener {

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            }

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == TYPE_LIGHT) {
                    val currentReading = event.values[0]

                    if(currentReading <= 0.5 && !isDimmed){
                        isDimmed = true
                        setLightLevel(0.0f)
                    } else if(currentReading > 0.5 && isDimmed) {
                        isDimmed = false
                        setLightLevel(oldBrightness)
                    }
                }
            }
        }

        if (lightSensor == null) {
            Toast.makeText(activity.applicationContext,"Light sensor functionality not available", Toast.LENGTH_LONG).show()
        } else {
            sensorManager.registerListener(lightSensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregisterAmbiantLightManager() {
        sensorManager.unregisterListener(lightSensorEventListener)
    }

    //SET BRIGHTNESS LEVEL BETWEEN 0 and 1.0f
    private fun setLightLevel(lvl: Float){
        val lp = activity.window.attributes

        if(isDimmed){
            oldBrightness = lp.screenBrightness
        }

        lp.screenBrightness = lvl
        activity.window.attributes = lp
    }
}