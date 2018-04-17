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

    private val testMyAddress = "C4:F3:12:4A:D9:81"
    private val HEART_RATE_SERVICE = "00002a37-0000-1000-8000-00805f9b34fb"

    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothGatt: BluetoothGatt
    private lateinit var bluetoothDevicesAdapter: ArrayAdapter<BluetoothDevice>
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
        mBluetoothAdapter.cancelDiscovery()
        mBluetoothGatt = mDiscoveredDevices[discoveredDeviceIndex].connectGatt(context,true,mGattCallback)
        Log.d("LAURENT", "START SERVICE DISCOVERY")
        //discoverServices()

//        for( i in mDiscoveredDevices.indices){
//            if(mDiscoveredDevices[i].address == testMyAddress){
//                mBluetoothGatt = mDiscoveredDevices[i].connectGatt(context,true,mGattCallback)
//                connected = true
//            }
//        }
    }

    val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    private val mGattCallback = object: BluetoothGattCallback() {
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d("LAURENT", "Service was discovered")
            readData()
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluetoothData",characteristic?.value.toString())
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            currentHr = parseBluetoothByteArray(characteristic!!.value)
            calculateNewHrAverage(parseBluetoothByteArray(characteristic!!.value))

            Log.d("LAURENT","CHARACTERISTIC CHANGED")
            Log.d("BluetoothCharacteristic",parseBluetoothByteArray(characteristic?.value!!).toString())
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            Log.d("BluetoothData",descriptor?.value.toString())
            //super.onDescriptorRead(gatt, descriptor, status)
            //mBluetoothGattList.setCharacteristicNotification(descriptor?.characteristic, true)
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
        mBluetoothGatt.services.forEach {
            it.characteristics.forEach {
                if (it.uuid == UUID.fromString(HEART_RATE_SERVICE)) {
                    mBluetoothGatt.readCharacteristic(it)
                    var descriptorList = mutableListOf<UUID>()
                    it.descriptors.forEach {
                        descriptorList.add(it.uuid)
                    }

                    if(mBluetoothGatt.setCharacteristicNotification(it,true)){
                        Log.d("LAURENT", "Discovered good ccharacteristic to read, setting up")
                        var descriptor = it.getDescriptor(descriptorList[0])
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        mBluetoothGatt.writeDescriptor(descriptor)
                    }
                }
            }
        }
    }

    fun discoverServices(){
        mBluetoothGatt.discoverServices()
    }

    fun parseBluetoothByteArray(data: ByteArray): Int{
        return data[1].toInt()
    }


}