package dev.gunnerstv.launcher;

import android.service.notification.NotificationListenerService;

/** Exists only so Android lets the Now Playing widget see media sessions (MediaSessionManager requires an enabled
 *  notification listener). It overrides nothing: Gunner's never reads, keeps or dismisses notifications. */
public final class MediaListener extends NotificationListenerService {}

