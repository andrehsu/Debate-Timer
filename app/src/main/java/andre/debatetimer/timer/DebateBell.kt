package andre.debatetimer.timer

import andre.debatetimer.Prefs
import andre.debatetimer.R
import andre.debatetimer.extensions.EnvVars
import android.media.MediaPlayer

enum class DebateBell(private val bell: MediaPlayer) {
	ONCE(MediaPlayer.create(EnvVars.applicationContext, R.raw.debate_bell_one)),
	TWICE(MediaPlayer.create(EnvVars.applicationContext, R.raw.debate_bell_two));
	
	@Synchronized fun ring() {
		if (Prefs.debateBellEnabled)
			bell.start()
	}
}