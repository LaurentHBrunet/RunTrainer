package com.brunet.henault.laurent.runtrainer

import android.content.Context
import android.os.BatteryManager.EXTRA_SCALE
import android.os.BatteryManager.EXTRA_LEVEL
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager



/**
 * Created by laurent on 2018-04-11.
 */
class BatteryManager (private val context: Context){

    companion object {
        var instance: com.brunet.henault.laurent.runtrainer.BatteryManager? = null
    }

    private val startingBatteryLevel: Int

    init{
        if(instance == null)
            instance = this
        else
            throw InstantiationException()

        startingBatteryLevel = getCurrentBatteryLevel()
    }

    private fun getCurrentBatteryLevel(): Int {
        var ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        var batteryStatus = context.registerReceiver(null, ifilter)

        var level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        var scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return level * 100 / scale
    }

    fun getBatteryUsageSinceStart() : Int {
        return startingBatteryLevel - getCurrentBatteryLevel()
    }
}