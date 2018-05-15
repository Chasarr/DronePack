package com.example.charliemichaelringstrom.dronepack

import java.net.Socket

/**
 * Stores information for asyncClass
 * @author Charlie Ringström
 * @version 1.0
 * @param command Command to be sent to the drone
 * @param userText Text to send to the drone. Optional, can be left as null
 * @param latitude The latitude coordinate of the destination
 * @param longitude The longitude coordinate of the destination
 * @param client Socket object to be reused. Can be left as null (not recommended long-term)
 */
class AsyncParams(var command: String, var userText: String?, var latitude: Double?, var longitude: Double?, var client: Socket?)