package andre.debatetimer.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.support.design.widget.Snackbar
import android.view.View

var clipboard: ClipboardManager? = null

fun copyText(view: View, label: String, textToCopy: String, textToShow: String? = null): Boolean {
	val clipboard = clipboard
	if (clipboard != null) {
		clipboard.primaryClip = ClipData.newPlainText(label, textToCopy)
		Snackbar.make(view, (textToShow ?: textToCopy) + " copied", Snackbar.LENGTH_SHORT).show()
	}
	return true
}