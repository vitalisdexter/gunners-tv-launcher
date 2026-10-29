package dev.gunnerstv.launcher;

import java.util.*;

/** Stable row identities; each provider keeps its own ranking and resume state. */
final class FeedCatalog {
    static final String YOUTUBE="com.google.android.youtube.tv", NETFLIX="com.netflix.ninja",
            PRIME="com.amazon.amazonvideo.livingroom", APPLE="com.apple.atve.androidtv.appletv",
            SPOTIFY="com.spotify.tv.android";
    static final class Row {
        final String id,title,pkg; final boolean resume,music;
        Row(String id,String title,String pkg,boolean resume,boolean music){this.id=id;this.title=title;this.pkg=pkg;this.resume=resume;this.music=music;}
        List<ContentFeedPolicy.Item> select(ContentFeedReader.Result feed){
            List<ContentFeedPolicy.Item> source=resume?feed.continuing:feed.recommendations;
            ArrayList<ContentFeedPolicy.Item> out=new ArrayList<>();
            for(ContentFeedPolicy.Item item:source){
                boolean provider=pkg.equals(item.pkg)||(music&&YOUTUBE.equals(pkg)&&"com.google.android.youtube.tvmusic".equals(item.pkg));
                if(provider&&(!music||ContentFeedPolicy.music(item)))out.add(item);
                if(out.size()==24)break;
            }
            return out;
        }
    }
    static final Row[] ROWS={
        new Row("youtube_resume","YouTube · Continue Watching",YOUTUBE,true,false),
        new Row("netflix_resume","Netflix · Continue Watching",NETFLIX,true,false),
        new Row("prime_resume","Prime Video · Continue Watching",PRIME,true,false),
        new Row("apple_resume","Apple TV · Continue Watching",APPLE,true,false),
        new Row("youtube_picks","YouTube · Recommended for you",YOUTUBE,false,false),
        new Row("netflix_picks","Netflix · Recommended for you",NETFLIX,false,false),
        new Row("prime_picks","Prime Video · Recommended for you",PRIME,false,false),
        new Row("apple_picks","Apple TV · Recommended for you",APPLE,false,false),
        new Row("youtube_music","Music from YouTube",YOUTUBE,false,true),
        new Row("spotify_picks","Spotify · For you",SPOTIFY,false,true)
    };
    static Row row(String id){for(Row row:ROWS)if(row.id.equals(id))return row;return null;}
    static Set<String> available(ContentFeedReader.Result result){
        Set<String> ids=new HashSet<>();for(Row row:ROWS)if(!row.select(result).isEmpty())ids.add(row.id);return ids;
    }
    static Set<String> availableIds(ContentFeedReader.Result current,Set<String> cached){return current==null?new HashSet<>(cached):available(current);}
    static volatile ContentFeedReader.Result latest;
}
