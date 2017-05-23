package andre.debatetimer

import andre.debatetimer.extensions.EnvVars
import android.media.MediaPlayer

enum class DebateBell(val intValue: Int, private val bell: MediaPlayer) {
	ONCE(1, MediaPlayer.create(EnvVars.applicationContext, R.raw.debate_bell_one)), TWICE(2, MediaPlayer.create(EnvVars.applicationContext, R.raw.debate_bell_two));
	
	fun ring() {
		if (debateBellEnabled)
			bell.start()
	}
	
	companion object {
		var debateBellEnabled = true
	}
}