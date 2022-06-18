# DrunkDetectionApp
An android application for detecting drunkness through gait analysis.

### Overview

The application is able to recognise when the user is drunk or not using a TensorFlow Lite model. The model was trained on over a months worth of data, in different cirumstances such as phone in pocket, phone in hand etc. The model is personalized, and has achieved 90% accuracy when testing in real life situations. The sensors used to achieve the predictions are the gyroscope and accelerometer. The app decides on whether the user is drunk or not by collecting 100 samples from both sensors, and then evaluating those readings using the Tflite model. If the majority of the readings are positive (drunk) the app will do a distress call.

### Customizability

Different distress messages can be set by the user, as well as a calling feature and a location system based on the gps of the phone.
![message](https://github.com/plotep/DrunkDetectionApp/blob/main/DrunkDetection/gitimages/settings_pg1.jpg)

### Demonstration
![showcasegif](https://github.com/plotep/DrunkDetectionApp/blob/main/DrunkDetection/gitimages/demonstration.gif)
### Authors
