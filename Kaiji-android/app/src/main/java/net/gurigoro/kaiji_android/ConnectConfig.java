package net.gurigoro.kaiji_android;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by takahito on 2016/10/14.
 */

public class ConnectConfig {
    public final static boolean OFFLINE = true;

    public static String getServerAddress(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_key_server_address), "");
    }

    public static int getServerPort(Context context){
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_key_server_port), "1257"));
    }

    public static String getAccessKey(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_key_access_key), "");
    }
}
