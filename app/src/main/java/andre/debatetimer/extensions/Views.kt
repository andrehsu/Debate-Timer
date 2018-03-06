@file:Suppress("NOTHING_TO_INLINE")

package andre.debatetimer.extensions

import andre.debatetimer.EnvVars.shortAnimTime
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewPropertyAnimator

fun ViewPropertyAnimator.setOnEnd(onEnd: (Animator?) -> Unit): ViewPropertyAnimator {
	setListener(object : AnimatorListenerAdapter() {
		override fun onAnimationEnd(animation: Animator?) {
			onEnd(animation)
		}
	})
	return this
}

inline fun View.fadeIn(animTime: Long = shortAnimTime): ViewPropertyAnimator = this.let {
	it.alpha = 0.0f
	it.setVisible()
	it.animate().alpha(1.0f).setDuration(animTime).setListener(null)
}

inline fun View.fadeOut(animTime: Long = shortAnimTime): ViewPropertyAnimator = this.animate().alpha(0.0f).setDuration(animTime).setOnEnd { this.setGone();this.alpha = 1.0f }


fun crossFade(from: View, to: View, animTime: Long = -1) {
	val outAnimator = from.fadeOut()
	val inAnimator = to.fadeOut()
	
	if (animTime != -1L) {
		outAnimator.duration = animTime
		inAnimator.duration = animTime
	}
}


inline infix fun View.replaceWith(to: View) {
	this.setGone()
	to.setVisible()
}

inline fun View.setVisible() {
	this.visibility = View.VISIBLE
}

inline fun View.setInvisible() {
	this.visibility = View.INVISIBLE
}

inline fun View.setGone() {
	this.visibility = View.GONE
}

inline fun View.ifVisible(block: (View) -> Unit): View {
	if (visibility == View.VISIBLE)
		block(this)
	return this
}

inline fun View.ifInvisible(block: (View) -> Unit): View {
	if (visibility == View.INVISIBLE)
		block(this)
	return this
}

inline fun View.ifGone(block: (View) -> Unit): View {
	if (visibility == View.GONE)
		block(this)
	return this
}