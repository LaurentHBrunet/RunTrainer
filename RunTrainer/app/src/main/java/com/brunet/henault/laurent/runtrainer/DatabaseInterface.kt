package com.brunet.henault.laurent.runtrainer

import android.app.Activity
import android.content.Context
import android.content.pm.InstrumentationInfo
import android.icu.util.UniversalTimeScale.toLong
import android.location.Location
import android.provider.ContactsContract
import android.support.v4.app.NavUtils
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.ListView
import android.widget.TextView
import com.google.firebase.database.*
import java.util.*
import kotlin.collections.ArrayList

//Class that manages the connection to the firebase database

class DatabaseInterface(private var phoneId : String) { //unique phone ID to store runs in DB

    companion object {
        var instance: DatabaseInterface? = null //Singleton object
    }

    private var mDatabaseRef : DatabaseReference
    private var mDatabase : FirebaseDatabase
    private var currentRunId : Long = 0
    private var isRunIdReadSuccessful = false

    init{
        if(instance == null){
            instance = this
            this.mDatabase = FirebaseDatabase.getInstance()
            this.mDatabaseRef = mDatabase.reference
            this.readRunId(true,null,null,null)
        } else {
            throw InstantiationException()
        }
    }


    //Gets runs from the database
    fun fetchRuns(activity: MainActivity){
        val runsListener = object: ValueEventListener{
            override fun onCancelled(error: DatabaseError?) {
            }

            override fun onDataChange(snapshot: DataSnapshot?) {
                if (snapshot != null) {
                    activity.showPastRuns(parseRunsSnapshot(snapshot.child(phoneId)))
                }
            }
        }
        mDatabaseRef.addListenerForSingleValueEvent(runsListener)
    }

    //Puts the datasnapshot for runs into an ArrayList that can be used to shown in an ListView
    private fun parseRunsSnapshot(snapshot: DataSnapshot): ArrayList<Run> {
        val runList = ArrayList<Run>()

        snapshot.children.reversed().forEach{
            if(it.key != "runIdCounter"){
                val dist = it.child("Distance").value as Long
                val time = it.child("Time").value as Long
                val alt = it.child("AltitudeGain").value as Long

                if(it.child("AverageHr").exists()){
                    val averageHr = it.child("AverageHr").value as Long
                    runList.add(Run(dist, time, alt, averageHr))
                } else {
                    runList.add(Run(dist, time, alt, null))
                }
            }
        }

        return runList
    }

    //Reads the current runIdCounter value on database, Not used for now
    private fun readRunId(initialRead: Boolean, activity: Activity?, newRun: Run?, dialog: AlertDialog?) {
        val runIdListener = object: ValueEventListener{
            override fun onCancelled(error: DatabaseError?) {
            }

            override fun onDataChange(snapshot: DataSnapshot?) {
                Log.d("LAURENT", "read runIdCounter")
                DatabaseInterface.instance?.isRunIdReadSuccessful = true
                if(snapshot != null) {
                    if(snapshot.child(phoneId).child("runIdCounter").exists()){
                        currentRunId = snapshot.child(phoneId).child("runIdCounter").value.toString().toLong()

                        if(!initialRead) {
                            mDatabaseRef.child(phoneId).child("runIdCounter").setValue(currentRunId + 1)
                            setRunValues(activity!!,newRun!!,dialog!!)
                        }

                    } else {
                        mDatabaseRef.child(phoneId).child("runIdCounter").setValue(0)
                    }
                }
            }
        }
        mDatabaseRef.addListenerForSingleValueEvent(runIdListener)
    }

    //Saves a new run to the database
    fun saveNewRun(activity: Activity, newRun: Run, dialog: AlertDialog) {
        readRunId(false, activity, newRun, dialog)
    }


    //Takes the current run values and  writes them to the database
    private fun setRunValues(activity: Activity, newRun: Run, dialog: AlertDialog) {
        val updatedValues = HashMap<String, Any>()
        updatedValues.put("Distance", newRun.distance.toLong())
        updatedValues.put("Time", newRun.elapsedTime)
        updatedValues.put("AltitudeGain", newRun.altitudeGain.toLong())
        if(newRun.averageBPM != null){
            updatedValues.put("AverageHr", newRun.averageBPM!!.toLong())
        }

        mDatabaseRef.child(phoneId).child(currentRunId.toString())
                .updateChildren(updatedValues,DatabaseReference.CompletionListener({DatabaseError, DatabaseReference ->
                    if(DatabaseError == null){
                        completeAndExitActivity(activity, dialog)
                    } else {
                        Thread.sleep(500)
                        saveNewRun(activity, newRun, dialog)
                        Log.d("LAURENT", "Error when saving to firebase")
                    }
                }))
    }

    //When the write to the database process is finished, the saving dialog is closed and the view goes back to main activity
    private fun completeAndExitActivity(activity: Activity, dialog: AlertDialog){
        val recordActivity = activity as recordRunActivity
        recordActivity.unregisterSensors()
        NavUtils.navigateUpFromSameTask(activity)
        dialog.dismiss()
    }


}