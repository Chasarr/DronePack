package com.example.charliemichaelringstrom.dronepack

import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException


/**
 * Handles server communication
 * @author Charlie Ringström
 * @version 1.0
 */
internal class HandleData(val ipAddress: String, val port: Int, private val callback: OnDownloadComplete?):
        AsyncTask<AsyncParams, Void, String>() {
    internal interface OnDownloadComplete {
        fun onDownloadComplete(output: String, longitude: String, latitude: String, client: Socket)
    }

    private lateinit var asyncClient: Socket
    private val TAG = "HandleData"
    private var messageCount = 0


    /**
     * Runs on the UI thread before [.doInBackground].
     *
     * @see .onPostExecute
     *
     * @see .doInBackground
     */
    override fun onPreExecute() {

    }

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
    override fun doInBackground(vararg params: AsyncParams?): String {
        return doInSameThread(params[0])
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
    override fun onPostExecute(result: String) {
        messageCount++
        //asyncClient.close()
        Log.i(TAG, result)

        val parsedString = result.split("\n")
        val gpsLine = parsedString.get(4).split(":", ",")

        if (!gpsLine.get(1).equals("None")) {
            val longitude = gpsLine.get(1)
            val latitude = gpsLine.get(2)
            Log.d(TAG, "Returned from server:\n--------------------\n" + result + "\n--------------------")
            callback?.onDownloadComplete(result, latitude, longitude, asyncClient)
        } else {
            Log.d(TAG, "Returned from server:\n--------------------\n" + result + "\n--------------------")
            Log.e(TAG, "Error parsing information from server:\n--------------------\n" + result + "\n--------------------")
        }

    }

    /**
     * "Downloads" a string from the server
     * @return String with text from the server
     */
    fun listen(): String {
        return buildString {
            while (true) {
                val ch = asyncClient.getInputStream()?.read()?.toChar()
                if (ch == '$') break
                append(ch)
            }
        }
    }

    /**
     * Runs on the same thread as the method is called from
     * @param asyncParams Information from the drone
     */
    fun doInSameThread(asyncParams: AsyncParams?): String {
        try {
            val command: String? = asyncParams?.command
            val userText = asyncParams?.userText
            val longitude = asyncParams?.longitude
            val latitude = asyncParams?.latitude
            var client: Socket? = asyncParams?.client
            if (client == null || client.isClosed) {
                Log.d(TAG, "Startar socket... ipAdress = " + ipAddress + ", port = " + port)
                client = Socket(ipAddress, port)
                Log.d(TAG, "Socket startad!")
            }
            asyncClient = client

            val printWriter = PrintWriter(client.getOutputStream())

            //Makes the drone go to a location (includes takeoff and such)
            if (command.equals("goToLocation")) {
                printWriter.write("UPDATE/USER/GOTO/DATA/0.1\n" +
                        "DEVICE: NAME" + "," + android.os.Build.VERSION.SDK_INT + "," + android.os.Build.MODEL + "\n" +
                        "TOKEN:USERTOKEN//BLANKFORNOW//\n" +
                        "GPS:" + latitude + "," + longitude + "\n" +
                        "ALT:15\n" +
                        "SPEED:5")
                Log.d(TAG, "Drone goes to location:\nLatitude: " + latitude + "\nLongitude: " + longitude)
            }
            //Requests update from drone
            if (command.equals("update")) {
                printWriter.write("GET/USER/TELDATA/0.0\n" +
                        "DEVICE: NAME" + "," + android.os.Build.VERSION.SDK_INT + "," + android.os.Build.MODEL + "\n" +
                        "TOKEN:USERTOKEN")
            }
            //Makes the drone take off to 15 meters
            if (command.equals("takeOff")) {
                printWriter.write("UPDATE/USER/TAKEOFF/DATA/0.0\n" +
                        "DEVICE: NAME" + "," + android.os.Build.VERSION.SDK_INT + "," + android.os.Build.MODEL + "\n" +
                        "TOKEN:USERTOKEN//BLANKFORNOW//\n" +
                        "ALT:15" + "\n" +
                        "HOVERTIME:60")
                Log.d(TAG, "Drone takes off to 15 metres")
            }
            //Drone returns to launch
            if (command.equals("returnToLaunch")) {
                printWriter.write("UPDATE/USER/RTL/DATA/0.0\n" +
                        "DEVICE: NAME" + "," + android.os.Build.VERSION.SDK_INT + "," + android.os.Build.MODEL + "\n" +
                        "TOKEN:USERTOKEN//BLANKFORNOW//")
                Log.d(TAG, "Drone returns to launch")
            }

            printWriter.flush()
            return listen()

        } catch (e: Throwable) {
            Log.e(TAG, "doInBackground: Invalid URL: " + e.printStackTrace())
            cancel(true)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Don't know about host: hostname" + e.printStackTrace());
            cancel(true)
        } catch (e: IOException) {
            Log.e(TAG, "Couldn't get I/O for the connection to: hostname" + e.printStackTrace());
            cancel(true)
        } catch (e: SecurityException) {
            Log.e(TAG, "doInBackground: Security Exception. Needs permission? " + e.printStackTrace())
            cancel(true)
        } catch (e: Exception) {
            Log.e(TAG, "Unknown exception: " + e.printStackTrace())
            cancel(true)
        }
        return "error"
    }
}