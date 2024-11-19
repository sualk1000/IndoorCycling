
![banner](https://raw.github.com/sualk1000/IndoorCycling/master/images/Indoor_Cycling_Tracker_Icon.jpg)

# Indoor Cycling Tracker
Android app for Indoor Cylcling Traing with a Magene T100 Smart Trainer. The App can store the Data to Garmin Connect.


## Introduction

The App can be used to track activities with a
[Magene T100 Smart Trainer](https://blog.magene.com/review-t100-direct-drive-power-bike-trainer/)

<img  width="150" height="150" src="https://blog.magene.com/wp-content/uploads/2021/12/Magene-T100-Bike-Power-Trainer-Direct-Drive-Foldable-Indoor-Bicycle-Trainer-Platform-For-PowerFun-Zwift-PerfPro-Thru-axle-10.jpg"/> 
The App will create a BLE Connection to the T100 Device to read the Power during an activitie. The App can also create a BLE Connection to a Heartrate Belt like Garmin HRM Pro.
During the activity, the App will show the Time, Power, Speed, Distance and Heartrate. The Power and Heartrate will be shown in a Timeline Chart.

<img  width="120" height="200" src="https://raw.github.com/sualk1000/IndoorCycling/master/images/Indoor_Cycling_Tracker.jpg" />

After the Activity is stopped, the App will upload the Data into an Garmin Connect Accosunt. The Activity can be found in the Garmin Connect App.
<img  width="120" height="200" src="https://raw.github.com/sualk1000/IndoorCycling/master/images/Garmin3.jpg" />

## Quick Start

When the App is started the first time, thw will request permissions for Bluetooth and GPS. If the permissions are granted, the App will show a Settings Dialog.
In the Settings Dialog, the user must provide credentials for Garmin connect. 
The should select the BLE Devices for Power and Heartrate.

<img  width="120" height="200" src="https://raw.github.com/sualk1000/IndoorCycling/master/images/Indoor_Cycling_Tracker_Settings.jpg" />


If everything is ok, beside the Scan Button, the two round icons should turn into green. The the use can start the activity.

## Donations

If you would like to support this project's **feel free to donate**. Your donation is highly appreciated. Thank you!

[Donate via PayPal](https://www.paypal.com/donate/?hosted_button_id=9S7ECE73ZYXJ6) All donations are awesome!

<br/>
## License :page_facing_up:

Copyright 2020 Philipp Jahoda

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


<h2 id="creators">Special Thanks </h2>
MPAndroidChart (https://github.com/PhilJay/MPAndroidChart)