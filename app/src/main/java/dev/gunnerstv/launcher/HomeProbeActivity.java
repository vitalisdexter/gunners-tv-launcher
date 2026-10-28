package dev.gunnerstv.launcher;
import android.app.Activity;
import android.os.Bundle;

/** A transparent boot-only window. Closing it re-emits the previously visible activity's window event. */
public final class HomeProbeActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().getDecorView().post(()->{finish();overridePendingTransition(0,0);});}
}
