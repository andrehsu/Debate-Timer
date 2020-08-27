package org.debatetimer.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import org.debatetimer.R

class BellRinger(context: Context) {
    private var soundPool: SoundPool
    private var debateBellOnce: Int = -1
    private var debateBellTwice: Int = -1
    
    init {
        
        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        soundPool = SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attributes)
                .build()
        
        debateBellOnce = soundPool.load(context, R.raw.debate_bell_one, 1)
        debateBellTwice = soundPool.load(context, R.raw.debate_bell_two, 1)
    }
    
    
    fun playBell(bell: DebateBell) {
        soundPool.play(
                when (bell) {
                    DebateBell.Once -> debateBellOnce
                    DebateBell.Twice -> debateBellTwice
                },
                1f,
                1f,
                1,
                0,
                1f
        )
    }
}

enum class DebateBell {
    Once, Twice
}