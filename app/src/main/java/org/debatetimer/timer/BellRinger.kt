package org.debatetimer.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import org.debatetimer.R

class BellRinger(context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                    AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            )
            .build()
    private val debateBellOnce = soundPool.load(context, R.raw.debate_bell_one, 1)
    private val debateBellTwice = soundPool.load(context, R.raw.debate_bell_two, 1)
    
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