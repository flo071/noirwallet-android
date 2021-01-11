package org.noirofficial.presenter.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;

public class ExpandableTextView extends AppCompatTextView implements View.OnClickListener {
    private static final int MAX_LINES = 3;
    private int currentMaxLines = Integer.MAX_VALUE;

    public ExpandableTextView(Context context) {
        super(context);
        setOnClickListener(this);
    }

    public ExpandableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnClickListener(this);
    }

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        post(() -> setMaxLines(MAX_LINES));
    }

    @Override
    public void setMaxLines(int maxLines) {
        currentMaxLines = maxLines;
        super.setMaxLines(maxLines);
    }

    public int getMyMaxLines() {
        return currentMaxLines;
    }

    @Override
    public void onClick(View v) {
        if (getMyMaxLines() == Integer.MAX_VALUE)
            setMaxLines(MAX_LINES);
        else
            setMaxLines(Integer.MAX_VALUE);
    }
}