package com.brunet.henault.laurent.runtrainer

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.opengl.Visibility
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.text.format.DateUtils.formatElapsedTime
import android.util.Log
import android.view.View
import com.brunet.henault.laurent.runtrainer.R.id.current_run_elapsed_time
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_record_run.*
import java.text.SimpleDateFormat

class recordRunActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var currentRun : Run
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager : LocationManager
    private lateinit var currentLocation: Location
    private lateinit var locationListener: MyLocationListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_run)

        requestHeartRateSensorUse()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationListener()

        start_run_button.setOnClickListener {
            startRunRecording()
        }

        pause_play_button.setOnClickListener{
            managePausePlay()
        }

        stop_run_button.setOnClickListener{
            finishRunRecording()
        }

    }

    private fun setupLocationListener(){
        locationListener = MyLocationListener()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0.0f, locationListener)
        }
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
        //TODO("Get HR monitors in range in a list")
        //TODO("Show dialog with options")
        //TODO("Connect on click in dialog")
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
    private fun startRunRecording() {
        start_run_button.visibility = View.GONE
        start_pause_button_layout.visibility = View.VISIBLE

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
        locationManager.removeUpdates(locationListener)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.0f, locationListener)
    }

    private fun setCameraPosition(location: Location){

        val montrealPos = LatLng(location.latitude, location.longitude) //Assign base position
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(montrealPos,15.0f)) //Zoom in to that base position
    }

    inner class updateDataThread :Thread{
        constructor():super(){

        }

        override fun run(){
            while(true){
                try{
                    runOnUiThread{
                        current_run_elapsed_time.text = formatElapsedTime()
                        current_distance.text = "${currentRun.distance} m"
                    }

                    Thread.sleep(500)
                }catch(ex: Exception){

                }
            }
        }

        private fun formatElapsedTime(): String{
            val seconds = currentRun.elapsedTime
            val hr = seconds / 3600
            val rem = seconds % 3600
            val min = rem / 60
            val sec = rem % 60

            return "${hr}:${min}:${sec}"
        }
    }

    //Location listener, that updates whenever the location of the user changes
    inner class MyLocationListener: android.location.LocationListener {
        constructor() {
            currentLocation = Location(LocationManager.GPS_PROVIDER)
        }

        override fun onLocationChanged(location: Location?) {
            location?.accuracy
            if(location != null && location.accuracy < 20){
                currentLocation = location //Assigns new user location
                setCameraPosition(location)
                //updateMap(location)
                currentRun.addLocation(location)
            }
            Log.d("location", "${currentLocation.latitude.toString()}/${currentLocation.longitude.toString()}")
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
        }

        override fun onProviderEnabled(p0: String?) {
        }

        override fun onProviderDisabled(p0: String?) {
        }
    }
}
