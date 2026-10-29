package dev.gunnerstv.launcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Selection of actual app-published programs, shared by the TV UI and JVM tests. */
final class ContentFeedPolicy {
    static final class Item {
        String pkg = "", app = "", title = "", art = "", intent = "", episode = "", description = "", channel = "";
        String contentId = "", genre = "", author = "", channelTitle = "";
        int type = -1;
        boolean live;
        long position, duration, engaged, weight;
        int watchType = -1;
        String key() { return pkg + "\n" + (contentId.isEmpty() ? title + "\n" + episode : "id:" + contentId); }
    }

    static List<Item> recommendations(List<Item> source, int maximum) {
        if(maximum<=0)return new ArrayList<>();
        Map<String, List<Item>> groups = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Item item : source) {
            if (item.title.isEmpty() || item.intent.isEmpty() || !seen.add(item.key())) continue;
            groups.computeIfAbsent(item.pkg, ignored -> new ArrayList<>()).add(item);
        }
        List<Item> result = new ArrayList<>();
        for (int round = 0; result.size() < maximum; round++) {
            boolean found = false;
            for (List<Item> group : groups.values()) {
                if (round < group.size() && result.size() < maximum) {
                    result.add(group.get(round));
                    found = true;
                }
            }
            if (!found) break;
        }
        return result;
    }

    static List<Item> continuing(List<Item> source, int maximum) {
        if(maximum<=0)return new ArrayList<>();
        List<Item> sorted = new ArrayList<>(source);
        sorted.sort(Comparator.comparingLong((Item item) -> item.engaged).reversed());
        List<Item> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Item item : sorted) {
            boolean legacyProgress=item.watchType==-1&&item.position>0&&item.duration>item.position;
            if ((item.watchType != 0 && item.watchType != 1&&!legacyProgress) || item.title.isEmpty()
                    || item.intent.isEmpty() || !seen.add(item.key())) continue;
            result.add(item);
            if (result.size() == maximum) break;
        }
        return result;
    }

    static boolean supported(String pkg) {
        return "com.google.android.youtube.tv".equals(pkg) || "com.netflix.ninja".equals(pkg)
                || "com.amazon.amazonvideo.livingroom".equals(pkg) || "com.apple.atve.androidtv.appletv".equals(pkg)
                || "com.google.android.youtube.tvmusic".equals(pkg) || "com.spotify.tv.android".equals(pkg);
    }

    /** Conservative classification: a generic YouTube playlist or a title mentioning music is not enough. */
    static boolean music(Item item) {
        if("com.google.android.youtube.tvmusic".equals(item.pkg)||"com.spotify.tv.android".equals(item.pkg))return true;
        if(!"com.google.android.youtube.tv".equals(item.pkg))return false;
        if(item.type>=7&&item.type<=9)return true; // TV provider: track, album, artist.
        for(String genre:item.genre.toUpperCase(java.util.Locale.ROOT).split(","))
            if("MUSIC".equals(genre.trim()))return true;
        String channel=item.channelTitle.trim().toLowerCase(java.util.Locale.ROOT);
        if(channel.equals("music")||channel.equals("music videos")||channel.equals("your music")||channel.equals("recommended music"))return true;
        return false;
    }

    static boolean plutoLive(Item item, String channelName) {
        String name = channelName.toLowerCase(java.util.Locale.ROOT);
        return "tv.pluto.android".equals(item.pkg)
                && (item.live || name.contains("featured channels") || name.contains("live"));
    }

    static int progress(long position, long duration) {
        if (duration <= 0) return -1;
        return (int) Math.max(0, Math.min(100, 100.0 * Math.max(0, position) / duration));
    }
}
