# Drone app
This repository contains the android app for the [App-controlled drone](https://github.com/KTheXIII/app-controlled-drone) repository.
To use the app, download the repository and import the project into the [Android Studio IDE](https://developer.android.com/studio/).

This app is for controlling a drone remotly with the software in the other repository. It utilizes Google Maps' API, and the user can graphically set a position for the drone to fly to. It displays the drone's position in real-time by communicating with a server which relays information from the drone via the internet.

To make the app run, the `API KEY` for the [Google Maps Platform](https://cloud.google.com/maps-platform/) in the `google_maps_api.xml` file needs to be changed. The ipAddress and port needs to be change in the `MapsActivity.kt` file.

For testing purposes only

## Authors
* [KTheXIII](https://github.com/KTheXIII)
* [Chasarr](https://github.com/Chasarr)

See also the list of [contributors](https://github.com/KTheXIII/app-controlled-drone/graphs/contributors) who participated in this project.
