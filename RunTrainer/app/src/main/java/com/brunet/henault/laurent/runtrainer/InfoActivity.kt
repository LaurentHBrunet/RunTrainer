package com.brunet.henault.laurent.runtrainer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_info.*

class InfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        batteryUsage.text = "Battery usage: ${BatteryManager.instance?.getBatteryUsageSinceStart().toString()} %"

        dataTx.text = "Outgoing data (Tx) ${DataConsumptionManager.instance?.getTxBytesSinceStart()?.div(1000000).toString()} MB"
        dataRx.text = "Received data (Rx) ${DataConsumptionManager.instance?.getRxBytesSinceStart()?.div(1000000).toString()} MB"
    }
}
