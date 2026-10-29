package dev.gunnerstv.launcher;

import android.content.Context;
import android.content.SharedPreferences;

final class FootballSettings {
    private static SharedPreferences store(Context c){return c.getSharedPreferences("gunners",0);}
    static FootballSelection load(Context c){return FootballSelection.decode(store(c).getString("football_selection",""));}
    static String signature(Context c){return store(c).getString("football_selection","");}
    static boolean completed(Context c){return store(c).getBoolean("football_setup_complete",false);}
    static void save(Context c,FootballSelection selection){store(c).edit().putString("football_selection",selection.encode()).putBoolean("football_setup_complete",true).apply();}
}
