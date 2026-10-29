package dev.gunnerstv.launcher;

import android.service.notification.NotificationListenerService;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Exists only so Android lets the Now Playing widget see media sessions (MediaSessionManager requires an enabled
 *  notification listener). Only connection lifecycle is observed; no notification contents are read. */
public final class MediaListener extends NotificationListenerService {
    private static final Set<Runnable> observers=new CopyOnWriteArraySet<>();
    static void observe(Runnable r){observers.add(r);}
    static void remove(Runnable r){observers.remove(r);}
    private void changed(){for(Runnable r:observers)r.run();}
    @Override public void onListenerConnected(){super.onListenerConnected();changed();}
    @Override public void onListenerDisconnected(){super.onListenerDisconnected();changed();}
}

