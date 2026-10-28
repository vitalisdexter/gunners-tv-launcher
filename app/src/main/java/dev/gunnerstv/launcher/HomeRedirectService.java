package dev.gunnerstv.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

/** Only the stock HOME window triggers this helper. No key filtering, screen reads or notifications. */
public final class HomeRedirectService extends AccessibilityService {
    private long last;
    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        int boot=Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,-1);
        android.content.SharedPreferences p=getSharedPreferences("home_boot",0);
        if(boot>=0&&p.getInt("last_boot",-1)!=boot&&SystemClock.elapsedRealtime()<5*60_000L){
            p.edit().putInt("last_boot",boot).apply();
            // Re-emit the foreground window after a late service bind, without reading its contents.
            try{startActivity(new Intent(this,HomeProbeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION));}
            catch(android.content.ActivityNotFoundException|SecurityException ignored){}
        }
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        if(e.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||!getSharedPreferences("gunners",0).getBoolean("redirect_home",true))return;
        if(!"com.google.android.apps.tv.launcherx".contentEquals(e.getPackageName()==null?"":e.getPackageName()))return;
        if(!"com.google.android.apps.tv.launcherx.home.HomeActivity".contentEquals(e.getClassName()==null?"":e.getClassName()))return;
        long now=SystemClock.elapsedRealtime();if(now-last<700)return;last=now;
        // ACTION_MAIN is deliberately absent: Google TV remains the system Home process for remote app buttons.
        startActivity(new Intent(this,MainActivity.class).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
    }
    @Override public void onInterrupt(){}
}
