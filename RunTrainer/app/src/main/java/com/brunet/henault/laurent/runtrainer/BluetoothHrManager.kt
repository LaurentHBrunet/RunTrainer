package com.brunet.henault.laurent.runtrainer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.IntentFilter
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.util.Log


/**
 * Created by laurent on 2018-03-23.
 */
 class BluetoothHrManager(private val context: Context) {

    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var mDiscoveredDevices = mutableListOf<BluetoothDevice>()


    private val REQUEST_ENABLE_BT = 2
    fun  isBluetoothEnabled(): Boolean{
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            return false
        }
//
//        TODO("setup intent to enable bluetooth directly here")
//        if (!mBluetoothAdapter.isEnabled) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT,null)
//        }

        return true
    }


    fun scanBluetoothDevices(){
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(mReceiver, filter)

        mBluetoothAdapter.startDiscovery()
        //TODO("add cancel discovery somewhere before connecting")
    }


    // Create a BroadcastReceiver for ACTION_FOUND.
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                mDiscoveredDevices.add(device)
            }
        }
    }

    fun connectBluetoothDevice(){

    }

    fun getUpdates(){

    }

}