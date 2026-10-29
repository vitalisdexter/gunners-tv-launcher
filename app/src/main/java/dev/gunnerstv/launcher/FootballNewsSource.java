package dev.gunnerstv.launcher;

import android.content.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

final class FootballNewsSource {
    static final ExecutorService IO=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"Gunner creator feeds");t.setDaemon(true);return t;});
    static final long CACHE=30*60000L;
    static boolean fetchDue(long now,long fetched,long attempted,boolean force){return (attempted<=0||attempted>now||now-attempted>=60000L)&&(force||fetched<=0||fetched>now||now-fetched>=CACHE);}
    static final class Result {final List<YouTubeFeeds.Video> videos=new ArrayList<>();int sources,failed;}
    static Result load(Context c,boolean force){
        SharedPreferences cache=c.getSharedPreferences("football_news_cache",0);long now=System.currentTimeMillis();Result out=new Result();LinkedHashMap<String,YouTubeFeeds.Video> merged=new LinkedHashMap<>();Map<String,List<YouTubeFeeds.Video>> loaded=new HashMap<>();
        for(FootballNewsSources.Source source:FootballNewsSources.list(c)){
            if(Thread.currentThread().isInterrupted())break;
            List<YouTubeFeeds.Video> videos=loaded.get(source.channel);
            if(videos==null){out.sources++;videos=new ArrayList<>();String raw=cache.getString("items_"+source.channel,"[]");
                if(fetchDue(now,cache.getLong("at_"+source.channel,0),cache.getLong("try_"+source.channel,0),force)){
                    cache.edit().putLong("try_"+source.channel,now).apply();try{YouTubeFeeds.Feed feed=YouTubeFeeds.fetch(source.channel,now);JSONArray a=new JSONArray();for(YouTubeFeeds.Video v:feed.videos)a.put(v.json());raw=a.toString();cache.edit().putString("items_"+source.channel,raw).putLong("at_"+source.channel,now).apply();}catch(Exception e){out.failed++;}
                }
                try{JSONArray a=new JSONArray(raw);for(int i=0;i<Math.min(a.length(),30);i++){YouTubeFeeds.Video v=YouTubeFeeds.Video.json(a.optJSONObject(i));if(v!=null&&source.channel.equals(v.channel)&&v.published>=now-YouTubeFeeds.WEEK&&v.published<=now+5*60000L&&!v.title.isEmpty())videos.add(v);}}catch(JSONException ignored){}
                loaded.put(source.channel,videos);
            }
            for(YouTubeFeeds.Video v:videos){YouTubeFeeds.Video target=merged.get(v.id);if(target==null){target=new YouTubeFeeds.Video(v.id,v.title,v.author,v.channel,v.published);merged.put(v.id,target);}target.clubs.add(source.club);}
        }
        out.videos.addAll(merged.values());out.videos.sort(Comparator.comparingLong((YouTubeFeeds.Video v)->v.published).reversed().thenComparing(v->v.id));if(out.videos.size()>60)out.videos.subList(60,out.videos.size()).clear();return out;
    }
}
