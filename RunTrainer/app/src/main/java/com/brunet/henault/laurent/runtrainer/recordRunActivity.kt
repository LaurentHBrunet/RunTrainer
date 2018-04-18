package com.brunet.henault.laurent.runtrainer

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NavUtils
import android.support.v7.app.AlertDialog
import android.text.format.DateUtils.formatElapsedTime
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.brunet.henault.laurent.runtrainer.R.id.*
import com.google.android.gms.location.FusedLocationProviderClient

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_record_run.*

class recordRunActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var currentRun : Run
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager : LocationManager
    private lateinit var currentLocation: Location
    private lateinit var locationListener: MyLocationListener
    private var mBluetoothHrManager : BluetoothHrManager? = null
    var updatePeriodms: Long = 5000
    private val ADDMAPMARKERPERIOD = 15000
    private var markerUpdatesInPeriod = ADDMAPMARKERPERIOD / updatePeriodms
    private var markerUpdateCount = 0

    private var isPositionAccurate = false

    private var isMenuHidden = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_run)

        requestHeartRateSensorUse()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        start_run_button.setOnClickListener {
            startRunRecordingProcess()
        }

        pause_play_button.setOnClickListener{
            managePausePlay()
        }

        stop_run_button.setOnClickListener{
            finishRunRecording()
        }

        //TODO("CHECK FOR THE CURRENT UPDATE RATE SETTINGS")

    }

    private fun setupLocationListener(){
        locationListener = MyLocationListener()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, updatePeriodms*2, 0.0f, locationListener)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updatePeriodms*2, 0.0f, locationListener)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        }
        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

    }

    private fun requestHeartRateSensorUse(){
        val dialogBuilder = AlertDialog.Builder(this)

        dialogBuilder.setTitle(R.string.request_heartrate_title)
        dialogBuilder.setMessage(R.string.request_heartrate_message)

        dialogBuilder.setPositiveButton("Yes",DialogInterface.OnClickListener {dialog, button ->
            manageHeartRateConnection()
        })
        dialogBuilder.setNegativeButton("No", DialogInterface.OnClickListener { dialog, button ->
            showReadyToStartAlert()
        })

        dialogBuilder.show()
    }

    private fun manageHeartRateConnection(){
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Pick a device")

        mBluetoothHrManager = BluetoothHrManager(this)

        val arAdapt = BluetoothDevicesAdapter(this, R.layout.bluetooth_device_row, mBluetoothHrManager!!.mDiscoveredDevices)

        mBluetoothHrManager?.setArrayAdapter(arAdapt)

        dialogBuilder.setAdapter(arAdapt,DialogInterface.OnClickListener{ dialog, Int ->
            mBluetoothHrManager?.connectBluetoothDevice(Int)
        })

        dialogBuilder.show()

        if(mBluetoothHrManager?.isBluetoothEnabled() == true) {
            mBluetoothHrManager?.scanBluetoothDevices()
        }
    }

    private fun showReadyToStartAlert(){
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setTitle(R.string.ready_to_start_title)
        alertBuilder.setMessage(R.string.ready_to_start_message)

        alertBuilder.setPositiveButton("Ok",DialogInterface.OnClickListener {dialog, button ->
            //TODO("maybe do something eventually")
        })

        alertBuilder.show()
    }

    @SuppressLint("MissingPermission")
    private fun startRunRecordingProcess() {
        setupLocationListener()

        start_run_button.visibility = View.GONE

        mBluetoothHrManager?.discoverServices()

        waitForGPSAccuracy()
    }

    private fun waitForGPSAccuracy(){
        gps_accuracy_progress.visibility = View.VISIBLE
    }

    private fun startRunRecording(){
        gps_accuracy_progress.visibility = View.GONE
        start_pause_button_layout.visibility = View.VISIBLE

        this.supportActionBar!!.setDisplayShowHomeEnabled(false)
        this.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        this.supportActionBar!!.setHomeButtonEnabled(false) // disable the button

        setCameraPosition(currentLocation)

        currentRun = Run()

        val updateDataThread = updateDataThread()
        updateDataThread.start()
    }


    private fun managePausePlay() {
        if(currentRun.isRunning()){
            currentRun.pauseRun()
            pause_play_button.text = "Resume"
        } else {
            currentRun.resumeRun()
            pause_play_button.text = "Pause"
        }
    }

    @SuppressLint("MissingPermission")
    private fun finishRunRecording() {
        currentRun.averageBPM = mBluetoothHrManager?.averageHr //get final average BPM from bluetooth manager
        currentRun.timerThreadInstance.stopThread()

        locationManager.removeUpdates(locationListener) //Stop requesting position updates, to save battery


        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder
                .setTitle("Saving your run")
                .setView(R.layout.saving_run_dialog)
                .setNegativeButton("Cancel save (Your run will be lost)", DialogInterface.OnClickListener{ DialogInterface, Int ->
                    NavUtils.navigateUpFromSameTask(this)
                })

        val dialog = dialogBuilder.show()

        DatabaseInterface.instance?.saveNewRun(this, currentRun, dialog)



    }

    private fun setCameraPosition(location: Location){
        val currentPos = LatLng(location.latitude, location.longitude) //Assign base position
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPos,15.0f)) //Zoom in to that base position
    }

    private fun addDotMarker(location: Location){
        if(isAddMarkerPeriodFinished()) {
            Log.d("marker","Adding marker")
            mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.position_dot_blue))
                    .position(LatLng(location.latitude, location.longitude))
                    .anchor(0.5f, 0.5f))
        }
    }

    private fun isAddMarkerPeriodFinished(): Boolean{
        if(markerUpdateCount < markerUpdatesInPeriod){ //Check if we reached required number of position updates
            markerUpdateCount++
            return false
        } else {
            markerUpdateCount = 0
            Log.d("update", "updating marker")
            return true
        }
    }

    inner class updateDataThread :Thread{
        constructor():super()

        override fun run(){
            while(true){
                try{
                    runOnUiThread{
                        current_run_elapsed_time.text = "Elapsed time: ${formatElapsedTime(currentRun.elapsedTime)}"
                        current_distance.text = "Distance : ${(currentRun.distance / 1000)} km"
                        current_pace.text = "Current pace : ${formatPace()}"
                        current_hr.text = "Heart rate : ${mBluetoothHrManager?.currentHr} BPM"
                        current_altitude_gain.text = "Altitude gain : ${currentRun.altitudeGain} m"
                    }

                    Thread.sleep(500)
                }catch(ex: Exception){

                }
            }
        }

        private fun formatElapsedTime(elapsedTime: Long): String{
            val seconds = elapsedTime
            val hr = seconds / 3600
            val rem = seconds % 3600
            val min = rem / 60
            val sec = rem % 60

            return "${hr}:${min}:${sec}"
        }

        private fun formatPace(): String{
            if(currentRun.pace > 0.3) {
                val secondsPerKilometer = Math.round((16.66667 / currentRun.pace) * 60)
                val min = secondsPerKilometer / 60
                val sec = secondsPerKilometer % 60

                return "${min}:${sec} /km"
            } else {
                return "Not available"
            }
        }
    }

    //Location listener, that updates whenever the location of the user changes
    inner class MyLocationListener: android.location.LocationListener {
        constructor() {
            currentLocation = Location(LocationManager.GPS_PROVIDER)
        }

        override fun onLocationChanged(location: Location?) {

            if(location != null && location.accuracy <= 20){

                if(!isPositionAccurate){
                    isPositionAccurate = true
                    startRunRecording()
                }

                currentLocation = location
                setCameraPosition(location)
                addDotMarker(location)
                currentRun.addLocation(location)
                currentRun.setCurrentSpeed(location)
            }
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
        }

        override fun onProviderEnabled(p0: String?) {
        }

        override fun onProviderDisabled(p0: String?) {
        }
    }

    inner class BluetoothDevicesAdapter(context: Context,
                                       textViewResourceId: Int,
                                       private val items: List<BluetoothDevice>)
        : ArrayAdapter<BluetoothDevice>(context, textViewResourceId, items) {

        //For each item in items getView is called to fill a row of the List
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view: View? = convertView

            if (view == null) {
                val viewInflater = LayoutInflater.from(context)
                view = viewInflater.inflate(R.layout.bluetooth_device_row, null) //Sets row view to custom ap_list_row
            }
            val obj = items[position]
            if (obj != null) {
                val deviceName = view!!.findViewById<TextView>(R.id.device_name)
                deviceName.text = obj.name
            }
            return view!!
        }
    }

}
