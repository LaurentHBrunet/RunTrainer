package com.brunet.henault.laurent.runtrainer

import android.bluetooth.*
import android.content.IntentFilter
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.util.Log
import java.util.*
import java.nio.ByteBuffer
import kotlin.experimental.and


/**
 * Created by laurent on 2018-03-23.
 */
 class BluetoothHrManager(private val context: Context) {

    private val testMyAddress = "C4:F3:12:4A:D9:81"

    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothGatt: BluetoothGatt
    var mDiscoveredDevices = mutableListOf<BluetoothDevice>()
    private val HEART_RATE_SERVICE = "00002a37-0000-1000-8000-00805f9b34fb"
    private var connected = false


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

                Log.d("bluetooth", device.address)
                mDiscoveredDevices.add(device)
                if(!connected) {
                    connectBluetoothDevice()
                }
            }
        }
    }

    fun connectBluetoothDevice(discoveredDeviceIndex: Int = 0){

        for( i in mDiscoveredDevices.indices){
            if(mDiscoveredDevices[i].address == testMyAddress){
                mBluetoothGatt = mDiscoveredDevices[i].connectGatt(context,true,mGattCallback)
                connected = true
            }
        }
    }

    val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    private val mGattCallback = object: BluetoothGattCallback() {
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d("bluetoothService", status.toString())
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluetoothData",characteristic?.value.toString())
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            Log.d("BluetoothCharacteristic",parseBluetoothByteArray(characteristic?.value!!).toString())
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            Log.d("BluetoothData",descriptor?.value.toString())
            //super.onDescriptorRead(gatt, descriptor, status)
            //mBluetoothGattList.setCharacteristicNotification(descriptor?.characteristic, true)
        }


    }

    fun readData() {
        mBluetoothGatt.services.forEach {
            it.characteristics.forEach {
                if (it.uuid == UUID.fromString(HEART_RATE_SERVICE)) {
                    mBluetoothGatt.readCharacteristic(it)
                    var descriptorList = mutableListOf<UUID>()
                    it.descriptors.forEach {
                        descriptorList.add(it.uuid)
                    }

                    if(mBluetoothGatt.setCharacteristicNotification(it,true)){
                        var descriptor = it.getDescriptor(descriptorList[0])
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        mBluetoothGatt.writeDescriptor(descriptor)
                    }

                    //var descriptor = it.getDescriptor(UUID.fromString(HEART_RATE_SERVICE))
                    //mBluetoothGatt.readDescriptor(descriptor)
                }
            //Log.d("bluetoothcharacteristics",it.uuid.toString())
            }
        }
    }

    fun discoverServices(){
        mBluetoothGatt.discoverServices()
    }

    fun parseBluetoothByteArray(data: ByteArray): Int{

        var b1 = data[0].toInt()
        var dataByteArray = ByteArray(4)
        dataByteArray[0] = data[1]
        dataByteArray[1] = data[2]
        var valueBytes = ByteBuffer.wrap(dataByteArray)



//        if(b1 > 127){
            return data[1].toInt()
          //  Log.d("BluetoothHR","under127")
//        }
//        else{
//            return valueBytes.int
//        }

    }


}