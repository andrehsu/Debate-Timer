package andre.debatetimer.timer

import andre.debatetimer.R
import andre.debatetimer.extensions.EnvVars
import android.media.MediaPlayer

enum class DebateBell(private val bell: MediaPlayer) {
	ONCE(MediaPlayer.create(EnvVars.applicationContext, R.raw.debate_bell_one)),
	TWICE(MediaPlayer.create(EnvVars.applicationContext, R.raw.debate_bell_two));
	
	companion object {
		var debateBellEnabled = true
	}
	
	@Synchronized fun ring() {
		if (debateBellEnabled)
			bell.start()
	}
}