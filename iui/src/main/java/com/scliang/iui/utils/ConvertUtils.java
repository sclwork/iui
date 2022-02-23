package com.scliang.iui.utils;

import android.content.Context;

public final class ConvertUtils {
    public static int dp2px(final Context context, final float dp) {
        if (context == null) return (int) dp;
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
