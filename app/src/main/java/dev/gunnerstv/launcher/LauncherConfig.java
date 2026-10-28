package dev.gunnerstv.launcher;

import android.content.Context;
import android.content.SharedPreferences;

final class LauncherConfig {
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("gunners",0);}
    static String city(Context c){String name=prefs(c).getString("weather_city",c.getString(R.string.weather_city)).trim();return name.isEmpty()?Daylight.city():name;}
    static double[] place(Context c){
        SharedPreferences p=prefs(c);
        double[] place=LocationConfig.coordinates(p.getString("weather_latitude",c.getString(R.string.weather_latitude)),p.getString("weather_longitude",c.getString(R.string.weather_longitude)));
        return place==null?Daylight.place(c):place;
    }
}
