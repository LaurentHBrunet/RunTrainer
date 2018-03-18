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
    var averageBPM = 0
    var distance = 0.0f
    var pace = 0.0f
    var timerThreadInstance = timerThread()
    var currentlyRunning = true
    private var lastLocation: Location? = null

    init{
        timerThreadInstance.start()
    }

    fun isRunning() : Boolean {
        return currentlyRunning
    }

    fun pauseRun(){
        timerThreadInstance.pauseThread()
        currentlyRunning = false
    }

    fun resumeRun(){
        timerThreadInstance.resumeThread()
        currentlyRunning = true
    }

    fun stopRun(){
        timerThreadInstance.pauseThread()
    }

    fun addLocation(location: Location){
        if(lastLocation != null){
            distance += location.distanceTo(lastLocation)
            Log.d("distance", location.distanceTo(lastLocation).toString())
        }
        lastLocation = location
        listOfPositions.add(LatLng(location.latitude,location.longitude))
    }

    inner class timerThread :Thread{
        var pauseThread = false

        constructor():super(){

        }

        override fun run(){
            while(true){
                if(!pauseThread) {
                    elapsedTime++

                    Log.d("timer", elapsedTime.toString())
                }
                Thread.sleep(1000)
            }
        }

        fun pauseThread(){
            pauseThread = true
        }

        fun resumeThread(){
            pauseThread = false
        }
    }
}