/*
 * Authors: Pawel Bielinski, Jamie Grant
 * App that detects sobriety through gait detection using a TensorFlow model that has been converted
 * to TensorFlow Lite. Once the model detects drunkness, the app allows for a customizable response
 * such as calling or texting a set number with the GPS location of the user. The status is constantly
 * shown to show the predicted state of the model. The model has been trained on a weeks worth of
 * sober and "drunk" walking data in order to classify both categories, and being able to do so
 * with over 95% accuracy in training, and 90% when tested on a Samsung Galaxy S8 phone.
 * The app takes readings from the gyroscope and accelerometer sensors as input for the
 * Tflite model in one array. To make the process more accurate, the app collects 100 samples from
 * the phone, and when the classification function is called, if the model has classified the
 * majority of the samples as drunk or sober, the state changes.
 */

package com.example.diceroller

import android.animation.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.hardware.Sensor
import android.hardware.Sensor.*
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import com.example.diceroller.ml.Model
import com.google.android.gms.location.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), SensorEventListener {
    // sensor globals
    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccelerometer: Sensor
    private lateinit var mGyroscope: Sensor
    private lateinit var mPressure: Sensor
    var accelerometerData = ArrayList<Float>()
    var accelerometerDatax = ArrayList<Float>()
    var accelerometerDatay = ArrayList<Float>()
    var accelerometerDataz = ArrayList<Float>()
    var gyroscopeData = ArrayList<Float>()
    var gyroscopeDatax = ArrayList<Float>()
    var gyroscopeDatay = ArrayList<Float>()
    var gyroscopeDataz = ArrayList<Float>()
    // switch on/off
    var modelOnOffFlag = 0
    // fusedlocation global
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // locationrequest global
    private lateinit var locationRequest: LocationRequest

    // latitude and longitude global
    var latitude = 0.0
    var longitude = 0.0

    // locationcallback global
    private lateinit var locationCallback: LocationCallback

    @ExperimentalStdlibApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // setting the fading animation
        val mainview: ConstraintLayout = findViewById(R.id.constlayout)
        var animationDrawable: AnimationDrawable = mainview.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(3000)
        animationDrawable.setExitFadeDuration(3000)
        animationDrawable.start()
        // getting location updates to be able to send messges
        getLocationUpdates()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // setting an onclick listener for settings
        var settingsBtn: ImageButton = findViewById(R.id.settingsbutton)
        settingsBtn.setOnClickListener {
            val intent = Intent(this@MainActivity,
                    SettingsActivity::class.java)
            startActivity(intent)
        }
        // getting sensormanager and the relevant sensors
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE)
        mPressure = mSensorManager.getDefaultSensor(TYPE_PRESSURE)
        val switch : Switch = findViewById(R.id.modelOnOff)
        switch.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            if (b){
                modelOnOffFlag = 1
            }
            else{
                modelOnOffFlag = 0
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    @ExperimentalStdlibApi
    override fun onSensorChanged(event: SensorEvent?) {

        val sensorName: String = event?.sensor!!.getName();
        // run until x samples of gyro and accel data are taken
        if (gyroscopeData.count() < 100) {
            if (event.sensor.type == TYPE_GYROSCOPE) {
                // magnitude of gyroscope data
                gyroscopeData.add(sqrt((event.values[0] * event.values[0]) + (event.values[1] * event.values[1]) + (event.values[2] * event.values[2])))
                // x value
                gyroscopeDatax.add(event.values[0])
                // y value
                gyroscopeDatay.add(event.values[1])
                // z value
                gyroscopeDataz.add(event.values[2])
            }
            else if (event.sensor.type == TYPE_ACCELEROMETER) {
                // magnitude of accelerometer data
                accelerometerData.add(sqrt((event.values[0] * event.values[0]) + (event.values[1] * event.values[1]) + (event.values[2] * event.values[2])))
                // x value
                accelerometerDatax.add(event.values[0])
                // y value
                accelerometerDatay.add(event.values[1])
                // z value
                accelerometerDataz.add(event.values[2])
            }
        } else if(modelOnOffFlag == 1){
            // run prediction if enough data is collected and drunkness detection switch is on
            runPrediction()
        }
    }

    // animation used for change of states
    private fun fadeInOut() {
        val mainView: ConstraintLayout = findViewById(R.id.constlayout)
        val fadeOut: Animation = AnimationUtils.loadAnimation(mainView.context, R.anim.fade_out)
        mainView.startAnimation(fadeOut)
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                mainView.setBackgroundResource(R.drawable.item_list_2);
                var animationDrawable: AnimationDrawable = mainView.background as AnimationDrawable
                animationDrawable.setEnterFadeDuration(3000)
                animationDrawable.setExitFadeDuration(3000)
                animationDrawable.start()
                val fadeIn: Animation = AnimationUtils.loadAnimation(
                        mainView.context,
                        R.anim.fade_in
                )
                mainView.startAnimation(fadeIn)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }
    // animation used for change of states
    private fun fadeInOut2() {
        val mainView: ConstraintLayout = findViewById(R.id.constlayout)

        val fadeOut: Animation = AnimationUtils.loadAnimation(mainView.context, R.anim.fade_out)
        mainView.startAnimation(fadeOut)
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                mainView.setBackgroundResource(R.drawable.gradient_list);
                var animationDrawable: AnimationDrawable = mainView.background as AnimationDrawable
                animationDrawable.setEnterFadeDuration(3000)
                animationDrawable.setExitFadeDuration(3000)
                animationDrawable.start()
                val fadeIn: Animation = AnimationUtils.loadAnimation(
                        mainView.context,
                        R.anim.fade_in
                )
                mainView.startAnimation(fadeIn)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    latitude = location?.latitude!!
                    longitude = location?.longitude
                }
    }

    private fun getLocationUpdates() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 50000
        locationRequest.fastestInterval = 50000
        locationRequest.smallestDisplacement = 170f // 170 m = 0.1 mile
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //set according to your app function
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                if (locationResult.locations.isNotEmpty()) {
                    // get latest location
                    val location =
                            locationResult.lastLocation
                    latitude = location.latitude
                    longitude = location.longitude
                }
            }
        }
    }

    //start location updates
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null /* Looper */
        )
    }

    // stop location updates
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // stop receiving location update when activity not visible/foreground
    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
        stopLocationUpdates()
    }

    // start receiving location update when activity  visible/foreground
    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL)
        startLocationUpdates()
    }

    fun sendMessage(context: Context) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        // check if texts are enabled in settings
        val emergencyTextOnOff = sp.getBoolean("emergency_text_yesno", true)
        // check if location is enabled in settings
        val locationenable = sp.getBoolean("attach_location", false)
        if (emergencyTextOnOff) {
            // get number from settings
            val number = sp.getString("edit_emergency_number", "Empty")

            if(PhoneNumberUtils.isGlobalPhoneNumber(number)){
                // get message from settings
                val messageString = sp.getString("message_string", "Not set")
                // default message if not set
                val defaultMessage = "I am intoxicated, please help! "
                // if recent location is available & the user enabled location in settings send relevant message
                if (locationenable && longitude != 0.0 && latitude != 0.0) {
                    if (messageString != "Not set") {
                        SmsManager.getDefault().sendTextMessage(number, null, messageString + " I am at " + "https://maps.google.com/?q= " + latitude + "," + longitude, null, null)
                    } else {
                        SmsManager.getDefault().sendTextMessage(number, null, defaultMessage + " I am at " + "https://maps.google.com/?q=" + latitude + "," + longitude, null, null)
                    }
                } else {
                    if (messageString != "Not set") {
                        SmsManager.getDefault().sendTextMessage(number, null, messageString, null, null)
                    } else {
                        SmsManager.getDefault().sendTextMessage(number, null, defaultMessage, null, null)
                    }
                }
            }
            else{

            }
        }
    }

    private fun runModel(data: FloatArray): Int {
        // model instance from file
        val model = Model.newInstance(this)
        // create a tensorbuffer that takes a float array of size 8
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 8, 1), DataType.FLOAT32)
        // load data
        inputFeature0.loadArray(data)
        // process data
        val outputs = model.process(inputFeature0)
        // get outputs
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer
        // get prediction
        val classPrediction = if (outputFeature0.floatArray[0] >= 0.5) 0 else 1
        model.close()
        return classPrediction
    }

    @ExperimentalStdlibApi
    fun normalizeData() {
        val stdAccel = calculateStandardDeviation(accelerometerData)
        val stdAccelx = calculateStandardDeviation(accelerometerDatax)
        val stdAccely = calculateStandardDeviation(accelerometerDatay)
        val stdAccelz = calculateStandardDeviation(accelerometerDataz)

        val stdGyro = calculateStandardDeviation(gyroscopeData)
        val stdGyrox = calculateStandardDeviation(gyroscopeDatax)
        val stdGyroy = calculateStandardDeviation(gyroscopeDatay)
        val stdGyroz = calculateStandardDeviation(gyroscopeDataz)

        val sizes = intArrayOf(accelerometerData.size, gyroscopeData.size)
        val minval = sizes.min()
        if (accelerometerData.size != minval) {
            val dif = accelerometerData.size - minval!!
            for (i in 1..dif) {
                accelerometerData.removeLast()
                accelerometerDatax.removeLast()
                accelerometerDatay.removeLast()
                accelerometerDataz.removeLast()
            }
        }
        if (gyroscopeData.size != minval) {
            val dif = gyroscopeData.size - minval!!
            for (i in 1..dif) {
                gyroscopeData.removeLast()
                gyroscopeDatax.removeLast()
                gyroscopeDatay.removeLast()
                gyroscopeDataz.removeLast()
            }
        }

        // sums and sizes for normalizing data for model
        val sumAccel = accelerometerData.sum()
        val sumAccelx = accelerometerDatax.sum()
        val sumAccely = accelerometerDatay.sum()
        val sumAccelz = accelerometerDataz.sum()
        val sumGyro = gyroscopeData.sum()
        val sumGyrox = gyroscopeDatax.sum()
        val sumGyroy = gyroscopeDatay.sum()
        val sumGyroz = gyroscopeDataz.sum()
        val totalAccel = accelerometerData.size
        val totalAccelx = accelerometerDatax.size
        val totalAccely = accelerometerDatay.size
        val totalAccelz = accelerometerDataz.size
        val totalGyro = gyroscopeData.size
        val totalGyrox = gyroscopeDatax.size
        val totalGyroy = gyroscopeDatay.size
        val totalGyroz = gyroscopeDataz.size
        // check for array size being equal
        for (i in 0..accelerometerData.size-1) {
            accelerometerData[i] = (accelerometerData[i] - (sumAccel/totalAccel)) / stdAccel
            accelerometerDatax[i] = (accelerometerDatax[i] - (sumAccelx/totalAccelx)) / stdAccelx
            accelerometerDatay[i] = (accelerometerDatay[i] - (sumAccely/totalAccely)) / stdAccely
            accelerometerDataz[i] = (accelerometerDataz[i] - (sumAccelz/totalAccelz)) / stdAccelz
            gyroscopeData[i] = (gyroscopeData[i] - (sumGyro/totalGyro)) / stdGyro
            gyroscopeDatax[i] = (gyroscopeDatax[i] - (sumGyrox/totalGyrox)) / stdGyrox
            gyroscopeDatay[i] = (gyroscopeDatay[i] - (sumGyroy/totalGyroy)) / stdGyroy
            gyroscopeDataz[i] = (gyroscopeDataz[i] - (sumGyroz/totalGyroz)) / stdGyroz

        }
    }
    @ExperimentalStdlibApi
    fun calculatePrediction(): Boolean {
        normalizeData()
        var predYes = 0
        var predNo = 0
        for (i in 0..accelerometerData.size-1) {
            val getpred = runModel(floatArrayOf(accelerometerDatax[i],accelerometerDatay[i],accelerometerDataz[i], gyroscopeDatax[i],gyroscopeDatay[i],gyroscopeDataz[i],accelerometerData[i],gyroscopeData[i]))

            if (getpred == 0){
                predNo += 1
            }
            else{
                predYes += 1
            }
        }
        Log.wtf("PREDYES", "HERE" + predYes)
        Log.wtf("PREDNO", "HERE" + predNo)
        if (predYes > predNo) return true
        else if (predYes < predNo) return false
        else return false
    }

    @ExperimentalStdlibApi
    fun runPrediction() {
        val result = calculatePrediction()

        // clear arrays after getting the result for new data to come in
        accelerometerDatax.clear()
        accelerometerDatay.clear()
        accelerometerDataz.clear()
        gyroscopeData.clear()
        gyroscopeDatax.clear()
        gyroscopeDatay.clear()
        gyroscopeDataz.clear()
        accelerometerData.clear()
        // set new screen with the model prediction
        val resultText : TextView = findViewById(R.id.detectingtext)
        if (result){
            fadeInOut2()
            resultText.text = "Drunk"
        }
        else{
            fadeInOut()
            resultText.text = "Sober"
        }
    }

    fun calculateStandardDeviation(numArray: ArrayList<Float>): Float {
        var sum = 0.0
        var standardDeviation = 0.0

        for (num in numArray) {
            sum += num
        }
        val mean = sum / numArray.size

        for (num in numArray) {
            standardDeviation += Math.pow(num - mean, 2.0)
        }
        val ret = sqrt(standardDeviation / (numArray.size - 1))
        return ret.toFloat()
    }
    // needed for certain Tflite models
    fun floatsToBytes(floats: FloatArray): ByteArray? {
        val bytes = ByteArray(java.lang.Float.BYTES * floats.size)
        ByteBuffer.wrap(bytes).asFloatBuffer().put(floats)
        return bytes

    }

}