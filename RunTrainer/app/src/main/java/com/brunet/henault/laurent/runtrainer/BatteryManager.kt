package com.brunet.henault.laurent.runtrainer

import android.content.Context
import android.os.BatteryManager.EXTRA_SCALE
import android.os.BatteryManager.EXTRA_LEVEL
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager



/**
 * Created by laurent on 2018-04-11.
 *
 * Singleton instantiated to check for battery level when instantiated and then whenever asked for it
 */
class BatteryManager (private val context: Context){

    companion object {
        var instance: com.brunet.henault.laurent.runtrainer.BatteryManager? = null //Singleton instance
    }

    private val startingBatteryLevel: Int

    init{
        if(instance == null) //Instantiate singleton
            instance = this
        else
            throw InstantiationException()

        startingBatteryLevel = getCurrentBatteryLevel()
    }

    //Gets the battery level in percentage of the device
    private fun getCurrentBatteryLevel(): Int {
        var ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        var batteryStatus = context.registerReceiver(null, ifilter)

        var level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        var scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return level * 100 / scale
    }

    //Returns difference between current battery level and beginning battery level
    // Percentage used since application started
    fun getBatteryUsageSinceStart() : Int {
        return startingBatteryLevel - getCurrentBatteryLevel()
    }
}