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
 *
 * Class used to manage the communicatinoto the bluetooth device
 */
 class BluetoothHrManager(private val context: Context) {

    private val HEART_RATE_SERVICE = "00002a37-0000-1000-8000-00805f9b34fb" //Service number referencing heart rate monitors

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

        return true
    }

    //Array adapter used to update the dialog showing the blutooth options
    fun setArrayAdapter(adapter: ArrayAdapter<BluetoothDevice>){
        bluetoothDevicesAdapter = adapter
    }


    //Starts the scan of bluetooth devices
    fun scanBluetoothDevices(){
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(mReceiver, filter)

        mBluetoothAdapter.startDiscovery()
    }


    // Called when bluetooth device is found
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                addBluetoothDeviceIfUniqueAndNonHidden(device) //Adds the bluetooth device to the device list
                bluetoothDevicesAdapter.notifyDataSetChanged()
            }
        }
    }

    //Checks if the bluetooth device found is not in the list currently, and that it has a name, otherwise rejected
    private fun addBluetoothDeviceIfUniqueAndNonHidden(device: BluetoothDevice){
        if(device.name.isNullOrBlank() || device.name.isNullOrEmpty())
            return

        mDiscoveredDevices.forEach {
            if(it.address == device.address)
                return
        }

        mDiscoveredDevices.add(device)
    }

    //When a device is tapped by the user, connects the device to the bluetooth monitor
    fun connectBluetoothDevice(discoveredDeviceIndex: Int = 0){
        mBluetoothGatt = mDiscoveredDevices[discoveredDeviceIndex].connectGatt(context,true,mGattCallback)
    }


    //Callback function for bluetooth monitor
    val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    private val mGattCallback = object: BluetoothGattCallback() {
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            readData() //When services discovery returns something, setup the connection to get updates form the bluetooth device
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        }

        //Called whenever a characteristic the device is subscribed to changes
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            currentHr = parseBluetoothByteArray(characteristic!!.value)
            calculateNewHrAverage(parseBluetoothByteArray(characteristic!!.value))
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        }


    }

    //Calulates Hr average based on previous average and new value
    fun calculateNewHrAverage(newHr: Int){
        hrTicks++
        if(averageHr == null){
            averageHr = newHr
        } else {
            averageHr = (averageHr!! * (hrTicks - 1) + newHr) / hrTicks
        }
    }

    //When a service is discovered, itterates through the service to find a characteristic that matches
    // the Heart rate service, and subscribes to that characteristic to get notifications from the monitor when it changes
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


    //Starts the discovery of services
    fun discoverServices(){
        mBluetoothAdapter.cancelDiscovery()
        mBluetoothGatt?.discoverServices()
    }


    //Parses the byte array to return the actual heart rate reading
    fun parseBluetoothByteArray(data: ByteArray): Int{
        return data[1].toInt().and(0xFF)
    }


}