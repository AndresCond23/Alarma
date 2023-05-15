package com.ingenieriajhr.alarma

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.ingenieriajhr.alarma.databinding.ActivityMainBinding
import com.ingenieriajhr.blujhr.BluJhr

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding : ActivityMainBinding
    //create instance object bluJhr
    private lateinit var bluJhr: BluJhr

    //arrayList devices Bluetooth
    private var devicesBluetooth = ArrayList<String>()

    //soundAlarm
    private lateinit var soundAlarma: SoundAlarma

    //alarm state
    private var alarmState = false

    //animationStart
    private  var animationEnd = true

    //alarmSoundActivate
    private var alarmSoundActivate = true

    //vibrator
    lateinit var vibrator: Vibrator

    //value max
    val VALUE_MAX = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //color bar
        window.statusBarColor = ContextCompat.getColor(this,R.color.purple_500)

        //init blueJhr
        bluJhr = BluJhr(this)
        //onBluetooth
        bluJhr.onBluetooth()

        //init event soundAlarm
        soundAlarma = SoundAlarma(this)

        //clickListView
        eventClickDevice()

        //btnSoundClick
        binding.btnSound.setOnClickListener(this)
        binding.btnBluetoothDesactive.setOnClickListener(this)

        //init vibrator
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    }

    /**
     * event click listDevice
     */
    private fun eventClickDevice() {
        binding.listDevices.setOnItemClickListener { adapterView, view, i, l ->

            if (devicesBluetooth.isNotEmpty()){
                bluJhr.connect(devicesBluetooth[i])

                bluJhr.setDataLoadFinishedListener(object :BluJhr.ConnectedBluetooth{
                    override fun onConnectState(state: BluJhr.Connected) {
                        //state conect
                        when(state){
                            //connected sucesfull
                            BluJhr.Connected.True->{
                                Toast.makeText(applicationContext, "Conexion exitosa", Toast.LENGTH_SHORT).show()
                                binding.devicesView.visibility = View.GONE
                                binding.controlView.visibility = View.VISIBLE
                                rxArduino()
                            }

                            BluJhr.Connected.Pending->{
                                Toast.makeText(applicationContext, "Conectando...", Toast.LENGTH_SHORT).show()
                            }

                            BluJhr.Connected.False->{
                                Toast.makeText(applicationContext, "No se pudo conectar el dispositivo", Toast.LENGTH_SHORT).show()

                            }

                            BluJhr.Connected.Disconnect->{
                                disconectBluetooth()
                            }
                        }
                    }
                })
            }
        }

    }

    /**
     * Rx data arduino
     */
    private fun rxArduino() {
        bluJhr.loadDateRx(object :BluJhr.ReceivedData{
            override fun rxDate(rx: String) {
                //show text rx in TextView
                binding.txtRx.text = rx
                try {
                    //conver string to float
                    val dataNumber = rx.toFloat()
                    //value up alert
                    if (dataNumber>VALUE_MAX){
                        //change state alert to true
                        alarmState = true
                        //if alarm sound is active so play alarm
                        if (alarmSoundActivate)soundAlarma.playAlarm()
                        //if animation is end so play animation, repeat
                        if(animationEnd) animationButton()
                    }else{
                       stopAll()
                    }

                }catch (e:java.lang.NumberFormatException){
                    Toast.makeText(applicationContext, "Error en información", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /**
     * stop al elements, animation and sound
     */
    private fun stopAll() {
        //stop sound if dataNumber no alert
        soundAlarma.stopAlarm()
        //alarm state = false
        alarmState = false
        //animation end true
        animationEnd = true
    }

    /**
     * animation button
     */
    private fun animationButton() {
        animationEnd = false
        val anim = RotateAnimation(0f,360f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f)
        anim.setAnimationListener(object: Animation.AnimationListener{
            override fun onAnimationStart(p0: Animation?) {

            }

            override fun onAnimationEnd(p0: Animation?) {
                //execute change in principal thread else generate conflict
                runOnUiThread {
                    //if background tint is purple so now is red
                    if (binding.btnNotifications.backgroundTintList == ContextCompat.getColorStateList(applicationContext,R.color.purple_500)){
                        binding.btnNotifications.backgroundTintList = ContextCompat.getColorStateList(applicationContext,R.color.red)
                    }else{
                        //else now is purple
                        binding.btnNotifications.backgroundTintList = ContextCompat.getColorStateList(applicationContext,R.color.purple_500)
                        //end animation for repeat
                        animationEnd = true
                    }
                }
                //animationEnd is false so animationButton execute
                if (!animationEnd){
                    vibrateFunction()
                    animationButton()
                }
            }
            override fun onAnimationRepeat(p0: Animation?) {
            }

        })
        //duration animation
        anim.duration = 800
        //execute change in principal thread
        runOnUiThread {
            binding.btnNotifications.startAnimation(anim)
        }
    }

    /**
     * vibra movil
     */
    private fun vibrateFunction() {
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            vibrator.vibrate(500);
        }
    }


    /**
     * disconect bluetooth
     */
    private fun disconectBluetooth() {
        Toast.makeText(this,"Bluetooth desconectado",Toast.LENGTH_SHORT).show()
        //change views
        binding.devicesView.visibility = View.VISIBLE
        binding.controlView.visibility = View.GONE
    }


    /**
     * @param p0 View
     */
    override fun onClick(p0: View?) {
        when(p0){
            binding.btnSound->{
                alarmSoundActivate = !alarmSoundActivate
                if (!alarmSoundActivate){
                    soundAlarma.stopAlarm()
                    runOnUiThread {
                        binding.btnSound.setBackgroundResource(R.drawable.baseline_play_circle_filled_24)
                    }
                }else{
                    runOnUiThread {
                        binding.btnSound.setBackgroundResource(R.drawable.baseline_stop_circle_24)
                    }
                }
            }
            binding.btnBluetoothDesactive->{
                stopAll()
                bluJhr.closeConnection()
            }
        }
    }




    /**
     * pedimos los permisos correspondientes, para android 12 hay que pedir los siguientes BLUETOOTH_SCAN y BLUETOOTH_CONNECT
     * en android 12 o superior se requieren permisos adicionales
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (bluJhr.checkPermissions(requestCode,grantResults)){
            Toast.makeText(this, "Permisos otorgados", Toast.LENGTH_SHORT).show()
            bluJhr.initializeBluetooth()
        }else{
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
                bluJhr.initializeBluetooth()
            }else{
                Toast.makeText(this, "Algo salio mal", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * on Active Bluetooth
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!bluJhr.stateBluetoooth() && requestCode == 100){
            bluJhr.initializeBluetooth()
        }else{
            if (requestCode == 100){
                devicesBluetooth = bluJhr.deviceBluetooth()
                if (devicesBluetooth.isNotEmpty()){
                    val adapter = ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1,devicesBluetooth)
                    binding.listDevices.adapter = adapter
                }else{
                    Toast.makeText(this, "No tienes vinculados dispositivos", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }




}