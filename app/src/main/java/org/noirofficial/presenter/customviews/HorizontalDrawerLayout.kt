package org.noirofficial.presenter.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.drawerlayout.widget.DrawerLayout

class HorizontalDrawerLayout(context: Context, attrs: AttributeSet?) : DrawerLayout(context, attrs) {

    var downY: Float = 0f
    var downX: Float = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            downY = ev.y
            downX = ev.x
        }
        if (ev?.action == MotionEvent.ACTION_MOVE) {
            if (Math.abs(ev.y - downY) > Math.abs(ev.x - downX)) {
                return false;
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}