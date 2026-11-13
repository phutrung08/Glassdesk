package io.github.phutrung08.glassdeck;

import android.content.Context;
import android.widget.ScrollView;

/** ScrollView co theo nội dung nhưng không cao quá giới hạn; quá thì cuộn. */
final class MaxHeightScroll extends ScrollView {
    int maxHeight = Integer.MAX_VALUE;

    MaxHeightScroll(Context c) {
        super(c);
    }

    @Override
    protected void onMeasure(int w, int h) {
        super.onMeasure(w, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
    }
}
