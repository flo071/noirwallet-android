package org.noirofficial.tools.util;

import android.graphics.Rect;
import android.view.TouchDelegate;
import android.view.View;

public class ViewUtils {

    public static void increaceClickableArea(View view) {
        final View parent = (View) view.getParent();
        parent.post(() -> {
            final Rect rect = new Rect();
            view.getHitRect(rect);
            rect.top -= 100;    // increase top hit area
            rect.left -= 100;   // increase left hit area
            rect.bottom += 100; // increase bottom hit area
            rect.right += 100;  // increase right hit area
            parent.setTouchDelegate( new TouchDelegate( rect , view));
        });
    }
}
