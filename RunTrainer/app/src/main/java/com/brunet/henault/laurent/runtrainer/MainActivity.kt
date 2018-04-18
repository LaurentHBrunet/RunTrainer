package com.brunet.henault.laurent.runtrainer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AppCompatActivity
import android.text.format.DateUtils.formatElapsedTime
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.database.DataSnapshot

import kotlinx.android.synthetic.main.activity_main.*
import org.w3c.dom.Text

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            startRunRecording()
        }

        initializeSingletons()
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

    private fun initializeSingletons(){
        try {
            BatteryManager(this)
            DataConsumptionManager()
            DatabaseInterface(Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID))
        } catch (e : Exception) {
            Log.i("LAURENT", e.toString()) //Just makes sure the singletons are initiated once and leaves a trace
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
            R.id.info_action -> openInfos()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openInfos() : Boolean {
        val infoIntent = Intent(this@MainActivity, InfoActivity::class.java)
        startActivity(infoIntent)

        return true
    }

    private fun startRunRecording() {
        val recordRunIntent = Intent(this@MainActivity, recordRunActivity::class.java)
        startActivity(recordRunIntent)
    }

    private fun loadPastRuns() {
        DatabaseInterface.instance?.fetchRuns(this)
    }

    fun showPastRuns(runList: ArrayList<Run>) {
        fetching_runs_view.visibility = View.GONE
        run_list_view.visibility = View.VISIBLE

        val runListView = run_list_view
        runListView.adapter = RunListAdapter(this, runListView.id, runList)
    }

    inner class RunListAdapter(context: Context,
                                       textViewResourceId: Int,
                                       private val items: ArrayList<Run>) //If this is the current APs view, pass favoritesList adapter so it can be accessed from this
        : ArrayAdapter<Run>(context, textViewResourceId, items) {

        //For each item in items getView is called to fill a row of the List
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view: View? = convertView
            if (view == null) {
                val viewInflater = LayoutInflater.from(context)
                view = viewInflater.inflate(R.layout.run_list_item, null) //Sets row view to custom ap_list_row
            }
            val obj = items[position]
            if (obj != null) {
                val distTV = view!!.findViewById<TextView>(R.id.distance_textview)
                val timeTV = view!!.findViewById<TextView>(R.id.time_textview)
                val altitudeTV = view!!.findViewById<TextView>(R.id.altitude_textview)
                val bpmTV = view!!.findViewById<TextView>(R.id.hr_textview)
                val dateTV = view!!.findViewById<TextView>(R.id.date_textview)

                dateTV.text = "TODO"
                altitudeTV.text = "Altitude gain : ${obj.altitudeGain} m"
                distTV.text = "Distance : ${obj.distance / 1000} km"
                timeTV.text = "Elapsed time: ${formatElapsedTime(obj.elapsedTime)}"

                if(obj.averageBPM != null){
                    bpmTV.text = "Heart rate : ${obj.averageBPM} BPM"
                } else {
                    bpmTV.text = "No recorded HR"
                }
            }
            return view!!
        }
    }

    //MAKE THIS AND RECORDRUNACTIVITY INTO A BASE SHARED UTILS FOLDER
    private fun formatElapsedTime(elapsedTime: Long): String{
        val seconds = elapsedTime
        val hr = seconds / 3600
        val rem = seconds % 3600
        val min = rem / 60
        val sec = rem % 60

        return "${hr}:${min}:${sec}"
    }

}
