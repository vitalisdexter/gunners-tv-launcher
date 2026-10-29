package dev.gunnerstv.launcher;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads only the platform's published program tables; never reads another app's private files. */
final class ContentFeedReader {
    static final class Result {
        final List<ContentFeedPolicy.Item> recommendations = new ArrayList<>();
        final List<ContentFeedPolicy.Item> continuing = new ArrayList<>();
        final List<ContentFeedPolicy.Item> live = new ArrayList<>();
        int publishedRecommendations, publishedWatchNext;
        String error = "";
        final ContentFeedStatus status=new ContentFeedStatus();
    }
    private final Context context;
    private final Map<String, String> names = new HashMap<>();
    private final Map<String, String> channels = new HashMap<>();
    ContentFeedReader(Context context) { this.context = context; }

    Result read() {
        Result result = new Result();
        result.status.listingsGranted=context.checkSelfPermission("android.permission.READ_TV_LISTINGS")==PackageManager.PERMISSION_GRANTED;
        try(Cursor c=context.getContentResolver().query(Uri.parse("content://android.media.tv/channel"),new String[]{"_id","display_name"},null,null,null)){
            while(c!=null&&c.moveToNext())channels.put(value(c,"_id"),value(c,"display_name"));
        }catch(RuntimeException ignored){}
        List<ContentFeedPolicy.Item> preview = readTable("preview_program", result);
        List<ContentFeedPolicy.Item> watch = readTable("watch_next_program", result);
        result.publishedRecommendations = preview.size();
        result.publishedWatchNext = watch.size();
        preview.sort(Comparator.comparingInt((ContentFeedPolicy.Item item) -> priority(item.pkg))
                .thenComparing(Comparator.comparingLong((ContentFeedPolicy.Item item) -> item.weight).reversed()));
        // Filter by provider before applying each row's six-card limit: busy apps must not crowd out others.
        result.recommendations.addAll(ContentFeedPolicy.recommendations(preview, 1500));
        result.continuing.addAll(ContentFeedPolicy.continuing(watch, 1500));
        if(result.status.deniedTables>0||result.status.failedTables>0)result.error=result.status.message();
        return result;
    }

    private List<ContentFeedPolicy.Item> readTable(String table, Result result) {
        List<ContentFeedPolicy.Item> items = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://android.media.tv/" + table), null, null, null, null)) {
            if(cursor==null){result.status.failedTables++;return items;}
            result.status.readTables++;
            int scanned=0;
            while (cursor != null && cursor.moveToNext() && scanned++ < 3000 && items.size() < 1500) {
                if("preview_program".equals(table))result.status.previewRows++;else result.status.watchRows++;
                try{
                if (number(cursor, "browsable", 1) == 0) {result.status.hiddenRows++;continue;}
                String pkg = value(cursor, "package_name");
                if (!ContentFeedPolicy.supported(pkg)) {result.status.unsupportedRows++;continue;}
                String name = appName(pkg);
                if (name.isEmpty()) {result.status.invalidRows++;continue;}
                ContentFeedPolicy.Item item = new ContentFeedPolicy.Item();
                item.pkg = pkg;
                item.app = name;
                item.title = value(cursor, "title");
                item.episode = value(cursor, "episode_title");
                item.description = value(cursor, "short_description");
                item.channel = value(cursor, "channel_id");
                item.channelTitle = channels.getOrDefault(item.channel, "");
                item.contentId = value(cursor, "content_id");
                item.genre = value(cursor, "canonical_genre");
                item.author = value(cursor, "author");
                item.type = (int) number(cursor, "type", -1);
                item.live = number(cursor, "live", 0) == 1;
                item.art = value(cursor, "poster_art_uri");
                if (item.art.isEmpty()) item.art = value(cursor, "thumbnail_uri");
                item.intent = value(cursor, "intent_uri");
                item.position = number(cursor, "last_playback_position_millis", 0);
                item.duration = number(cursor, "duration_millis", 0);
                item.engaged = number(cursor, "last_engagement_time_utc_millis", 0);
                item.weight = number(cursor, "weight", 0);
                item.watchType = (int) number(cursor, "watch_next_type", -1);
                if (!item.title.isEmpty() && !item.intent.isEmpty()) items.add(item);else result.status.invalidRows++;
                }catch(RuntimeException malformed){result.status.invalidRows++;}
            }
        } catch (SecurityException denied) {
            result.status.deniedTables++;
        } catch (RuntimeException unavailable) {
            result.status.failedTables++;
        }
        return items;
    }

    private String appName(String pkg) {
        if (names.containsKey(pkg)) return names.get(pkg);
        String name = "";
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(pkg, 0);
            if (info.enabled) name = context.getPackageManager().getApplicationLabel(info).toString();
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
        names.put(pkg, name);
        return name;
    }

    private static int priority(String pkg) {
        String[] preferred = {"com.google.android.youtube.tv", "com.netflix.ninja",
                "com.amazon.amazonvideo.livingroom", "com.apple.atve.androidtv.appletv"};
        for (int i = 0; i < preferred.length; i++) if (preferred[i].equals(pkg)) return i;
        return preferred.length;
    }
    private static String value(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index < 0 || cursor.isNull(index) ? "" : cursor.getString(index);
    }
    private static long number(Cursor cursor, String column, long fallback) {
        int index = cursor.getColumnIndex(column);
        return index < 0 || cursor.isNull(index) ? fallback : cursor.getLong(index);
    }
}
