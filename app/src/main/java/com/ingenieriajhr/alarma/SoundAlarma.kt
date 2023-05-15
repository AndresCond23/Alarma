package com.ingenieriajhr.alarma

import android.app.Activity
import android.content.Context
import android.media.MediaPlayer

class SoundAlarma(val context: Context) {

    //media alarm
    var alarm = MediaPlayer.create(context,R.raw.alarma)

    //endAlarm
    var endAlarm  = true


    fun playAlarm(){
        if (endAlarm){
            endAlarm = false
            alarm = MediaPlayer.create(context,R.raw.alarma)
            alarm.setOnCompletionListener {
                stopAlarm()
            }
            alarm.start()
        }
    }

    fun stopAlarm(){
        if (!endAlarm){
            alarm.stop()
            //remove mediaPlayer
            alarm.release()
            alarm = null
            endAlarm = true
        }
    }

}