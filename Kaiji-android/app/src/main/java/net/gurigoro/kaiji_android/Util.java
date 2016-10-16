package net.gurigoro.kaiji_android;

import android.content.Context;

/**
 * Created by takahito on 2016/10/16.
 */

public class Util {
    public static int convertPxToDp(Context context, int px){
        float d = context.getResources().getDisplayMetrics().density;
        return (int)((px / d) + 0.5);
    }

    public static int convertDpToPx(Context context, int dp){
        float d = context.getResources().getDisplayMetrics().density;
        return (int)((dp * d) + 0.5);
    }
}
