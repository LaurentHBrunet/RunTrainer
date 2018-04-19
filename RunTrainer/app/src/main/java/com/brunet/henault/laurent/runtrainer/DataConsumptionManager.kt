package com.brunet.henault.laurent.runtrainer

import android.net.TrafficStats

/**
 * Created by laurent on 2018-04-11.
 *
 * Singleton to check for the bandwidth used since starting the application
 */
class DataConsumptionManager{

    companion object {
        var instance: DataConsumptionManager? = null //Singleton instance
    }

    private val startingRxBytes : Long
    private val startingTxBytes : Long
    private val appUid : Int = android.os.Process.myUid()

    init {
        if(instance == null)
            instance = this
        else
            throw InstantiationException()

        startingRxBytes = TrafficStats.getUidRxBytes(appUid)
        startingTxBytes = TrafficStats.getUidTxBytes(appUid)
    }

    fun getRxBytesSinceStart(): Long {
        return TrafficStats.getUidRxBytes(appUid) - startingRxBytes
    }

    fun getTxBytesSinceStart(): Long {
        return TrafficStats.getUidTxBytes(appUid) - startingTxBytes
    }
}