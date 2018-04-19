package com.brunet.henault.laurent.runtrainer

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast


//Class that implements the step detector to calculate the cadence of a runner
class StepCounterManager(private val activity: Activity, private val currentRun: Run)  {
    private val sensorManager : SensorManager
    private val stepDetector : Sensor
    private val stepDetectorListener: SensorEventListener

    init{
        sensorManager = activity.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        stepDetectorListener = object : SensorEventListener {

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            }

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    currentRun.calculateCadence() //When a step is detected, calculate the new cadence
                }
            }
        }

        if (stepDetector == null) {
            Toast.makeText(activity.applicationContext,"Step sensor not available", Toast.LENGTH_LONG).show()
        } else {
            sensorManager.registerListener(stepDetectorListener, stepDetector, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    //Unregister the listener to stop getting events
    fun unregisterStepCountListener() {
        sensorManager.unregisterListener(stepDetectorListener)
    }


}