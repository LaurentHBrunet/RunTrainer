package com.brunet.henault.laurent.runtrainer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_info.*


//Very basic activity to display battery use and bandwidth use
class InfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        if(true){
            Log.d("test","test")
        }

        batteryUsage.text = "Battery usage: ${BatteryManager.instance?.getBatteryUsageSinceStart().toString()} %"

        dataTx.text = "Outgoing data (Tx) ${DataConsumptionManager.instance?.getTxBytesSinceStart()?.div(1000).toString()} kB"
        dataRx.text = "Received data (Rx) ${DataConsumptionManager.instance?.getRxBytesSinceStart()?.div(1000).toString()} kB"
    }
}
