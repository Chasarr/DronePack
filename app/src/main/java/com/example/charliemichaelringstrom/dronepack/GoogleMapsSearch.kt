package com.example.charliemichaelringstrom.dronepack

import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import java.io.IOException


/**
 * @author Charlie Ringström
 * @version 1.0 Handles downloads from Google's database
 */
internal class GoogleMapsSearch(private val callback: GoogleMapsSearch.OnMapsResultComplete?) : AsyncTask<MapsSearchParams, Void, Address?>() {
    internal interface OnMapsResultComplete {
        fun onMapsResultComplete(address: Address?)
    }
    private val TAG = "GoogleMapsSearch"


    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to [.execute]
     * by the caller of this task.
     *
     * This method can call [.publishProgress] to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     *
     * @return A result, defined by the subclass of this task.
     *
     * @see .onPreExecute
     * @see .onPostExecute
     *
     * @see .publishProgress
     */
    override fun doInBackground(vararg params: MapsSearchParams?): Address? {
        val context = params[0]?.context
        val query = params[0]?.query
        try {

            val gc = Geocoder(context)
            val list = gc.getFromLocationName(query, 1)
            if(list.size > 0) {
                val address: Address = list[0]
                return address
            }else{
                return null
            }
            /*
            if (address.locality != null && !address.locality.isEmpty()) {
                Toast.makeText(context, address.locality, Toast.LENGTH_LONG).show()
            }
            val lat = address.latitude
            val lng = address.longitude
            goToLocation(lat, lng, 17f)
            val userSearchCoordinates = LatLng(lat, lng)

            if (mUserMarker != null) {
                mUserMarker?.remove()
            }
            mUserMarker = mGoogleMap.addMarker(MarkerOptions().position(userSearchCoordinates)
                    .title("Marker in " + location))
            mUserMarker?.isDraggable = true
            mUserMarker?.tag = 0
            */

        } catch (e: IOException) {
            Log.e(TAG, "searchPosition: IOException CRASH: " + e.message)
            Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
        } catch (e: IndexOutOfBoundsException) {
            Log.e(TAG, "searchPosition: Exception CRASH: " + e.message)
            Toast.makeText(context, "No result", Toast.LENGTH_SHORT).show()
        } catch(e: Exception){
            Log.e(TAG, "No results")
            Toast.makeText(context, "No results", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    /**
     *
     * Runs on the UI thread after [.doInBackground]. The
     * specified result is the value returned by [.doInBackground].
     *
     *
     * This method won't be invoked if the task was cancelled.
     *
     * @param result The result of the operation computed by [.doInBackground].
     *
     * @see .onPreExecute
     *
     * @see .doInBackground
     *
     * @see .onCancelled
     */
    override fun onPostExecute(address: Address?) {
        callback?.onMapsResultComplete(address)
    }
}