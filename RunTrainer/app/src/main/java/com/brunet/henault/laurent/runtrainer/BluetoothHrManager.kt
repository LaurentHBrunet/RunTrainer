package com.brunet.henault.laurent.runtrainer

import android.bluetooth.*
import android.content.IntentFilter
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.os.SystemClock
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.util.Log
import android.widget.ArrayAdapter
import java.util.*
import java.nio.ByteBuffer
import kotlin.experimental.and


/**
 * Created by laurent on 2018-03-23.
 */
 class BluetoothHrManager(private val context: Context) {

    private val HEART_RATE_SERVICE = "00002a37-0000-1000-8000-00805f9b34fb"

    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothDevicesAdapter: ArrayAdapter<BluetoothDevice>
    private var mBluetoothGatt: BluetoothGatt? = null
    var mDiscoveredDevices = mutableListOf<BluetoothDevice>()

    var currentHr = 0
    var averageHr: Int? = null
    private var hrTicks = 0


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

    fun setArrayAdapter(adapter: ArrayAdapter<BluetoothDevice>){
        bluetoothDevicesAdapter = adapter
    }


    fun scanBluetoothDevices(){
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(mReceiver, filter)

        mBluetoothAdapter.startDiscovery()
    }


    // Create a BroadcastReceiver for ACTION_FOUND.
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                Log.d("bluetooth", device.address)

                addBluetoothDeviceIfUniqueAndNonHidden(device)
                bluetoothDevicesAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun addBluetoothDeviceIfUniqueAndNonHidden(device: BluetoothDevice){
        if(device.name.isNullOrBlank() || device.name.isNullOrEmpty())
            return

        mDiscoveredDevices.forEach {
            if(it.address == device.address)
                return
        }

        mDiscoveredDevices.add(device)
    }

    fun connectBluetoothDevice(discoveredDeviceIndex: Int = 0){
        mBluetoothGatt = mDiscoveredDevices[discoveredDeviceIndex].connectGatt(context,true,mGattCallback)
    }

    val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    private val mGattCallback = object: BluetoothGattCallback() {
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            readData()
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            currentHr = parseBluetoothByteArray(characteristic!!.value)
            calculateNewHrAverage(parseBluetoothByteArray(characteristic!!.value))
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        }


    }

    fun calculateNewHrAverage(newHr: Int){
        hrTicks++
        if(averageHr == null){
            averageHr = newHr
        } else {
            averageHr = (averageHr!! * (hrTicks - 1) + newHr) / hrTicks
        }
    }

    fun readData() {
        mBluetoothGatt!!.services.forEach {
            it.characteristics.forEach {
                if (it.uuid == UUID.fromString(HEART_RATE_SERVICE)) {
                    mBluetoothGatt!!.readCharacteristic(it)
                    var descriptorList = mutableListOf<UUID>()
                    it.descriptors.forEach {
                        descriptorList.add(it.uuid)
                    }

                    if(mBluetoothGatt!!.setCharacteristicNotification(it,true)){
                        var descriptor = it.getDescriptor(descriptorList[0])
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        mBluetoothGatt!!.writeDescriptor(descriptor)
                    }
                }
            }
        }
    }

    fun discoverServices(){
        mBluetoothAdapter.cancelDiscovery()
        mBluetoothGatt?.discoverServices()
    }

    fun parseBluetoothByteArray(data: ByteArray): Int{
        return data[1].toInt()
    }


}