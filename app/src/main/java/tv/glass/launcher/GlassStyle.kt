package tv.glass.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.widget.TextView

fun Context.dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
fun glassSurface(context: Context, dark: Boolean = true, focused: Boolean = false, radius: Int = 24) = GradientDrawable(
    GradientDrawable.Orientation.TL_BR,
    if (focused) intArrayOf(0xAA92BCEB.toInt(), 0x995675A4.toInt())
    else if (dark) intArrayOf(0xAA536783.toInt(), 0xBB24344D.toInt())
    else intArrayOf(0xCCDFE9F5.toInt(), 0xBBA8BED8.toInt())
).apply {
    cornerRadius = context.dp(radius).toFloat()
    setStroke(context.dp(if (focused) 2 else 1), if (focused) 0xFFE0F4FF.toInt() else 0x55E0EDFF)
}
fun focusSurface(context: Context, dark: Boolean, radius: Int = 18) = StateListDrawable().apply {
    addState(intArrayOf(android.R.attr.state_focused), glassSurface(context, dark, true, radius))
    addState(intArrayOf(), glassSurface(context, dark, false, radius))
}
fun Context.label(text: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
    this.text = text; textSize = size; setTextColor(color); typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    setShadowLayer(2f, 0f, 1f, 0x44000000)
}
fun View.focusAnimation(reduceMotion: Boolean, scale: Float = 1.07f) {
    setOnFocusChangeListener { view, focused ->
        view.animate().cancel()
        view.animate().scaleX(if (focused) scale else 1f).scaleY(if (focused) scale else 1f)
            .translationZ(if (focused) context.dp(10).toFloat() else 0f).setDuration(if (reduceMotion) 0 else 150).start()
    }
}
