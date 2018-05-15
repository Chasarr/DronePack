package com.example.charliemichaelringstrom.dronepack

/**
 * Created by charlieringstrom on 2017-11-12.
 */
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View

/**
 * Created by timbuchalka on 10/08/2016.
 * To be used as a parent class. Its subclasses inherits from BaseActivity
 */

open class BaseActivity : AppCompatActivity() {
    private val TAG = "BaseActivity"
    internal fun activateToolbar(enableHome: Boolean) {
        Log.d(TAG, "activateToolbar: starts")
        var actionBar = supportActionBar
        if (actionBar == null) {
            val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
            setSupportActionBar(toolbar)
            actionBar = supportActionBar
        }

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(enableHome)
        }
    }


}
