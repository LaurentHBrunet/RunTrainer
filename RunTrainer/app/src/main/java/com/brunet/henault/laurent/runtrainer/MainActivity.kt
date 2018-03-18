package com.brunet.henault.laurent.runtrainer

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            startRunRecording()
        }

        requestMapPermission()
        loadPastRuns()
    }

    val ACCESLOCATION=1
    private fun requestMapPermission(){
        //Checks if the user has the right permissions enabled to use GPS
        if(Build.VERSION.SDK_INT >= 23) {
            if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), ACCESLOCATION) //if permission isn't enabled, ask user for it
                return
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startRunRecording() {
        val recordRunIntent = Intent(this@MainActivity, recordRunActivity::class.java)
        startActivity(recordRunIntent)
    }

    private fun loadPastRuns() {

    }

}
