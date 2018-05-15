package com.example.charliemichaelringstrom.dronepack

import android.Manifest
import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.lapism.searchview.SearchAdapter
import com.lapism.searchview.SearchHistoryTable
import com.lapism.searchview.SearchItem
import com.lapism.searchview.SearchView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

/**
 * Class that implements Google Maps API for Android.
 * It contains basic UI elements which enables the user to send a drone to a specific location
 * @author Charlie Ringström
 * @version 1.0
 */
class MapsActivity : BaseActivity(),    //Extends BaseActivity to inherit it's basic features
        OnMapReadyCallback,
        NavigationView.OnNavigationItemSelectedListener,
        GoogleMap.OnMarkerDragListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        HandleData.OnDownloadComplete,
        GoogleMapsSearch.OnMapsResultComplete {
    private val TAG: String = "MainActivity"
    private val GOOGLE_API_KEY = "AIzaSyClDqR98t_ytnz0hMTBPlK2ijF5vrKPrBs"      //API key. Not used in app
    private lateinit var mGoogleMap: GoogleMap      //GoogleMap object, the main Maps object
    private var mHistoryDatabase: SearchHistoryTable = SearchHistoryTable(this)     //Used with the SearchView API
    private var mUserMarker: Marker? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var mPermissionDenied = false
    private lateinit var handler: Handler           //Handles Android's task queue system
    private var drone: Marker? = null
    private var mCounter: Int = 0
    private lateinit var mNotificationManager: NotificationManager
    private var arrived = false

    var outputStream: DataOutputStream? = null
    var inputStream: DataInputStream? = null

    private val ipAddress = "31.211.232.4"
    private val port = 7000
    private var client: Socket? = null

    //lazy initializes the mSearchview variable when first used in the code
    private val mSearchView: SearchView by lazy {
        findViewById<SearchView>(R.id.searchView)
    }

    private val mFab: FloatingActionButton by lazy {
        findViewById<FloatingActionButton>(R.id.fab)
    }
    private val mDrawerLayout: DrawerLayout by lazy {
        findViewById<DrawerLayout>(R.id.drawer_layout)
    }

    /**
     * Starts when the app initializes
     * @param savedInstanceState Bundled information to resume from a previous session
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate mapsActivity: starts")
        //Checks if Google Maps is installed on the device. Otherwise it links to Play Store for you to download it
        if (googleServicesAvailability()) {
            setContentView(R.layout.activity_main)
            //Initializes search, map and disable the ability to fly to a location until a destination is set
            search()
            initMap()
            mFab.hide()

        } else {
            Toast.makeText(this, "Google Maps needs to be installed to run DronePack", Toast.LENGTH_LONG).show()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps"))
            startActivity(intent)
        }
        handler = Handler()
        handler.post(sendData)

    }

    /**
     * Executes when the Floating Action Button is clicked
     * @param view FAB object
     */
    fun fabClicked(view: View) {
        // When the FAB button is pressed, information is sent to the drone
        // If mUserMarker is null for some reason it won't execute. Otherwise it sends a message to the server
        if (mUserMarker == null) {
            Toast.makeText(this, "No destination set", Toast.LENGTH_SHORT).show()
        } else {
            val asyncParams = AsyncParams("goToLocation", null, mUserMarker?.position?.latitude, mUserMarker?.position?.longitude, null)
            val dataHandler = HandleData(ipAddress, port, this)
            dataHandler.execute(asyncParams)
        }

    }

    /**
     * Callback method which executes when the app has finished downloading location information from Google's servers
     * @param address Address object provided with Google Maps
     */
    override fun onMapsResultComplete(address: Address?) {
        if(address != null) {
            if (address.locality != null && !address.locality!!.isEmpty()) {
                Toast.makeText(this, address.locality, Toast.LENGTH_LONG).show()
            }
            val lat = address.latitude
            val lng = address.longitude
            //Zooms in on the location
            goToLocation(lat, lng, 17f)
            val userSearchCoordinates = LatLng(lat, lng)

            if (mUserMarker != null) {
                mUserMarker?.remove()
            }
            mUserMarker = mGoogleMap.addMarker(MarkerOptions().position(userSearchCoordinates)
                    .title("Destination"))
            mUserMarker?.isDraggable = true
            mUserMarker?.tag = 0
            arrived = false
        }else{
            //Displays Error message if no address is returned
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Called when an item in the navigation menu is selected. Not currently implemented
     * @param item The selected item
     * @return true to display the item as the selected item
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_camera -> {
                //TODO Add feature
            }
            R.id.nav_gallery -> {
                //TODO Add feature
            }
            R.id.nav_slideshow -> {
                //TODO Add feature
            }
            R.id.nav_manage -> {
                //TODO Add feature
            }
            R.id.nav_share -> {
                //TODO Add feature
            }
            R.id.nav_send -> {
                //TODO Add feature
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Makes the user's location appear on the map. If the user hasn't given it permission to access location it asks for it
     */
    fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);

        } else {
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    /**
     * Initializes the map view
     */
    private fun initMap() {
        val mapFragment = fragmentManager.findFragmentById(R.id.mapFragment) as MapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Method which gets called when the map has initialized. Sets a couple of preset options such as starting position
     * @param googleMap Map object
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        enableMyLocation()
        mGoogleMap.setOnMarkerDragListener(this)
    }

    /**
     * Makes the camera go to a location
     * @param lat Latitude coordinate
     * @param lng Longitude coordinate
     */
    private fun goToLocation(lat: Double, lng: Double) {
        goToLocation(lat, lng, 17f)
    }

    /**
     * Overloading method, makes the camera go to a location
     * @param lat Latitude coordinate
     * @param lng Longitude coordinate
     * @param zoom How far the app should zoom in on the coordinate
     */
    private fun goToLocation(lat: Double, lng: Double, zoom: Float) {
        Log.d(TAG, "Went to latitude: " + lat + ", longitude: " + lng)
        val ll = LatLng(lat, lng)
        val update = CameraUpdateFactory.newLatLngZoom(ll, zoom)
        mGoogleMap.animateCamera(update)

    }

    /**
     * Checks if Google Maps is installed on the device
     * If not, it
     * @return Returns true if Maps is available on the device
     */
    private fun googleServicesAvailability(): Boolean {
        val api = GoogleApiAvailability.getInstance()
        val isAvailable = api.isGooglePlayServicesAvailable(this)
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true
        } else if (api.isUserResolvableError(isAvailable)) {
            val dialog = api.getErrorDialog(this, isAvailable, 0)
            dialog.show()
        } else {
            Toast.makeText(this, "Cannot connect to play services", Toast.LENGTH_LONG).show()
        }
        return false
    }

    /**
     * Searches for a posision on Google's database
     * @param location of the location
     */
    private fun searchPosition(location: String) {
        val mapSearch = GoogleMapsSearch(this)
        val mapsSearchParams = MapsSearchParams(this, location)
        mapSearch.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mapsSearchParams)
    }

    /**
     * Executes when the user hits the "Take off" button.
     * Makes the drone take off the ground to 15 meters above ground
     * @param view View object
     */
    fun takeOff(view: View) {
        Log.d(TAG, "Taking off...")
        val asyncParams = AsyncParams("takeOff", null, mUserMarker?.position?.latitude, mUserMarker?.position?.longitude, client)
        val dataHandler = HandleData(ipAddress, port, this)
        dataHandler.execute(asyncParams)
    }

    /**
     * Executes when the user hits the FAB.
     * Makes the drone take off the ground to 15 meters above ground and then goes the desired destination
     */
    fun returnToLaunch(view: View) {
        Log.d(TAG, "Returning to launch...")
        val asyncParams = AsyncParams("returnToLaunch", null, mUserMarker?.position?.latitude, mUserMarker?.position?.longitude, client)
        val dataHandler = HandleData(ipAddress, port, this)
        dataHandler.execute(asyncParams)
    }

    /**
     * Initializes the search function
     */
    private fun search() {
            mSearchView.setVersionMargins(SearchView.VersionMargins.TOOLBAR_SMALL)
            mSearchView.setHint(R.string.search)

            mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    searchPosition(query)
                    try{
                        mHistoryDatabase.addItem(SearchItem(query))
                    } catch (SQLException: Exception) {
                        Log.e(TAG, "search: Tried to add the same object to the history database twice: " + SQLException.message)
                    }
                    mSearchView.close(false)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    return false
                }
            })
            mSearchView.setOnOpenCloseListener(object : SearchView.OnOpenCloseListener {
                override fun onOpen(): Boolean {
                        mFab.hide()
                    return true
                }

                override fun onClose(): Boolean {
                    if (!mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                        mFab.show()
                    }
                    return true
                }
            })
            mSearchView.setVoiceText("Set permission on Android 6.0+ !")
            mSearchView.setOnVoiceIconClickListener(SearchView.OnVoiceIconClickListener() {
                fun onVoiceIconClick() {
                }
            })

            val suggestionsList: MutableList<SearchItem> = ArrayList()
            suggestionsList.add(SearchItem("Södra Sandby Skallavångsvägen 18"))


            val searchAdapter = SearchAdapter(this@MapsActivity, suggestionsList)
            searchAdapter.setOnSearchItemClickListener { view, position, text ->
                searchPosition(text)
                mSearchView.setTextOnly(text)
                Log.d(TAG, "Trying to close...")
                mSearchView.close(false)
                Log.d(TAG, "CLOSED! I think...")
            }
            mSearchView.adapter = searchAdapter
            searchAdapter.notifyDataSetChanged()
    }

    /**
     * Handles permission results
     * @param requestCode Integer code
     * @param permission Array with permissions
     * @param grantResults Array with Integers
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    /**
     * Method which is executed when it returns information from the server
     * @param output string
     * @param longitude value
     * @param latitude value
     * @param socket object which is used for maintaining connection to the server
     */
    override fun onDownloadComplete(output: String, longitude: String, latitude: String, client: Socket) {
        //Reuses the client object. This means the server doesn't have to constantly handle new socket requests
        this.client = client
        //If there is no drone icon present on the map, it creates one
        if (drone == null) {
            drone = mGoogleMap.addMarker(MarkerOptions()
                    .position(LatLng(latitude.toDouble(), longitude.toDouble()))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.drone)))
        }
        //Sets the updated drone position on the map
        drone?.position = LatLng(latitude.toDouble(), longitude.toDouble())
        if (mUserMarker != null) {
            Log.d(TAG, "Latitude = " + latitude +
                    "\nLongitude = " + longitude +
                    "\nDelta latitude = " + (latitude.toDouble() - mUserMarker!!.position.latitude).toString() +
                    "\nDelta longitude = " + (longitude.toDouble() - mUserMarker!!.position.longitude).toString() +
                    "\nMarker latitude: " + mUserMarker!!.position.latitude.toString() +
                    "\nMarker longitude = " + mUserMarker!!.position.latitude.toString())

            //Calculates the distance between the drone and its destination.
            // If it comes in a certain radius from the destination, a notification is sent
            if (Math.pow(mUserMarker!!.position!!.latitude - latitude.toDouble(), 2.0) + Math.pow(mUserMarker!!.position!!.longitude - longitude.toDouble(), 2.0) <= Math.pow(30 / 111111.0, 2.0) && !arrived) {
                Log.d(TAG, "It has been clarified that this application indeed is working nominally, according to our defenition of nominal")
                arrived = true
                buildNotification()
            }
            Log.d(TAG, (Math.pow(longitude.toDouble() - mUserMarker!!.position.longitude, 2.0) + Math.pow(latitude.toDouble() - mUserMarker!!.position.latitude, 2.0)).toString() + "\t" + Math.pow(350 / 111111.0, 2.0))
        }
    }

    /**
     * Notifications to be used if the device is on Android 8.0 or higher
     */
    @TargetApi(Build.VERSION_CODES.O)
    private fun buildNotification() {
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // The id of the channel
            val id = getString(R.string.drone_finished)
            // The user-visible name of the channel
            val name = getString(R.string.channel_name)
            // The user-visible description of the channel
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(id, name, importance)
            // Configure the notification channel
            mChannel.description = description
            mChannel.enableLights(true)
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            mChannel.lightColor = Color.RED
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            mNotificationManager.createNotificationChannel(mChannel)
            pushNotification(id)
        } else {
            pushNotification(getString(R.string.drone_finished))
        }
    }

    /**
     * Enables the marker to be dragged
     */
    private fun setMarkerListener() {
        mGoogleMap.setOnMarkerDragListener(object : OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {
                Log.d(TAG, "onMarkerDragStart..." + marker.getPosition().latitude + "..." + marker.getPosition().longitude)
            }

            override fun onMarkerDragEnd(marker: Marker) {
                Log.d(TAG, "onMarkerDragEnd..." + marker.getPosition().latitude + "..." + marker.getPosition().longitude)
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()))
            }

            override fun onMarkerDrag(marker: Marker) {
                Log.i(TAG, "onMarkerDrag...")
            }
        })
    }

    /**
     * Sets the markers coordinates the dragged position
     * @param marker Marker object
     */
    override fun onMarkerDragEnd(marker: Marker?) {
        Log.d(TAG, "onMarkerDragEnd..." + marker?.getPosition()?.latitude + "..." + marker?.getPosition()?.longitude)
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(marker?.getPosition()))
    }

    /**
     * Gets called when marker gets dragged
     * @param marker Marker object
     */
    override fun onMarkerDragStart(marker: Marker?) {
        Log.d(TAG, "onMarkerDragStart..." + marker?.getPosition()?.latitude + "..." + marker?.getPosition()?.longitude)
    }

    /**
     * Gets called when the marker is dragged
     * @param marker Marker object
     */
    override fun onMarkerDrag(marker: Marker?) {
        Log.i(TAG, "onMarkerDrag...")
    }

    /**
     * Creates a notification with SDK levels below 26
     */
    private fun pushNotification(notificationID: String) {
        val notification: Notification = NotificationCompat.Builder(this, notificationID)
                .setCategory("DronePack")
                .setContentTitle("Drone has arrived")
                .setContentText("Open app for more actions")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        mNotificationManager.notify(mCounter++, notification)
    }

    /**
     * Requests updates from server ten times per second
     */
    private val sendData = object : Runnable {
        override fun run() {
            try {
                val dataHandler = HandleData(ipAddress, port, this@MapsActivity)
                val asyncParams = AsyncParams("update", null, mUserMarker?.position?.latitude, mUserMarker?.position?.longitude, client)
                dataHandler.execute(asyncParams)

                handler.postDelayed(this, 100)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Gets called when the app quits
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Reachable: true")
        handler.removeCallbacks(sendData)
    }
}
