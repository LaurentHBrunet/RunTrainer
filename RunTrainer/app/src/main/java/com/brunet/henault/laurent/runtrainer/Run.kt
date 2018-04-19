package com.brunet.henault.laurent.runtrainer

import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.sql.Time
import java.util.*
import kotlin.concurrent.timer

/**
 * Created by laurent on 2018-03-17.
 */

class Run {
    var listOfPositions = mutableListOf<LatLng>()
    var elapsedTime = 0L //elapsed Time in seconds
    var averageBPM : Int? = null
    var distance = 0.0f
    var pace = 0.0f
    var timerThreadInstance = timerThread()
    var currentlyRunning = true
    var altitudeGain = 0.0f
    var cadence = 0

    private var lastStep = 0L

    private var lastLocation: Location? = null

    constructor() {
        timerThreadInstance.start()
    }

    constructor(newDistance: Long, newTime: Long, newAlt: Long, newAverageHr: Long?){
        distance = newDistance.toFloat()
        elapsedTime = newTime
        altitudeGain = newAlt.toFloat()
        averageBPM = newAverageHr?.toInt()
    }

    //Calculates cadence based on time difference between steps
    fun calculateCadence() {
        if(lastStep != 0L && System.currentTimeMillis() - lastStep > 150) {
            cadence = 60000 / (System.currentTimeMillis() - lastStep).toInt()
        }

        lastStep = System.currentTimeMillis()
    }

    fun isRunning() : Boolean {
        return currentlyRunning
    }

    fun setCurrentSpeed(location: Location){
        pace = location.speed
    }

    fun pauseRun(){
        timerThreadInstance.pauseThread()
        currentlyRunning = false
    }

    fun resumeRun(){
        timerThreadInstance.resumeThread()
        currentlyRunning = true
    }

    fun addLocation(location: Location){
        if(currentlyRunning) {
            if (lastLocation != null) {
                distance += location.distanceTo(lastLocation)
                altitudeGain += Math.abs(lastLocation!!.altitude - location.altitude).toFloat()
                Log.d("distance", location.distanceTo(lastLocation).toString())
            }
            lastLocation = location
            listOfPositions.add(LatLng(location.latitude, location.longitude))
        }
    }

    //1 second timer counter, used this instead of System.Time so it is easier to keep track of pauses
    // Might want to switch back to System.Time just to save a thread.
    inner class timerThread :Thread{
        var pauseThread = false
        var stopThread = false

        constructor():super()

        override fun run(){
            while(!stopThread){
                if(!pauseThread) {
                    elapsedTime++

                    Log.d("timer", elapsedTime.toString())
                }
                Thread.sleep(1000)
            }
        }

        fun stopThread(){
            stopThread = true
        }

        fun pauseThread(){
            pauseThread = true
        }

        fun resumeThread(){
            pauseThread = false
        }
    }
}